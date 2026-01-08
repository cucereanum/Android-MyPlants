package com.example.myplants.presentation.plantDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.example.myplants.data.Plant
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.R
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.repository.BleDatabaseRepository
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantDetailsViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val bleDatabaseRepository: BleDatabaseRepository,
    private val bleManagerRepository: BleManagerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlantDetailsUiState())
    val state: StateFlow<PlantDetailsUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PlantDetailsEffect>()
    val effect: SharedFlow<PlantDetailsEffect> = _effect.asSharedFlow()

    private var connectJob: Job? = null
    private var liveJob: Job? = null
    private var lastConnectedAddress: String? = null
    private var isConnecting: Boolean = false
    private var hasCompletedInitialLoad: Boolean = false
    private var lastKnownSensorDeviceId: String? = null

    fun loadPlant(plantId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val plant: Plant? = runCatching { repository.getPlantById(plantId) }
                .getOrElse { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    return@launch
                }

            val linkedSensor = runCatching { bleDatabaseRepository.getDeviceByPlantId(plantId) }
                .getOrElse { throwable ->
                    _state.update {
                        it.copy(
                            plant = plant,
                            isLoading = false,
                            errorMessage = throwable.message,
                        )
                    }
                    return@launch
                }

            _state.update { it.copy(plant = plant, linkedSensor = linkedSensor, isLoading = false) }

            // Mark initial load as complete and track the sensor ID
            hasCompletedInitialLoad = true
            lastKnownSensorDeviceId = linkedSensor?.deviceId
        }
    }

    fun connectToLinkedSensor() {
        val address = _state.value.linkedSensor?.deviceId ?: return

        // Prevent multiple concurrent connection attempts to the same device
        if (isConnecting && lastConnectedAddress == address) {
            android.util.Log.d("PlantDetails", "Already connecting to $address, skipping")
            return
        }

        android.util.Log.d("PlantDetails", "connectToLinkedSensor called for $address")

        // First, ensure any previous connection is properly cleaned up
        connectJob?.cancel()
        liveJob?.cancel()

        // Small delay to ensure cleanup is complete before starting new connection
        viewModelScope.launch {
            // Disconnect to reset the BLE repository state
            bleManagerRepository.disconnect()
            kotlinx.coroutines.delay(200)
            startConnection(address)
        }
    }

    private fun startConnection(address: String) {
        android.util.Log.d("PlantDetails", "startConnection: Starting connection to $address")
        isConnecting = true
        lastConnectedAddress = address

        connectJob = bleManagerRepository
            .connect(address, autoConnect = false)
            .onEach { state ->
                android.util.Log.d("PlantDetails", "Connection state received: $state")
                _state.update { it.copy(connectionState = state, errorMessage = null) }

                when (state) {
                    is ConnectionState.ServicesDiscovered -> {
                        android.util.Log.d(
                            "PlantDetails",
                            "ServicesDiscovered - starting live readings"
                        )
                        isConnecting = false
                        startLiveReadings()
                    }

                    is ConnectionState.Disconnected -> {
                        android.util.Log.d("PlantDetails", "Disconnected: cause=${state.cause}")
                        isConnecting = false
                        // Stay in Idle state - user can manually refresh if needed
                        _state.update { it.copy(connectionState = ConnectionState.Idle) }
                    }

                    else -> {}
                }
            }
            .catch { throwable ->
                android.util.Log.e(
                    "PlantDetails",
                    "Connection error: ${throwable.message}",
                    throwable
                )
                isConnecting = false
                _state.update {
                    it.copy(
                        connectionState = ConnectionState.Disconnected(address, throwable.message),
                        errorMessage = throwable.message,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startLiveReadings() {
        android.util.Log.d("PlantDetails", "startLiveReadings: Starting live readings flow")
        liveJob?.cancel()
        liveJob = bleManagerRepository
            .startFlowerCareLive()
            .onEach { parsed ->
                android.util.Log.d(
                    "PlantDetails",
                    "Received sensor data: temp=${parsed.temperatureC}, moisture=${parsed.moisturePct}"
                )
                _state.update { it.copy(sensorReadings = parsed, errorMessage = null) }
            }
            .catch { e ->
                // Log error but keep last readings; connection state will surface errors
                android.util.Log.w("PlantDetails", "Live readings error: ${e.message}", e)
            }
            .launchIn(viewModelScope)
        android.util.Log.d("PlantDetails", "startLiveReadings: Live job started")
    }

    fun disconnectSensor() {
        isConnecting = false
        lastConnectedAddress = null
        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null
        bleManagerRepository.stopFlowerCareLive()
        viewModelScope.launch { bleManagerRepository.disconnect() }
        _state.update { it.copy(connectionState = ConnectionState.Idle, sensorReadings = null) }
    }

    fun unlinkSensor(plantId: Int) {
        viewModelScope.launch {
            disconnectSensor()
            bleDatabaseRepository.forgetDeviceByPlant(plantId)
            // Reset tracked sensor ID so next linked sensor will auto-connect
            lastKnownSensorDeviceId = null
            _state.update {
                it.copy(
                    linkedSensor = null,
                    sensorReadings = null,
                    errorMessage = null
                )
            }
        }
    }

    fun refreshLinkedSensor(plantId: Int) {
        android.util.Log.d(
            "PlantDetails",
            "refreshLinkedSensor called: hasCompletedInitialLoad=$hasCompletedInitialLoad, lastKnownSensorDeviceId=$lastKnownSensorDeviceId"
        )

        viewModelScope.launch {
            val linkedSensor = runCatching { bleDatabaseRepository.getDeviceByPlantId(plantId) }
                .getOrElse { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                    return@launch
                }
            _state.update { it.copy(linkedSensor = linkedSensor, errorMessage = null) }

            val newDeviceId = linkedSensor?.deviceId

            android.util.Log.d(
                "PlantDetails",
                "refreshLinkedSensor: newDeviceId=$newDeviceId, lastKnownSensorDeviceId=$lastKnownSensorDeviceId, hasCompletedInitialLoad=$hasCompletedInitialLoad"
            )

            // Auto-connect if:
            // 1. Initial load is complete (not first composition)
            // 2. We didn't have a sensor before (lastKnownSensorDeviceId was null)
            // 3. We now have a sensor (newDeviceId is not null)
            val shouldAutoConnect = hasCompletedInitialLoad &&
                    lastKnownSensorDeviceId == null &&
                    newDeviceId != null

            android.util.Log.d("PlantDetails", "shouldAutoConnect=$shouldAutoConnect")

            if (shouldAutoConnect) {
                android.util.Log.d(
                    "PlantDetails",
                    "New sensor linked, auto-connecting to $newDeviceId"
                )
                lastKnownSensorDeviceId = newDeviceId
                connectToLinkedSensor()
            } else {
                // Update the tracked sensor ID
                lastKnownSensorDeviceId = newDeviceId
            }
        }
    }

    fun deletePlant() {
        val plant = _state.value.plant ?: return
        if (_state.value.isDeleting) return

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                unlinkSensor(plant.id)
                repository.deletePlant(plant)
                _state.update { it.copy(isDeleting = false, errorMessage = null) }

                _effect.emit(
                    PlantDetailsEffect.ShowMessage(
                        R.string.plant_details_deleted_successfully_message
                    )
                )
                _effect.emit(PlantDetailsEffect.NavigateBack)
            } catch (t: Throwable) {
                _state.update { it.copy(isDeleting = false, errorMessage = t.message) }
            }
        }
    }

    fun onMarkAsWatered() {
        _state.value.plant?.let { currentPlant ->
            val updatedPlant = currentPlant.copy(isWatered = true)

            viewModelScope.launch {
                repository.updatePlant(updatedPlant)
                _state.update { it.copy(plant = updatedPlant, errorMessage = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up BLE connection when ViewModel is destroyed
        isConnecting = false
        lastConnectedAddress = null
        connectJob?.cancel()
        liveJob?.cancel()
        bleManagerRepository.stopFlowerCareLive()
        // Don't call disconnect() here as it's a suspend function and viewModelScope is cancelled
    }

}

sealed interface PlantDetailsEffect {
    data object NavigateBack : PlantDetailsEffect
    data class ShowMessage(@StringRes val messageResId: Int) : PlantDetailsEffect
}