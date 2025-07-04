package com.example.myplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connected_ble_devices")
data class ConnectedBleDeviceEntity(
    @PrimaryKey val deviceId: String,
    val plantId: Int,
    val name: String?,
    val lastConnected: Long
)
