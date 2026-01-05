package com.example.myplants.presentation.plantDetails

import androidx.compose.runtime.Immutable
import com.example.myplants.data.ConnectedBleDeviceEntity
import com.example.myplants.data.Plant
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.data.repository.RealtimeParsed

@Immutable
data class PlantDetailsUiState(
    val plant: Plant? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val linkedSensor: ConnectedBleDeviceEntity? = null,
    val connectionState: ConnectionState = ConnectionState.Idle,
    val sensorReadings: RealtimeParsed? = null,
    val errorMessage: String? = null,
)
