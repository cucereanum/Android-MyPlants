package com.example.myplants.ui.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.BleUuids
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.util.UUID
import javax.inject.Inject

data class BleUiState(
    val isBluetoothOn: Boolean = false,
    val scanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Idle,
    val error: String? = null,
    val selectedDeviceName: String? = null,
    val readings: Map<String, String> = emptyMap() // key -> pretty text
)

@HiltViewModel
class BleViewModel @Inject constructor(
    private val repo: BleManagerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BleUiState())
    val state: StateFlow<BleUiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    init {
        // Observe adapter state
        viewModelScope.launch {
            repo.isBluetoothOn.collect { on ->
                _state.update { it.copy(isBluetoothOn = on) }
            }
        }
    }

    fun startScan(filterServiceUuid: UUID? = null) {
        if (_state.value.scanning) return
        _state.update {
            it.copy(
                scanning = true,
                error = null,
                devices = emptyList(),
                connectionState = ConnectionState.Scanning
            )
        }
        scanJob = viewModelScope.launch {
            repo.scanDevices(filterServiceUuid)
                .catch { e ->
                    _state.update {
                        it.copy(
                            scanning = false,
                            error = e.message,
                            connectionState = ConnectionState.ScanError(e.message.orEmpty())
                        )
                    }
                }
                .collect { list ->
                    _state.update { it.copy(devices = list) }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(scanning = false, connectionState = ConnectionState.Idle) }
        viewModelScope.launch { repo.stopScan() }
    }

    fun connectTo(address: String, autoConnect: Boolean = false) {
        // cancel any previous connection stream
        connectJob?.cancel()

        connectJob = repo.connect(address, autoConnect)
            .onEach { st ->
                _state.update { it.copy(connectionState = st, error = null) }
                if (st is ConnectionState.ServicesDiscovered) {
                    // kick off one-time reads (this function should launch its own coroutine)
                    readPlantParamsOnce()
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        error = e.message,
                        connectionState = ConnectionState.Disconnected(address, e.message)
                    )
                }
            }
            .launchIn(viewModelScope) // <-- non-suspending; safe to call from onClick
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        viewModelScope.launch { repo.disconnect() }
        _state.update { it.copy(connectionState = ConnectionState.Idle) }
    }

    private fun readPlantParamsOnce() {
        viewModelScope.launch {
            try {
                // Read temperature (Int16 little-endian, tenths of °C)
                val raw = repo.readCharacteristic(BleUuids.SERVICE_PLANT, BleUuids.CHAR_PLANT_TEMP)

                // little-endian Int16
                val lo = raw.getOrNull(0)?.toInt() ?: 0
                val hi = raw.getOrNull(1)?.toInt() ?: 0
                val value = (hi shl 8) or (lo and 0xFF)
                // interpret as signed 16-bit
                val signed = if (value and 0x8000 != 0) value or -0x10000 else value
                val celsius = signed / 10.0

                _state.update { it.copy(readings = mapOf("Temperature" to "%.1f °C".format(celsius))) }
            } catch (e: Throwable) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}