package com.example.myplants.data.ble

import android.bluetooth.BluetoothDevice
import java.util.UUID

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    val serviceUuids: List<UUID> = emptyList()
)