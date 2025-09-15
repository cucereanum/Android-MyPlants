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
    private val notifyFlows =
        mutableMapOf<Pair<UUID, UUID>, MutableSharedFlow<ByteArray>>() // no replay

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

    private data class ReadKey(val svc: UUID, val chr: UUID)

    private val pendingReads = mutableMapOf<ReadKey, CompletableDeferred<Result<ByteArray>>>()
    private val pendingWrites =
        mutableMapOf<ReadKey, CompletableDeferred<Result<Boolean>>>()

    private data class DescKey(val svc: UUID, val chr: UUID, val desc: UUID)

    private val pendingDescWrites =
        mutableMapOf<DescKey, CompletableDeferred<Result<Boolean>>>()

    private val gattOpMutex = kotlinx.coroutines.sync.Mutex()

    private fun completePendingRead(chr: BluetoothGattCharacteristic, status: Int) {
        val key = ReadKey(chr.service.uuid, chr.uuid)
        val result = if (status == BluetoothGatt.GATT_SUCCESS) {
            Result.success(chr.value ?: ByteArray(0))
        } else {
            Result.failure(IllegalStateException("Read failed: $status"))
        }
        synchronized(pendingReads) {
            pendingReads.remove(key)?.complete(result)
        }
    }

    // ---------- SCAN (filters for Xiaomi FE95 + "Flower care" name) ----------
    @SuppressLint("MissingPermission")
    override fun scanDevices(filterServiceUuid: UUID?): Flow<List<BleDevice>> = callbackFlow {
        discovered.clear()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val uuids = result.scanRecord?.serviceUuids?.mapNotNull { it.uuid } ?: emptyList()
                val name = result.scanRecord?.deviceName ?: device.name

                // Prefer matching actual device name
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

        // Strong filter for Xiaomi FE95 (adds performance/accuracy)
        val fe95 = UUID.fromString("0000FE95-0000-1000-8000-00805F9B34FB")
        filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(fe95)).build()

        // Optional extra service filter from caller
        if (filterServiceUuid != null) {
            filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(filterServiceUuid)).build()
        }

        scanner?.startScan(filters, settings, cb)
        trySend(discovered.values.toList())

        awaitClose { scanner?.stopScan(cb) }
    }.onStart { emit(emptyList()) }

    // ---------- CONNECT (short-session friendly) ----------
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
                        "BLE!!!!----!!!!",
                        "STATE=$newState status=$status device=${gatt.device.address}"
                    )

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            trySend(ConnectionState.Connected(address))
                            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                            gatt.requestMtu(128)
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            stopFlowerCareLive() // <- make sure the poller stops
                            val cause = if (status == 19) null else "status=$status"
                            trySend(ConnectionState.Disconnected(address, cause))
                            close()
                        }
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    // Continue with service discovery regardless of MTU result
                    gatt.discoverServices()
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        trySend(ConnectionState.ServicesDiscovered(address))
                    } else {
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
                    notifyFlows[characteristic.service.uuid to characteristic.uuid]
                        ?.tryEmit(characteristic.value ?: ByteArray(0))
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
                    val key = ReadKey(characteristic.service.uuid, characteristic.uuid)
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
                    val key = DescKey(parentChar.service.uuid, parentChar.uuid, descriptor.uuid)
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

    // ---------- GENERIC READ / NOTIFY ----------
    @SuppressLint("MissingPermission")
    override suspend fun readCharacteristic(service: UUID, characteristic: UUID): ByteArray =
        gattOpMutex.withLock {
            val g = gatt ?: throw IllegalStateException("Not connected")

            // sanity: still connected?
            val stillConnected =
                btManager.getConnectionState(
                    g.device,
                    BluetoothProfile.GATT
                ) == BluetoothProfile.STATE_CONNECTED
            if (!stillConnected) throw IllegalStateException("Peripheral not connected")

            val svc = g.getService(service)
                ?: throw IllegalArgumentException("Service $service not found")
            val chr = svc.getCharacteristic(characteristic)
                ?: throw IllegalArgumentException("Char $characteristic not found")

            val key = ReadKey(service, characteristic)
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

        val key = ReadKey(service, characteristic)
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
    override fun observeCharacteristic(
        service: UUID,
        characteristic: UUID,
        enable: Boolean
    ): Flow<ByteArray> = channelFlow {
        val g = gatt ?: run {
            close(IllegalStateException("Not connected")); return@channelFlow
        }
        val svc = g.getService(service) ?: run {
            close(IllegalArgumentException("Missing service $service")); return@channelFlow
        }
        val chr = svc.getCharacteristic(characteristic) ?: run {
            close(IllegalArgumentException("Missing characteristic $characteristic")); return@channelFlow
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        val cccd = chr.getDescriptor(cccdUuid)
        val flowKey = service to characteristic
        val shared = notifyFlows.getOrPut(flowKey) { MutableSharedFlow(extraBufferCapacity = 16) }

// Route notifications locally (this is local-only; fine outside the lock)
        val okNotif = g.setCharacteristicNotification(chr, enable)
        if (!okNotif) {
            close(IllegalStateException("setCharacteristicNotification failed"))
            return@channelFlow
        }

// If CCCD exists, write it and AWAIT the callback (serialize via mutex)
        if (cccd != null) {
            cccd.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            val dKey = DescKey(service, characteristic, cccdUuid)
            val waiter = CompletableDeferred<Result<Boolean>>()
            synchronized(pendingDescWrites) { pendingDescWrites[dKey] = waiter }

            val okStart = gattOpMutex.withLock {
                // sanity: still connected?
                val stillConnected =
                    btManager.getConnectionState(
                        g.device,
                        BluetoothProfile.GATT
                    ) == BluetoothProfile.STATE_CONNECTED
                if (!stillConnected) false else g.writeDescriptor(cccd)
            }
            if (!okStart) {
                synchronized(pendingDescWrites) { pendingDescWrites.remove(dKey) }
                close(IllegalStateException("writeDescriptor(CCCD) returned false"))
                return@channelFlow
            }

            // await onDescriptorWrite before proceeding
            waiter.await().getOrElse { err ->
                close(err); return@channelFlow
            }
        }

// Now notifications are actually enabled; start relaying
        val job = launch { shared.collect { send(it) } }
        awaitClose {
            job.cancel()
            try {
                g.setCharacteristicNotification(chr, false)
                if (cccd != null) {
                    cccd.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    // best-effort write, no suspending here
                    if (gattOpMutex.tryLock()) {
                        try {
                            g.writeDescriptor(cccd)
                        } finally {
                            gattOpMutex.unlock()
                        }
                    } else {
                        // If busy, just skip; it's teardown anyway
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() { /* scan stopped in awaitClose() */
    }


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
        liveJob = launch {
            // Keep tight connection parameters
            gattOpMutex.withLock { gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }

            if (!isConnected()) {
                close(); return@launch
            }

            // 1) Enable CCCD on 1A01 and AWAIT onDescriptorWrite (observeCharacteristic does that)
            val notifyFlow = try {
                observeCharacteristic(svc, chrRealtime, enable = true)
            } catch (t: Throwable) {
                close(t); return@launch
            }

            // 2) Arm real-time mode ONCE (prefer with response)
            val armed = try {
                writeCharacteristic(
                    svc, chrControl,
                    byteArrayOf(0xA0.toByte(), 0x1F.toByte()),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } catch (_: Throwable) {
                writeCharacteristic(
                    svc, chrControl,
                    byteArrayOf(0xA0.toByte(), 0x1F.toByte()),
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            }
            if (!armed) {
                close(IllegalStateException("Failed to arm live mode")); return@launch
            }

            // 3) Small settle
            delay(400)

            // 4) Collect ONLY notifications (no polling)
            notifyFlow.collect { bytes ->
                parseRealtime(bytes)?.let { trySend(it) }
            }
        }

        awaitClose {
            liveMode = false
            liveJob?.cancel(); liveJob = null
            // observeCharacteristic's awaitClose already disables CCCD best-effort
        }
    }

    /** Stop only the live poller (call disconnect() separately if you want to fully drop the link). */
    override fun stopFlowerCareLive() {
        liveMode = false
        liveJob?.cancel()
        liveJob = null
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun readFlowerCareOnce(timeoutMs: Long): Pair<ByteArray, ByteArray> {
        val svc = BleUuids.SERVICE_FLOWER_CARE
        val chrNotify = BleUuids.CHAR_REALTIME_DATA
        val chrControl = BleUuids.CHAR_CONTROL
        val chrBattVer = BleUuids.CHAR_VERSION_BATTERY

        // 1) subscribe
        val notify = observeCharacteristic(svc, chrNotify, enable = true)

        // 2) write trigger (0xA0 0x1F)
        val g = gatt ?: throw IllegalStateException("Not connected")
        val s = g.getService(svc) ?: error("Service $svc not found")
        val control = s.getCharacteristic(chrControl) ?: error("Char $chrControl not found")
        control.value = byteArrayOf(0xA0.toByte(), 0x1F.toByte())
        if (!g.writeCharacteristic(control)) error("Failed to write trigger")

        // 3) wait for realtime sample
        val sample = withTimeout(timeoutMs) { notify.first() }

        // 4) battery/version
        val batt = readCharacteristic(svc, chrBattVer)

        // 5) done (caller decides when to disconnect)
        return sample to batt
    }


    /** Flora live frame (16B on your unit):
     * [0..1]=temp*10 LE (signed)
     * [2]=unused/type
     * [3..6]=light u32 LE
     * [7]=moisture %
     * [8..9]=conductivity u16 LE
     * [10..15]=suffix/unused (can be ignored)
     */
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