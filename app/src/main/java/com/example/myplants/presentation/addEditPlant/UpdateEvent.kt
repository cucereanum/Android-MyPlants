package com.example.myplants.presentation.addEditPlant

enum class UpdateEvent {
    IMAGE_URI,
    SHOW_DIALOG,
    SHOW_CAMERA_VIEW,
    PLANT_NAME,
    TIME,
    WATER_AMOUNT,
    PLANT_SIZE,
    DESCRIPTION,
    SHOW_DATES_DIALOG,
    SHOW_TIME_DIALOG,
    SHOW_PLANT_SIZE_DIALOG,
    LENS_FACING
}

sealed class UpdateEventWithValue {
    data class UpdateTime(val hour: Int, val minute: Int) : UpdateEventWithValue()
    data class UpdateState(val type: UpdateEvent, val value: Any) : UpdateEventWithValue()
}