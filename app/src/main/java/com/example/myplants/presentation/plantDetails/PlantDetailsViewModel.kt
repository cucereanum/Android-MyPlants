package com.example.myplants.presentation.plantDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.ConnectedBleDeviceEntity
import com.example.myplants.data.Plant
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.repository.BleDatabaseRepository
import com.example.myplants.domain.repository.BleManagerRepository
import com.example.myplants.data.repository.RealtimeParsed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantDetailsViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val bleDatabaseRepository: BleDatabaseRepository,
    private val bleManagerRepository: BleManagerRepository,
) : ViewModel() {

    private val _plant = MutableStateFlow<Plant?>(null)
    val plant: StateFlow<Plant?> = _plant

    private val _linkedSensor = MutableStateFlow<ConnectedBleDeviceEntity?>(null)
    val linkedSensor: StateFlow<ConnectedBleDeviceEntity?> = _linkedSensor

    private val _bleConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val bleConnectionState: StateFlow<ConnectionState> = _bleConnectionState

    private val _sensorReadings = MutableStateFlow<Map<String, String>>(emptyMap())
    val sensorReadings: StateFlow<Map<String, String>> = _sensorReadings.asStateFlow()

    private var connectJob: Job? = null
    private var liveJob: Job? = null

    fun loadPlant(plantId: Int) {
        viewModelScope.launch {
            val result = repository.getPlantById(plantId)
            _plant.value = result
            _linkedSensor.value = bleDatabaseRepository.getDeviceByPlantId(plantId)
        }
    }

    fun connectToLinkedSensor() {
        val address = _linkedSensor.value?.deviceId ?: return

        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null

        connectJob = bleManagerRepository
            .connect(address, autoConnect = false)
            .onEach { state ->
                _bleConnectionState.value = state
                if (state is ConnectionState.ServicesDiscovered) {
                    startLiveReadings()
                }
            }
            .catch { throwable ->
                _bleConnectionState.value = ConnectionState.Disconnected(address, throwable.message)
            }
            .launchIn(viewModelScope)
    }

    private fun startLiveReadings() {
        liveJob?.cancel(); liveJob = null
        liveJob = bleManagerRepository
            .startFlowerCareLive()
            .onEach { parsed ->
                _sensorReadings.value = parsed.toPrettyReadings()
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
        _bleConnectionState.value = ConnectionState.Idle
    }

    fun unlinkSensor(plantId: Int) {
        viewModelScope.launch {
            disconnectSensor()
            bleDatabaseRepository.forgetDeviceByPlant(plantId)
            _linkedSensor.value = null
            _sensorReadings.value = emptyMap()
        }
    }

    fun refreshLinkedSensor(plantId: Int) {
        viewModelScope.launch {
            _linkedSensor.value = bleDatabaseRepository.getDeviceByPlantId(plantId)
        }
    }

    private fun RealtimeParsed.toPrettyReadings(): Map<String, String> {
        return buildMap {
            temperatureC?.let { put("Temperature", "%.1f °C".format(it)) }
            moisturePct?.let { put("Moisture", "$it %") }
            lightLux?.let { put("Light", "$it lx") }
            conductivity?.let { put("Conductivity", "$it µS/cm") }
        }
    }

    fun deletePlant() {
        _plant.value?.let { plant ->
            viewModelScope.launch {
                unlinkSensor(plant.id)
                repository.deletePlant(plant)
                _plant.value = null // Clear state after deletion
            }
        }
    }

    fun onMarkAsWatered() {
        _plant.value?.let { currentPlant ->
            val updatedPlant = currentPlant.copy(isWatered = true)

            viewModelScope.launch {
                repository.updatePlant(updatedPlant)
                _plant.value = updatedPlant // Update state after API call
            }
        }
    }

}