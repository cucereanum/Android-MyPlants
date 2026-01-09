package com.example.myplants.presentation.plantDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.example.myplants.data.Plant
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.R
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.repository.BleDatabaseRepository
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
                        it.copy(plant = plant, isLoading = false, errorMessage = throwable.message)
                    }
                    return@launch
                }

            _state.update { it.copy(plant = plant, linkedSensor = linkedSensor, isLoading = false) }
            hasCompletedInitialLoad = true
            lastKnownSensorDeviceId = linkedSensor?.deviceId
        }
    }

    fun connectToLinkedSensor() {
        val address = _state.value.linkedSensor?.deviceId ?: return
        if (isConnecting && lastConnectedAddress == address) return

        connectJob?.cancel()
        liveJob?.cancel()

        val currentConnectionState = _state.value.connectionState

        viewModelScope.launch {
            if (currentConnectionState !is ConnectionState.Idle) {
                bleManagerRepository.disconnect()
                delay(200)
            }
            startConnection(address)
        }
    }

    private fun startConnection(address: String) {
        isConnecting = true
        lastConnectedAddress = address

        connectJob = bleManagerRepository
            .connect(address, autoConnect = false)
            .onEach { state ->
                _state.update { it.copy(connectionState = state, errorMessage = null) }

                when (state) {
                    is ConnectionState.ServicesDiscovered -> {
                        isConnecting = false
                        startLiveReadings()
                    }

                    is ConnectionState.Disconnected -> {
                        isConnecting = false
                        _state.update { it.copy(connectionState = ConnectionState.Idle) }
                    }

                    else -> {}
                }
            }
            .catch { throwable ->
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
        liveJob?.cancel()
        liveJob = bleManagerRepository
            .startFlowerCareLive()
            .onEach { parsed ->
                _state.update { it.copy(sensorReadings = parsed, errorMessage = null) }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun disconnectSensor() {
        isConnecting = false
        lastConnectedAddress = null
        connectJob?.cancel()
        liveJob?.cancel()
        connectJob = null
        liveJob = null
        bleManagerRepository.stopFlowerCareLive()
        viewModelScope.launch { bleManagerRepository.disconnect() }
        _state.update { it.copy(connectionState = ConnectionState.Idle, sensorReadings = null) }
    }

    fun unlinkSensor(plantId: Int) {
        viewModelScope.launch {
            disconnectSensor()
            bleDatabaseRepository.forgetDeviceByPlant(plantId)
            lastKnownSensorDeviceId = null
            _state.update {
                it.copy(linkedSensor = null, sensorReadings = null, errorMessage = null)
            }
        }
    }

    fun refreshLinkedSensor(plantId: Int) {
        viewModelScope.launch {
            val linkedSensor = runCatching { bleDatabaseRepository.getDeviceByPlantId(plantId) }
                .getOrElse { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                    return@launch
                }
            _state.update { it.copy(linkedSensor = linkedSensor, errorMessage = null) }
            lastKnownSensorDeviceId = linkedSensor?.deviceId
        }
    }

    fun linkAndConnectSensor(plantId: Int, deviceAddress: String, deviceName: String?) {
        viewModelScope.launch {
            delay(100) // Ensure BleScreen cleanup is complete

            val device = BleDevice(
                address = deviceAddress,
                name = deviceName,
                rssi = null,
                serviceUuids = emptyList()
            )

            runCatching { bleDatabaseRepository.linkDeviceToPlant(plantId, device) }
                .onFailure { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                    return@launch
                }

            val linkedSensor = runCatching { bleDatabaseRepository.getDeviceByPlantId(plantId) }
                .getOrElse { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                    return@launch
                }

            _state.update { it.copy(linkedSensor = linkedSensor, errorMessage = null) }
            lastKnownSensorDeviceId = linkedSensor?.deviceId
            connectToLinkedSensor()
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
                _effect.emit(PlantDetailsEffect.ShowMessage(R.string.plant_details_deleted_successfully_message))
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
        isConnecting = false
        lastConnectedAddress = null
        connectJob?.cancel()
        liveJob?.cancel()
        bleManagerRepository.stopFlowerCareLive()
    }
}

sealed interface PlantDetailsEffect {
    data object NavigateBack : PlantDetailsEffect
    data class ShowMessage(@StringRes val messageResId: Int) : PlantDetailsEffect
}
