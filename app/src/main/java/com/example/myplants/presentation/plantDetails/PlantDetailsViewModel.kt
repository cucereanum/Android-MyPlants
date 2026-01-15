package com.example.myplants.presentation.plantDetails

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.example.myplants.data.Plant
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.R
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.repository.BleDatabaseRepository
import com.example.myplants.domain.repository.BleManagerRepository
import com.example.myplants.widget.WidgetUpdateManager
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
    private val application: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(PlantDetailsUiState())
    val state: StateFlow<PlantDetailsUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PlantDetailsEffect>()
    val effect: SharedFlow<PlantDetailsEffect> = _effect.asSharedFlow()

    private var connectJob: Job? = null
    private var liveJob: Job? = null

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
        }
    }

    fun connectToLinkedSensor() {
        val address = _state.value.linkedSensor?.deviceId ?: return

        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null

        connectJob = bleManagerRepository
            .connect(address, autoConnect = false)
            .onEach { state ->
                _state.update { it.copy(connectionState = state, errorMessage = null) }
                if (state is ConnectionState.ServicesDiscovered) {
                    startLiveReadings()
                }
            }
            .catch { throwable ->
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
        liveJob?.cancel(); liveJob = null
        liveJob = bleManagerRepository
            .startFlowerCareLive()
            .onEach { parsed ->
                _state.update { it.copy(sensorReadings = parsed, errorMessage = null) }
            }
            .catch {
                // Keep last readings; connection state will surface errors.
            }
            .launchIn(viewModelScope)
    }

    fun disconnectSensor() {
        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null
        bleManagerRepository.stopFlowerCareLive()
        viewModelScope.launch { bleManagerRepository.disconnect() }
        _state.update { it.copy(connectionState = ConnectionState.Idle) }
    }

    fun unlinkSensor(plantId: Int) {
        viewModelScope.launch {
            disconnectSensor()
            bleDatabaseRepository.forgetDeviceByPlant(plantId)
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
        viewModelScope.launch {
            val linkedSensor = runCatching { bleDatabaseRepository.getDeviceByPlantId(plantId) }
                .getOrElse { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                    return@launch
                }
            _state.update { it.copy(linkedSensor = linkedSensor, errorMessage = null) }
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

                // Update widgets after deleting a plant
                WidgetUpdateManager.updateAllWidgets(application)

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

                // Update widgets after marking plant as watered
                WidgetUpdateManager.updateAllWidgets(application)
            }
        }
    }

}

sealed interface PlantDetailsEffect {
    data object NavigateBack : PlantDetailsEffect
    data class ShowMessage(@StringRes val messageResId: Int) : PlantDetailsEffect
}