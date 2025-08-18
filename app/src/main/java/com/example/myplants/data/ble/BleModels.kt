package com.example.myplants.data.ble

import java.util.UUID


sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Scanning : ConnectionState()
    data class ScanError(val message: String) : ConnectionState()
    data class Connecting(val deviceAddress: String) : ConnectionState()
    data class Connected(val deviceAddress: String) : ConnectionState()
    data class ServicesDiscovered(val deviceAddress: String) : ConnectionState()
    data class Disconnected(val deviceAddress: String?, val cause: String? = null) :
        ConnectionState()
}

data class CharacteristicValue(
    val service: UUID,
    val characteristic: UUID,
    val value: ByteArray
)