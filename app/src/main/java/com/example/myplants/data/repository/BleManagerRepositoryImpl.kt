package com.example.myplants.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.BleUuids
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class RealtimeParsed(
    val temperatureC: Double?,
    val moisturePct: Int?,
    val lightLux: Long?,
    val conductivity: Int?
)


@Singleton
class BleManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : BleManagerRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val btManager by lazy { appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val btAdapter: BluetoothAdapter? get() = btManager.adapter
    private val scanner: BluetoothLeScanner? get() = btAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private val discovered = ConcurrentHashMap<String, BleDevice>()
    private var notificationChannel: kotlinx.coroutines.channels.Channel<ByteArray>? = null
    private var fe95HandshakeNotificationChannel: kotlinx.coroutines.channels.Channel<ByteArray>? =
        null

    @Volatile
    private var liveMode = false
    private var liveJob: Job? = null

    private companion object {
        private const val TAG = "BLE"

        // Keep logcat quiet by default. Flip to `true` temporarily while debugging.
        private const val ENABLE_VERBOSE_LOGS: Boolean = false

        // The GATT table dump is extremely noisy; keep it opt-in.
        private const val ENABLE_GATT_TABLE_DUMP_LOGS: Boolean = false

        private inline fun logVerbose(message: () -> String) {
            if (ENABLE_VERBOSE_LOGS) {
                Log.d(TAG, message())
            }
        }
    }

    override val isBluetoothOn: Flow<Boolean> =
        callbackFlow {
            trySend(btAdapter?.isEnabled == true)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    if (BluetoothAdapter.ACTION_STATE_CHANGED == i?.action) {
                        val state =
                            i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        trySend(state == BluetoothAdapter.STATE_ON)
                    }
                }
            }
            val flt = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            appContext.registerReceiver(receiver, flt)
            awaitClose { appContext.unregisterReceiver(receiver) }
        }.distinctUntilChanged()

    private data class GattOpKey(val svc: UUID, val chr: UUID)
    private data class DescOpKey(val svc: UUID, val chr: UUID, val desc: UUID)

    private val pendingReads = mutableMapOf<GattOpKey, CompletableDeferred<Result<ByteArray>>>()
    private val pendingWrites = mutableMapOf<GattOpKey, CompletableDeferred<Result<Boolean>>>()
    private val pendingDescWrites = mutableMapOf<DescOpKey, CompletableDeferred<Result<Boolean>>>()
    private val gattOpMutex = kotlinx.coroutines.sync.Mutex()

    private fun completePendingRead(chr: BluetoothGattCharacteristic, status: Int) {
        val key = GattOpKey(chr.service.uuid, chr.uuid)
        val result = if (status == BluetoothGatt.GATT_SUCCESS) {
            Result.success(chr.value ?: ByteArray(0))
        } else {
            Result.failure(IllegalStateException("Read failed: $status"))
        }
        synchronized(pendingReads) {
            pendingReads.remove(key)?.complete(result)
        }
    }

    private fun logDiscoveredGattTable(gatt: BluetoothGatt) {
        if (!ENABLE_GATT_TABLE_DUMP_LOGS) return
        try {
            val services = gatt.services.orEmpty()
            Log.d(TAG, "Discovered ${services.size} GATT service(s)")

            for (service in services) {
                Log.d(TAG, "GATT service uuid=${service.uuid} type=${service.type}")

                for (characteristic in service.characteristics.orEmpty()) {
                    val properties = characteristic.properties
                    val propertyLabels = buildList {
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) add("READ")
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) add("WRITE")
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) add(
                            "WRITE_NR"
                        )
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) add("NOTIFY")
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) add(
                            "INDICATE"
                        )
                    }.joinToString(separator = "|")

                    val descriptorUuids = characteristic.descriptors.orEmpty().joinToString(
                        separator = ",",
                        prefix = "[",
                        postfix = "]",
                    ) { descriptor ->
                        descriptor.uuid.toString()
                    }

                    Log.d(
                        TAG,
                        "GATT char uuid=${characteristic.uuid} props=$propertyLabels descriptors=$descriptorUuids"
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to log GATT table: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun scanDevices(filterServiceUuid: UUID?): Flow<List<BleDevice>> = callbackFlow {
        discovered.clear()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val uuids = result.scanRecord?.serviceUuids?.mapNotNull { it.uuid } ?: emptyList()
                val name = result.scanRecord?.deviceName ?: device.name
                if (name?.contains("Flower care", ignoreCase = true) != true) return

                val entry = BleDevice(
                    address = device.address,
                    name = name,
                    rssi = result.rssi,
                    serviceUuids = uuids
                )
                discovered[device.address] = entry
                trySend(discovered.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE })
            }

            override fun onScanFailed(errorCode: Int) {
                trySend(emptyList())
                close(IllegalStateException("BLE scan failed with code $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = mutableListOf<ScanFilter>()
        filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(BleUuids.SERVICE_XIAOMI_FE95))
            .build()

        if (filterServiceUuid != null) {
            filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(filterServiceUuid)).build()
        }

        scanner?.startScan(filters, settings, cb)
        trySend(discovered.values.toList())

        awaitClose { scanner?.stopScan(cb) }
    }.onStart { emit(emptyList()) }

    @SuppressLint("MissingPermission")
    override fun connect(address: String, autoConnect: Boolean): Flow<ConnectionState> =
        callbackFlow {
            val device = btAdapter?.getRemoteDevice(address) ?: run {
                close(IllegalArgumentException("Unknown device $address"))
                return@callbackFlow
            }
            Log.d(
                TAG,
                "Connecting to plant sensor: address=$address name=${device.name ?: "unknown"} autoConnect=$autoConnect"
            )
            trySend(ConnectionState.Connecting(address))

            val gattCallback = object : BluetoothGattCallback() {

                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    Log.w(
                        TAG,
                        "Connection state changed: STATE=$newState status=$status device=${gatt.device.address}"
                    )

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            logVerbose {
                                "Connected to plant sensor: address=${gatt.device.address} name=${gatt.device.name ?: "unknown"}"
                            }
                            trySend(ConnectionState.Connected(address))
                            scope.launch {
                                // Some Flower Care sensors have a short window before they drop the link.
                                // Start service discovery immediately; MTU negotiation can be slow/ignored.
                                delay(100)
                                gatt.discoverServices()
                            }
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.w(TAG, "Disconnected from ${gatt.device.address}, status=$status")
                            stopFlowerCareLive()
                            val cause = when (status) {
                                0 -> null
                                8 -> "Connection timeout"
                                19 -> "Connection terminated by peer"
                                22 -> "Connection LMP timeout"
                                133 -> "GATT error (device unreachable)"
                                else -> "Connection error (status=$status)"
                            }
                            trySend(ConnectionState.Disconnected(address, cause))
                            close()
                        }
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    logVerbose {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            "MTU changed successfully to $mtu"
                        } else {
                            "MTU change failed with status $status, using default"
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        logVerbose { "Services discovered successfully" }
                        logDiscoveredGattTable(gatt)
                        // Avoid forcing connection parameters; some Flower Care sensors are sensitive
                        // and may terminate the link when the central requests changes.
                        scope.launch {
                            delay(100)
                            trySend(ConnectionState.ServicesDiscovered(address))
                        }
                    } else {
                        Log.e(TAG, "Service discovery failed with status $status")
                        trySend(
                            ConnectionState.Disconnected(
                                address,
                                cause = "Service discovery failed ($status)"
                            )
                        )
                        close()
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    val bytes = characteristic.value ?: ByteArray(0)
                    // Route FE95 handshake notifications to a dedicated channel.
                    if (
                        characteristic.service.uuid == BleUuids.SERVICE_XIAOMI_FE95 &&
                        characteristic.uuid == BleUuids.CHAR_XIAOMI_FE95_0001
                    ) {
                        fe95HandshakeNotificationChannel?.trySend(bytes)
                    }

                    // Only forward realtime sensor notifications to the live parser.
                    if (characteristic.uuid == BleUuids.CHAR_REALTIME_DATA) {
                        notificationChannel?.trySend(bytes)
                    }
                }

                @Suppress("DEPRECATION")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    completePendingRead(characteristic, status)
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    val key = GattOpKey(characteristic.service.uuid, characteristic.uuid)
                    val result = if (status == BluetoothGatt.GATT_SUCCESS) {
                        Result.success(true)
                    } else {
                        Result.failure(IllegalStateException("Write failed: $status"))
                    }
                    synchronized(pendingWrites) {
                        pendingWrites.remove(key)?.complete(result)
                    }
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int
                ) {
                    val parentChar = descriptor.characteristic
                    val key = DescOpKey(parentChar.service.uuid, parentChar.uuid, descriptor.uuid)
                    val result = if (status == BluetoothGatt.GATT_SUCCESS) {
                        Result.success(true)
                    } else {
                        Result.failure(IllegalStateException("Descriptor write failed: $status"))
                    }
                    synchronized(pendingDescWrites) {
                        pendingDescWrites.remove(key)?.complete(result)
                    }
                }
            }

            gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(
                    appContext,
                    autoConnect,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                device.connectGatt(appContext, autoConnect, gattCallback)
            }

            awaitClose {
                try {
                    gatt?.disconnect()
                    gatt?.close()
                } catch (_: Throwable) {
                }
                gatt = null
            }
        }

    @SuppressLint("MissingPermission")
    override suspend fun readCharacteristic(service: UUID, characteristic: UUID): ByteArray =
        gattOpMutex.withLock {
            val g = gatt ?: throw IllegalStateException("Not connected")
            val stillConnected = btManager.getConnectionState(
                g.device,
                BluetoothProfile.GATT
            ) == BluetoothProfile.STATE_CONNECTED
            if (!stillConnected) throw IllegalStateException("Peripheral not connected")

            val svc = g.getService(service)
                ?: throw IllegalArgumentException("Service $service not found")
            val chr = svc.getCharacteristic(characteristic)
                ?: throw IllegalArgumentException("Char $characteristic not found")

            val key = GattOpKey(service, characteristic)
            val waiter = CompletableDeferred<Result<ByteArray>>()
            synchronized(pendingReads) { pendingReads[key] = waiter }

            if (!g.readCharacteristic(chr)) {
                synchronized(pendingReads) { pendingReads.remove(key) }
                throw IllegalStateException("readCharacteristic() returned false (busy/disconnected)")
            }

            waiter.await().getOrElse { throw it }
        }

    @SuppressLint("MissingPermission")
    override suspend fun writeCharacteristic(
        service: UUID,
        characteristic: UUID,
        value: ByteArray,
        writeType: Int
    ): Boolean = gattOpMutex.withLock {
        val g = gatt ?: throw IllegalStateException("Not connected")
        val stillConnected = btManager.getConnectionState(
            g.device,
            BluetoothProfile.GATT
        ) == BluetoothProfile.STATE_CONNECTED
        if (!stillConnected) throw IllegalStateException("Peripheral not connected")

        val svc =
            g.getService(service) ?: throw IllegalArgumentException("Service $service not found")
        val chr = svc.getCharacteristic(characteristic)
            ?: throw IllegalArgumentException("Char $characteristic not found")

        chr.writeType = writeType
        chr.value = value

        // For NO_RESPONSE writes, Android may not invoke onCharacteristicWrite().
        // Waiting for a callback here can stall the GATT operation queue.
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            logVerbose {
                "WRITE(no_response) start $characteristic len=${value.size}"
            }
            return@withLock g.writeCharacteristic(chr)
        }
        val key = GattOpKey(service, characteristic)
        val waiter = CompletableDeferred<Result<Boolean>>()
        synchronized(pendingWrites) { pendingWrites[key] = waiter }

        logVerbose {
            "WRITE start $characteristic type=$writeType len=${value.size}"
        }
        if (!g.writeCharacteristic(chr)) {
            synchronized(pendingWrites) { pendingWrites.remove(key) }
            throw IllegalStateException("writeCharacteristic() returned false (busy/disconnected)")
        }
        waiter.await().getOrElse { throw it }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
    }

    /**
     * Start MI Flora sensor monitoring.
     * Protocol: ARM sensor (0xA01F), enable notifications, receive sensor data, send keep-alive writes (0xA00000) every 250ms.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun startFlowerCareLive(): Flow<RealtimeParsed> = channelFlow {
        val svc = BleUuids.SERVICE_FLOWER_CARE
        val chrRealtime = BleUuids.CHAR_REALTIME_DATA
        val chrControl = BleUuids.CHAR_CONTROL
        val historySvc = BleUuids.SERVICE_FLOWER_CARE_HISTORY
        val chrHistoryControl = BleUuids.CHAR_HISTORY_CONTROL
        val xiaomiServiceFe95Uuid = BleUuids.SERVICE_XIAOMI_FE95
        val xiaomiHandshakeCharacteristicUuid0001 = BleUuids.CHAR_XIAOMI_FE95_0001
        val xiaomiWriteRequestHandle001bLikelyCharacteristicUuids = listOf(
            BleUuids.CHAR_XIAOMI_FE95_0010,
            BleUuids.CHAR_XIAOMI_FE95_0007,
            BleUuids.CHAR_XIAOMI_FE95_0013,
        )
        val xiaomiHandshakeValueHandle0012Payload = byteArrayOf(
            0xDD.toByte(),
            0xFE.toByte(),
            0x93.toByte(),
            0x07.toByte(),
            0xF7.toByte(),
            0xB1.toByte(),
            0xFE.toByte(),
            0x20.toByte(),
            0xB7.toByte(),
            0x61.toByte(),
            0xCE.toByte(),
            0x02.toByte(),
        )
        val xiaomiSecondWriteHandle0012Payload = byteArrayOf(
            0x65.toByte(),
            0x92.toByte(),
            0x3A.toByte(),
        )
        val xiaomiWriteRequestHandle001bPayload = byteArrayOf(
            0x90.toByte(),
            0xCA.toByte(),
            0x85.toByte(),
            0xDE.toByte(),
        )

        // Enable the full Xiaomi FE95 handshake.
        val enableSecondFe95PayloadWrite = true
        // Xiaomi writes CCCD (0x0013) value 0000 after the second 0x0012 write.
        val disableFe95NotificationsAfterSecondWrite = true

        // Keep-alive fallback. Leave off while testing the full Xiaomi handshake.
        val enableFe95MaintenanceWrites = false
        // The device seems to terminate the connection ~3-4s after the last FE95 activity.
        // Keep this interval below that window, but not so aggressive that it triggers 133.
        val fe95MaintenanceIntervalMs = 2_000L
        val fe95MaintenanceCharacteristicUuid = BleUuids.CHAR_XIAOMI_FE95_0010

        // Xiaomi does write to 0x1A10 (A0 00 00), but our device appears very sensitive to write volume.
        // Disable this while we tune the FE95 session keep-alive.
        val enableHistoryKeepAliveWrites = false

        fun isConnected(): Boolean {
            val g = gatt ?: return false
            return btManager.getConnectionState(
                g.device,
                BluetoothProfile.GATT
            ) == BluetoothProfile.STATE_CONNECTED
        }

        liveMode = true
        val dataChannel = kotlinx.coroutines.channels.Channel<ByteArray>(capacity = 16)
        notificationChannel = dataChannel

        liveJob = launch {
            if (!isConnected()) {
                close(); return@launch
            }

            delay(100)

            suspend fun enableNotificationsIfSupported(
                serviceUuid: UUID,
                characteristicUuid: UUID
            ) {
                val g = gatt ?: throw IllegalStateException("GATT null")
                val service = g.getService(serviceUuid)
                    ?: throw IllegalStateException("Service $serviceUuid not found")
                val characteristic = service.getCharacteristic(characteristicUuid)
                    ?: throw IllegalStateException("Characteristic $characteristicUuid not found")

                val supportsNotify =
                    (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val supportsIndicate =
                    (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (!supportsNotify && !supportsIndicate) {
                    logVerbose {
                        "Skipping notifications for $characteristicUuid (no NOTIFY/INDICATE property)"
                    }
                    return
                }

                val cccd = characteristic.getDescriptor(BleUuids.DESC_CCCD)
                    ?: throw IllegalStateException("CCCD descriptor not found for $characteristicUuid")

                if (!g.setCharacteristicNotification(characteristic, true)) {
                    throw IllegalStateException("setCharacteristicNotification failed for $characteristicUuid")
                }

                val enableValue = if (supportsNotify) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }

                cccd.value = enableValue
                val descriptorKey = DescOpKey(serviceUuid, characteristicUuid, BleUuids.DESC_CCCD)
                val waiter = CompletableDeferred<Result<Boolean>>()
                synchronized(pendingDescWrites) { pendingDescWrites[descriptorKey] = waiter }

                val writeAccepted: Boolean =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(cccd, enableValue) == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(cccd)
                    }

                if (!writeAccepted) {
                    synchronized(pendingDescWrites) { pendingDescWrites.remove(descriptorKey) }
                    throw IllegalStateException("writeDescriptor failed for $characteristicUuid")
                }

                waiter.await().getOrElse { throw it }

                val notificationModeLabel = if (supportsNotify) {
                    "notify"
                } else {
                    "indicate"
                }
                logVerbose {
                    "Notifications enabled for $characteristicUuid (service=$serviceUuid mode=$notificationModeLabel)"
                }
            }

            suspend fun writeCccdValue(
                serviceUuid: UUID,
                characteristicUuid: UUID,
                cccdValue: ByteArray,
            ) {
                val g = gatt ?: throw IllegalStateException("GATT null")
                val service = g.getService(serviceUuid)
                    ?: throw IllegalStateException("Service $serviceUuid not found")
                val characteristic = service.getCharacteristic(characteristicUuid)
                    ?: throw IllegalStateException("Characteristic $characteristicUuid not found")

                val cccd = characteristic.getDescriptor(BleUuids.DESC_CCCD)
                    ?: throw IllegalStateException("CCCD descriptor not found for $characteristicUuid")

                cccd.value = cccdValue
                val descriptorKey = DescOpKey(serviceUuid, characteristicUuid, BleUuids.DESC_CCCD)
                val waiter = CompletableDeferred<Result<Boolean>>()
                synchronized(pendingDescWrites) { pendingDescWrites[descriptorKey] = waiter }

                val writeAccepted: Boolean =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(cccd, cccdValue) == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(cccd)
                    }

                if (!writeAccepted) {
                    synchronized(pendingDescWrites) { pendingDescWrites.remove(descriptorKey) }
                    throw IllegalStateException("writeDescriptor failed for $characteristicUuid")
                }

                waiter.await().getOrElse { throw it }
                logVerbose {
                    "CCCD written for $characteristicUuid (service=$serviceUuid value=${
                        cccdValue.joinToString("") { b -> String.format("%02x", b) }
                    })"
                }
            }

            suspend fun writeCccdValueWithRetries(
                serviceUuid: UUID,
                characteristicUuid: UUID,
                cccdValue: ByteArray,
                maxAttempts: Int,
                attemptDelayMs: Long,
            ) {
                var attemptIndex = 1
                var lastError: Throwable? = null

                while (attemptIndex <= maxAttempts && isConnected()) {
                    try {
                        if (attemptIndex > 1) {
                            Log.w(
                                TAG,
                                "CCCD write retry $attemptIndex/$maxAttempts for $characteristicUuid"
                            )
                        }
                        writeCccdValue(serviceUuid, characteristicUuid, cccdValue)
                        return
                    } catch (e: Throwable) {
                        lastError = e
                        delay(attemptDelayMs)
                        attemptIndex++
                    }
                }

                throw IllegalStateException(
                    "CCCD write failed for $characteristicUuid after $maxAttempts attempts",
                    lastError
                )
            }

            try {
                val handshakeChannel = kotlinx.coroutines.channels.Channel<ByteArray>(capacity = 1)
                fe95HandshakeNotificationChannel = handshakeChannel

                // Start realtime streaming early so the UI gets data even if the device is picky about the FE95 handshake.
                enableNotificationsIfSupported(svc, chrRealtime)
                writeCharacteristic(
                    svc,
                    chrControl,
                    byteArrayOf(0xA0.toByte(), 0x1F.toByte()),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

                suspend fun performXiaomiFe95HandshakeIfPresent() {
                    val g = gatt ?: throw IllegalStateException("GATT null")
                    val fe95Service = g.getService(xiaomiServiceFe95Uuid)
                    if (fe95Service == null) {
                        Log.d(
                            TAG,
                            "Xiaomi FE95 handshake: FE95 service not present"
                        )
                        return
                    }

                    val handshakeCharacteristic =
                        fe95Service.getCharacteristic(xiaomiHandshakeCharacteristicUuid0001)
                    if (handshakeCharacteristic == null) {
                        Log.w(
                            TAG,
                            "Xiaomi FE95 handshake: characteristic 0x0001 not present in FE95 service"
                        )
                        return
                    }

                    Log.d(
                        TAG,
                        "Xiaomi FE95 handshake: using service=$xiaomiServiceFe95Uuid characteristic=$xiaomiHandshakeCharacteristicUuid0001"
                    )

                    try {
                        for (candidateUuid in xiaomiWriteRequestHandle001bLikelyCharacteristicUuids) {
                            try {
                                Log.d(
                                    TAG,
                                    "Xiaomi FE95 handshake: attempting 0x001b payload write to characteristic=$candidateUuid"
                                )
                                writeCharacteristic(
                                    xiaomiServiceFe95Uuid,
                                    candidateUuid,
                                    xiaomiWriteRequestHandle001bPayload,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                )
                                Log.d(
                                    TAG,
                                    "Xiaomi FE95 handshake: 0x001b payload write accepted by characteristic=$candidateUuid"
                                )
                                break
                            } catch (e: Throwable) {
                                Log.w(
                                    TAG,
                                    "Xiaomi FE95 handshake: 0x001b payload write rejected for characteristic=$candidateUuid (${e.message})"
                                )
                            }
                        }

                        enableNotificationsIfSupported(
                            xiaomiServiceFe95Uuid,
                            xiaomiHandshakeCharacteristicUuid0001
                        )

                        writeCharacteristic(
                            xiaomiServiceFe95Uuid,
                            xiaomiHandshakeCharacteristicUuid0001,
                            xiaomiHandshakeValueHandle0012Payload,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )

                        val handshakeNotificationBytes = withTimeout(1_000) {
                            fe95HandshakeNotificationChannel?.receive()
                        }

                        if (handshakeNotificationBytes == null) {
                            Log.w(
                                TAG,
                                "Xiaomi FE95 handshake: no notification received after first payload write"
                            )
                        } else {
                            val notificationHex =
                                handshakeNotificationBytes.joinToString(separator = "") { byte ->
                                    String.format("%02x", byte)
                                }
                            Log.d(
                                TAG,
                                "Xiaomi FE95 handshake: received notification len=${handshakeNotificationBytes.size} value=$notificationHex"
                            )
                        }

                        if (enableSecondFe95PayloadWrite) {
                            // The official app's 2nd write to handle 0x0012 is a very short payload.
                            // Some firmwares appear to reject it with a response (status=141). Try a
                            // write-without-response first to better match a "write command".
                            delay(5)
                            val wroteSecondPayloadNoResponse = try {
                                writeCharacteristic(
                                    xiaomiServiceFe95Uuid,
                                    xiaomiHandshakeCharacteristicUuid0001,
                                    xiaomiSecondWriteHandle0012Payload,
                                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                                )
                            } catch (e: Throwable) {
                                Log.w(
                                    TAG,
                                    "Xiaomi FE95 handshake: 2nd payload WRITE_NR threw (${e.message})"
                                )
                                false
                            }

                            if (!wroteSecondPayloadNoResponse) {
                                Log.w(
                                    TAG,
                                    "Xiaomi FE95 handshake: 2nd payload WRITE_NR returned false; retrying with WRITE_TYPE_DEFAULT"
                                )
                                writeCharacteristic(
                                    xiaomiServiceFe95Uuid,
                                    xiaomiHandshakeCharacteristicUuid0001,
                                    xiaomiSecondWriteHandle0012Payload,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                )
                            }
                            if (disableFe95NotificationsAfterSecondWrite) {
                                try {
                                    delay(120)
                                    writeCccdValueWithRetries(
                                        serviceUuid = xiaomiServiceFe95Uuid,
                                        characteristicUuid = xiaomiHandshakeCharacteristicUuid0001,
                                        cccdValue = byteArrayOf(0x00.toByte(), 0x00.toByte()),
                                        maxAttempts = 5,
                                        attemptDelayMs = 120,
                                    )

                                    // Best-effort: mirror the CCCD state locally too.
                                    val gattNow = gatt
                                    val fe95ServiceNow = gattNow?.getService(xiaomiServiceFe95Uuid)
                                    val handshakeCharacteristicNow =
                                        fe95ServiceNow?.getCharacteristic(
                                            xiaomiHandshakeCharacteristicUuid0001
                                        )
                                    if (gattNow != null && handshakeCharacteristicNow != null) {
                                        gattNow.setCharacteristicNotification(
                                            handshakeCharacteristicNow,
                                            false
                                        )
                                    }
                                } catch (e: Throwable) {
                                    Log.w(
                                        TAG,
                                        "Xiaomi FE95 handshake: CCCD disable failed (${e.message})"
                                    )
                                }
                            } else {
                                Log.d(
                                    TAG,
                                    "Xiaomi FE95 handshake: skipping 2nd payload write"
                                )
                            }
                        } else {
                            Log.d(
                                TAG,
                                "Xiaomi FE95 handshake: skipping 2nd payload write"
                            )
                        }

                        Log.d(TAG, "Xiaomi FE95 handshake: completed")
                    } catch (e: Throwable) {
                        Log.w(
                            TAG,
                            "Xiaomi FE95 handshake: failed (${e.message})"
                        )
                    }
                }

                performXiaomiFe95HandshakeIfPresent()

                fe95HandshakeNotificationChannel = null
                handshakeChannel.close()

                enableNotificationsIfSupported(historySvc, chrHistoryControl)
            } catch (e: Throwable) {
                Log.e(TAG, "Init read failed: ${e.message}")
                close(IllegalStateException("Device initialization failed")); return@launch
            }

            val notificationJob = launch {
                for (data in dataChannel) {
                    if (data.isNotEmpty()) {
                        parseRealtime(data)?.let {
                            logVerbose {
                                "Sensor: temp=${it.temperatureC}°C, moist=${it.moisturePct}%, light=${it.lightLux}lx, ec=${it.conductivity}µS/cm"
                            }
                            trySend(it)
                        }
                    }
                }
            }

            val keepAliveJob = launch {
                if (!enableHistoryKeepAliveWrites) return@launch
                // Captured in Wireshark: Write Request to UUID 0x1A10 with value A0 00 00.
                // Use a write-with-response; some firmwares seem to require an ACKed write.
                val keepAliveCommand = byteArrayOf(0xA0.toByte(), 0x00.toByte(), 0x00.toByte())

                // Delay periodic history keep-alives until the session is stable.
                delay(8_000)

                while (isActive && liveMode) {
                    try {
                        writeCharacteristic(
                            historySvc,
                            chrHistoryControl,
                            keepAliveCommand,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Keep-alive write failed: ${e.message}")
                    }

                    delay(8_000)
                }
            }

            val fe95MaintenanceJob = launch {
                if (!enableFe95MaintenanceWrites) return@launch

                // Start maintenance after initial setup.
                delay(1_000)

                val g = gatt ?: return@launch
                val fe95Service = g.getService(xiaomiServiceFe95Uuid) ?: return@launch
                val maintenanceCharacteristic =
                    fe95Service.getCharacteristic(fe95MaintenanceCharacteristicUuid)
                        ?: return@launch

                while (isActive && liveMode) {
                    try {
                        writeCharacteristic(
                            xiaomiServiceFe95Uuid,
                            maintenanceCharacteristic.uuid,
                            xiaomiWriteRequestHandle001bPayload,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )
                        Log.d(
                            TAG,
                            "FE95 maintenance write sent to ${maintenanceCharacteristic.uuid}"
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "FE95 maintenance write failed: ${e.message}")
                    }

                    delay(fe95MaintenanceIntervalMs)
                }
            }

            try {
                notificationJob.join()
            } finally {
                notificationJob.cancel()
                keepAliveJob.cancel()
                fe95MaintenanceJob.cancel()
            }
        }

        awaitClose {
            logVerbose { "Closing live mode" }
            liveMode = false
            liveJob?.cancel(); liveJob = null
            notificationChannel?.close()
            notificationChannel = null
            fe95HandshakeNotificationChannel?.close()
            fe95HandshakeNotificationChannel = null
        }
    }

    override fun stopFlowerCareLive() {
        liveMode = false
        liveJob?.cancel()
        liveJob = null
        notificationChannel?.close()
        notificationChannel = null
        fe95HandshakeNotificationChannel?.close()
        fe95HandshakeNotificationChannel = null
    }

    private fun parseRealtime(bytes: ByteArray): RealtimeParsed? {
        if (bytes.size < 10) return null
        val tRaw = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[0].toInt() and 0xFF)
        val tSigned = if ((tRaw and 0x8000) != 0) tRaw or -0x10000 else tRaw
        val tempC = tSigned / 10.0

        val light = ((bytes[6].toLong() and 0xFF) shl 24) or
                ((bytes[5].toLong() and 0xFF) shl 16) or
                ((bytes[4].toLong() and 0xFF) shl 8) or
                (bytes[3].toLong() and 0xFF)

        val moisture = bytes[7].toInt() and 0xFF
        val ec = ((bytes[9].toInt() and 0xFF) shl 8) or (bytes[8].toInt() and 0xFF)

        // sanity bounds; if out, skip (don’t re-arm)
        if (tempC !in -20.0..60.0) return null
        if (moisture !in 0..100) return null

        return RealtimeParsed(tempC, moisture, light, ec and 0xFFFF)
    }
}