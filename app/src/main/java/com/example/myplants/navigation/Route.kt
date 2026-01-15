package com.example.myplants.navigation

object Route {
    const val PLANT_LIST = "plant_list"
    const val ADD_EDIT_PLANT = "add_edit_plant"
    const val PLANT_DETAILS = "plant_details/{plantId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val BLE = "ble"
    const val BLE_LINK = "ble_link/{plantId}"

    const val KEY_SELECTED_DEVICE_ADDRESS = "selected_device_address"
    const val KEY_SELECTED_DEVICE_NAME = "selected_device_name"

    fun plantDetailsRoute(plantId: Int): String {
        return "plant_details/$plantId"
    }

    fun bleLinkRoute(plantId: Int): String {
        return "ble_link/$plantId"
    }
}