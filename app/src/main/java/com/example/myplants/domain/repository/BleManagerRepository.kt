package com.example.myplants.domain.repository

import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.ConnectionState
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BleManagerRepository {
    /** Emits true/false when Bluetooth adapter state changes. */
    val isBluetoothOn: Flow<Boolean>

    /** Emits running lists of discovered devices until stopped. */
    fun scanDevices(filterServiceUuid: UUID? = null): Flow<List<BleDevice>>

    /** Emits connection lifecycle states until disconnected. */
    fun connect(address: String, autoConnect: Boolean = false): Flow<ConnectionState>

    /** Disconnect the active GATT connection, if any. */
    suspend fun disconnect()

    /** Stop an ongoing scan (if started via scanDevices). */
    suspend fun stopScan()
}