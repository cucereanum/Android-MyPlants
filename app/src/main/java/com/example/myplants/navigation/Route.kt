package com.example.myplants.navigation

object Route {
    const val PLANT_LIST = "plant_list"
    const val ADD_EDIT_PLANT = "add_edit_plant"
    const val PLANT_DETAILS = "plant_details/{plantId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val BLE = "ble"
    const val BLE_LINK = "ble_link/{plantId}"

    fun plantDetailsRoute(plantId: Int): String {
        return "plant_details/$plantId"
    }

    fun bleLinkRoute(plantId: Int): String {
        return "ble_link/$plantId"
    }
}