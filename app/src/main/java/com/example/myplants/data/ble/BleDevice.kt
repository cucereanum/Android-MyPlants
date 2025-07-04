package com.example.myplants.data.ble

import android.bluetooth.BluetoothDevice

data class BleDevice(
    val id: Int,
    val device: BluetoothDevice,
    val name: String?,
    val address: String
)
