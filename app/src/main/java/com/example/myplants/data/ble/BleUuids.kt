package com.example.myplants.data.ble

import java.util.UUID

object BleUuids {
    // Optional: Standard Battery Service (not used by Flower Care, but many BLE devices support it)
    val SERVICE_BATTERY: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

    // Flower Care main service
    val SERVICE_FLOWER_CARE: UUID =
        UUID.fromString("00001204-0000-1000-8000-00805F9B34FB")

    // Control / Trigger measurement (Write)
    val CHAR_CONTROL: UUID =
        UUID.fromString("00001A00-0000-1000-8000-00805F9B34FB")

    // Real-time sensor data (Notify / Read) -> temp, light, moisture, conductivity
    val CHAR_REALTIME_DATA: UUID =
        UUID.fromString("00001A01-0000-1000-8000-00805F9B34FB")

    // Battery level & firmware info (Read)
    val CHAR_VERSION_BATTERY: UUID =
        UUID.fromString("00001A02-0000-1000-8000-00805F9B34FB")

    // History control (Notify / Read / Write)
    val CHAR_HISTORY_CONTROL: UUID =
        UUID.fromString("00001A10-0000-1000-8000-00805F9B34FB")

    // History data (Read)
    val CHAR_HISTORY_DATA: UUID =
        UUID.fromString("00001A11-0000-1000-8000-00805F9B34FB")

    // Device time / history metadata (Read)
    val CHAR_HISTORY_TIME: UUID =
        UUID.fromString("00001A12-0000-1000-8000-00805F9B34FB")
}