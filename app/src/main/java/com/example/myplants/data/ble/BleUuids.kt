package com.example.myplants.data.ble

import java.util.UUID

object BleUuids {
    // Example: standard Battery Service (optional)
    val SERVICE_BATTERY: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

    // TODO: replace with your LightBlue custom service/characteristics:
    val SERVICE_PLANT: UUID = UUID.fromString("11111111-2222-3333-4444-555555555555")
    val CHAR_PLANT_TEMP: UUID =
        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee1") // e.g., temperature (Int16 or float)
    val CHAR_PLANT_MOISTURE: UUID =
        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee2") // e.g., %
    val CHAR_PLANT_NAME: UUID =
        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee3") // UTF-8 string (optional)
}