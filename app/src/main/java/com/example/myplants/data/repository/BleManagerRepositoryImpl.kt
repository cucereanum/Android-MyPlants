package com.example.myplants.data.repository


import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

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
        mutableMapOf<Pair<UUID, UUID>, MutableSharedFlow<ByteArray>>(/* no replay */)

    override val isBluetoothOn: Flow<Boolean> =
        callbackFlow {
            trySend(btAdapter?.isEnabled == true)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, i: android.content.Intent?) {
                    if (BluetoothAdapter.ACTION_STATE_CHANGED == i?.action) {
                        val state =
                            i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        trySend(state == BluetoothAdapter.STATE_ON)
                    }
                }
            }
            val flt = android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            appContext.registerReceiver(receiver, flt)
            awaitClose { appContext.unregisterReceiver(receiver) }
        }.distinctUntilChanged()


    private data class ReadKey(val svc: UUID, val chr: UUID)

    private val pendingReads = mutableMapOf<ReadKey, CompletableDeferred<Result<ByteArray>>>()


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

    @SuppressLint("MissingPermission")
    override fun scanDevices(filterServiceUuid: UUID?): Flow<List<BleDevice>> = callbackFlow {
        discovered.clear()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val uuids = result.scanRecord?.serviceUuids?.mapNotNull { it.uuid } ?: emptyList()
                val name = device.name

                if (name?.contains("Plant", ignoreCase = true) != true) return

                val entry = BleDevice(
                    address = device.address,
                    name = device.name,
                    rssi = result.rssi,
                    serviceUuids = uuids
                )
                discovered[device.address] = entry
                trySend(discovered.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE })
            }

            override fun onScanFailed(errorCode: Int) {
                // Emit an empty list and let the ViewModel show an error state
                trySend(emptyList())
                // Close the flow with an exception to notify observers if desired
                close(IllegalStateException("BLE scan failed with code $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = mutableListOf<ScanFilter>()
        if (filterServiceUuid != null) {
            filters += ScanFilter.Builder().setServiceUuid(ParcelUuid(filterServiceUuid)).build()
        }

        scanner?.startScan(filters, settings, cb)
        trySend(discovered.values.toList())

        awaitClose {
            scanner?.stopScan(cb)
        }
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
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            trySend(ConnectionState.Connected(address))
                            gatt.discoverServices()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            trySend(ConnectionState.Disconnected(address, cause = "status=$status"))
                            close()
                        }
                    }
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
                    // We complete the suspended read via characteristic.setValue + a continuation map (see below).
                    completePendingRead(characteristic, status)
                }
            }

            // Keep a reference so we can disconnect later
            gatt = device.connectGatt(appContext, autoConnect, gattCallback)

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
    override suspend fun readCharacteristic(service: UUID, characteristic: UUID): ByteArray {
        val g = gatt ?: throw IllegalStateException("Not connected")
        val svc =
            g.getService(service) ?: throw IllegalArgumentException("Service $service not found")
        val chr = svc.getCharacteristic(characteristic)
            ?: throw IllegalArgumentException("Char $characteristic not found")

        val key = ReadKey(service, characteristic)
        val waiter = CompletableDeferred<Result<ByteArray>>()
        synchronized(pendingReads) { pendingReads[key] = waiter }

        if (!g.readCharacteristic(chr)) {
            synchronized(pendingReads) { pendingReads.remove(key) }
            throw IllegalStateException("readCharacteristic() returned false")
        }

        return waiter.await().getOrElse { throw it }
    }

    @SuppressLint("MissingPermission")
    override fun observeCharacteristic(
        service: UUID,
        characteristic: UUID,
        enable: Boolean
    ): Flow<ByteArray> = channelFlow {
        val g = gatt ?: run {
            close(IllegalStateException("Not connected"))
            return@channelFlow
        }
        val svc = g.getService(service) ?: run {
            close(IllegalArgumentException("Missing service $service"))
            return@channelFlow
        }
        val chr = svc.getCharacteristic(characteristic) ?: run {
            close(IllegalArgumentException("Missing characteristic $characteristic"))
            return@channelFlow
        }

        val cccd = chr.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        val flowKey = service to characteristic
        val shared = notifyFlows.getOrPut(flowKey) { MutableSharedFlow(extraBufferCapacity = 16) }

        val ok1 = g.setCharacteristicNotification(chr, enable)
        val ok2 = if (cccd != null) {
            cccd.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        } else true

        if (!ok1 || !ok2) {
            close(IllegalStateException("Failed to ${if (enable) "enable" else "disable"} notifications"))
            return@channelFlow
        }

        val job = launch { shared.collect { send(it) } }
        awaitClose {
            job.cancel()
            try {
                g.setCharacteristicNotification(chr, false)
                cccd?.let { d ->
                    d.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(d)
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
    override suspend fun stopScan() {
        // No-op here; the scan flow stops the scan in awaitClose()
        // This exists so the ViewModel can cancel its scan job or call flow's cancel.
    }

    fun clear() {
        scope.cancel()
    }
}