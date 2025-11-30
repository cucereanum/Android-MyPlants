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

    @Volatile
    private var liveMode = false
    private var liveJob: Job? = null

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
        val fe95 = UUID.fromString("0000FE95-0000-1000-8000-00805F9B34FB")
        filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(fe95)).build()

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
            trySend(ConnectionState.Connecting(address))

            val gattCallback = object : BluetoothGattCallback() {

                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    Log.w(
                        "BLE",
                        "Connection state changed: STATE=$newState status=$status device=${gatt.device.address}"
                    )

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d("BLE", "Connected to ${gatt.device.address}")
                            trySend(ConnectionState.Connected(address))
                            scope.launch {
                                delay(200)
                                gatt.requestMtu(64)
                            }
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.w("BLE", "Disconnected from ${gatt.device.address}, status=$status")
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
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE", "MTU changed successfully to $mtu")
                    } else {
                        Log.w("BLE", "MTU change failed with status $status, using default")
                    }
                    scope.launch {
                        delay(100)
                        gatt.discoverServices()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE", "Services discovered successfully")
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        scope.launch {
                            delay(300)
                            trySend(ConnectionState.ServicesDiscovered(address))
                        }
                    } else {
                        Log.e("BLE", "Service discovery failed with status $status")
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
                    notificationChannel?.trySend(bytes)
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
            val stillConnected = btManager.getConnectionState(g.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
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

        val key = GattOpKey(service, characteristic)
        val waiter = CompletableDeferred<Result<Boolean>>()
        synchronized(pendingWrites) { pendingWrites[key] = waiter }

        Log.d("BLE", "WRITE start $characteristic type=$writeType len=${value.size}")
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

            delay(500)

            try {
                Log.d("BLE", "Reading firmware/battery for initialization")
                val initData = readCharacteristic(svc, BleUuids.CHAR_VERSION_BATTERY)
                val battery = initData.firstOrNull()?.toInt()?.and(0xFF) ?: 0
                val fwVersion = if (initData.size >= 7) {
                    String(initData.copyOfRange(1, 7), Charsets.US_ASCII)
                } else {
                    "unknown"
                }
                Log.d("BLE", "Init OK - FW: $fwVersion, Batt: $battery%")
            } catch (e: Throwable) {
                Log.e("BLE", "Init read failed: ${e.message}")
                close(IllegalStateException("Device initialization failed")); return@launch
            }

            delay(300)

            try {
                val g = gatt ?: throw IllegalStateException("GATT null")
                val service = g.getService(svc) ?: throw IllegalStateException("Service not found")
                val realtimeChar = service.getCharacteristic(chrRealtime)
                    ?: throw IllegalStateException("Characteristic not found")

                val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
                val cccd = realtimeChar.getDescriptor(cccdUuid)
                    ?: throw IllegalStateException("CCCD descriptor not found")

                if (!g.setCharacteristicNotification(realtimeChar, true)) {
                    throw IllegalStateException("setCharacteristicNotification failed")
                }

                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val dKey = DescOpKey(svc, chrRealtime, cccdUuid)
                val waiter = CompletableDeferred<Result<Boolean>>()
                synchronized(pendingDescWrites) { pendingDescWrites[dKey] = waiter }

                if (!g.writeDescriptor(cccd)) {
                    synchronized(pendingDescWrites) { pendingDescWrites.remove(dKey) }
                    throw IllegalStateException("writeDescriptor failed")
                }

                waiter.await().getOrElse { throw it }
                delay(300)

                writeCharacteristic(
                    svc, chrControl,
                    byteArrayOf(0xA0.toByte(), 0x1F.toByte()),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

                val notificationJob = launch {
                    for (data in dataChannel) {
                        if (data.isNotEmpty()) {
                            parseRealtime(data)?.let {
                                Log.d(
                                    "BLE",
                                    "Sensor: temp=${it.temperatureC}°C, moist=${it.moisturePct}%, light=${it.lightLux}lx, ec=${it.conductivity}µS/cm"
                                )
                                trySend(it)
                            }
                        }
                    }
                }

                val keepAliveJob = launch {
                    delay(500)
                    val historySvc = BleUuids.SERVICE_FLOWER_CARE_HISTORY
                    val historyControl = BleUuids.CHAR_HISTORY_CONTROL
                    val keepAliveCommand = byteArrayOf(0xA0.toByte(), 0x00.toByte(), 0x00.toByte())

                    while (isActive && liveMode) {
                        try {
                            writeCharacteristic(
                                historySvc,
                                historyControl,
                                keepAliveCommand,
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            )
                            delay(250)
                        } catch (e: Exception) {
                            delay(250)
                        }
                    }
                }

                // Wait for both jobs
                try {
                    notificationJob.join()
                    keepAliveJob.join()
                } finally {
                    notificationJob.cancel()
                    keepAliveJob.cancel()
                }

            } catch (e: Exception) {
                Log.e("BLE", "Live mode error: ${e.message}")
                throw e
            }
        }

        awaitClose {
            Log.d("BLE", "Closing live mode")
            liveMode = false
            liveJob?.cancel(); liveJob = null
            notificationChannel?.close()
            notificationChannel = null
        }
    }

    override fun stopFlowerCareLive() {
        liveMode = false
        liveJob?.cancel()
        liveJob = null
        notificationChannel?.close()
        notificationChannel = null
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