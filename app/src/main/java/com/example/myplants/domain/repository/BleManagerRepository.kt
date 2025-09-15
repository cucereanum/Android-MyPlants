package com.example.myplants.domain.repository

import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.data.repository.RealtimeParsed
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

    /** Read a characteristic once. Throws if not connected or missing. */
    suspend fun readCharacteristic(service: UUID, characteristic: UUID): ByteArray

    /** Write a characteristic once. Returns true if the write was accepted by the stack. */
    suspend fun writeCharacteristic(
        service: UUID,
        characteristic: UUID,
        value: ByteArray,
        writeType: Int
    ): Boolean

    /** Enable/disable notifications and emit updates for this characteristic. */
    fun observeCharacteristic(
        service: UUID,
        characteristic: UUID,
        enable: Boolean = true
    ): Flow<ByteArray>

    // ---------------- Flower Care (session-based) convenience ----------------

    /**
     * One-shot Flower Care read:
     * 1) enable notify on 1A01
     * 2) write trigger (0xA0,0x1F) to 1A00
     * 3) await first notify payload from 1A01 (realtime data)
     * 4) read 1A02 (battery+fw)
     *
     * Returns Pair(realtimePayload, batteryFirmwarePayload).
     * Caller decides when to disconnect (typically right after this).
     */
    suspend fun readFlowerCareOnce(timeoutMs: Long = 3_000): Pair<ByteArray, ByteArray>

    /** Stream real-time parsed data while screen is active. */
    fun startFlowerCareLive(): Flow<RealtimeParsed>

    /** Stop live polling without disconnecting. */
    fun stopFlowerCareLive()
}