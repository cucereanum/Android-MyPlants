package com.example.myplants.presentation.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val readings: Map<String, String> = emptyMap(),
    val lastConnectedAddress: String? = null,
    val isReconnecting: Boolean = false
)

@HiltViewModel
class BleViewModel @Inject constructor(
    private val repo: BleManagerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BleUiState())
    val state: StateFlow<BleUiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null
    private var liveJob: Job? = null
    private var battJob: Job? = null

    init {
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
        _state.update { it.copy(scanning = false) }
        viewModelScope.launch { repo.stopScan() }
    }

    /**
     * Connect to a MI Flower Care device.
     * Note: Device will disconnect after ~6 seconds due to firmware battery-saving behavior.
     */
    fun connectTo(address: String, autoConnect: Boolean = false) {
        stopScan()

        val isReconnecting = _state.value.lastConnectedAddress == address

        connectJob?.cancel()
        liveJob?.cancel()
        battJob?.cancel()
        connectJob = null
        liveJob = null
        battJob = null

        _state.update {
            it.copy(error = null, isReconnecting = isReconnecting, lastConnectedAddress = address)
        }

        connectJob = repo.connect(address, autoConnect)
            .onEach { st ->
                if (!isReconnecting || st is ConnectionState.ServicesDiscovered || st is ConnectionState.Disconnected) {
                    _state.update { it.copy(connectionState = st, error = null) }
                }

                when (st) {
                    is ConnectionState.ServicesDiscovered -> {
                        _state.update { it.copy(isReconnecting = false) }
                        startLiveScreen()
                    }

                    is ConnectionState.Disconnected -> {
                        val shouldAutoReconnect = st.cause != null &&
                                _state.value.lastConnectedAddress == address &&
                                liveJob != null

                        if (shouldAutoReconnect) {
                            _state.update { it.copy(isReconnecting = true) }
                            viewModelScope.launch {
                                delay(500)
                                connectTo(address, autoConnect = false)
                            }
                        } else {
                            stopLiveScreen(disconnect = false)
                            _state.update { it.copy(isReconnecting = false) }
                        }
                    }

                    else -> Unit
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        error = e.message,
                        connectionState = ConnectionState.Disconnected(address, e.message),
                        isReconnecting = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        stopLiveScreen(disconnect = true)
        _state.update {
            it.copy(
                connectionState = ConnectionState.Idle,
                lastConnectedAddress = null
            )
        }
    }

    private fun startLiveScreen() {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            repo.startFlowerCareLive()
                .catch { e -> _state.update { it.copy(error = e.message) } }
                .collect { parsed ->
                    val readings = buildMap<String, String> {
                        parsed.temperatureC?.let { put("Temperature", "%.1f °C".format(it)) }
                        parsed.moisturePct?.let { put("Moisture", "$it %") }
                        parsed.lightLux?.let { put("Light", "$it lx") }
                        parsed.conductivity?.let { put("Conductivity", "$it µS/cm") }
                    }
                    _state.update { it.copy(readings = readings, error = null) }
                }
        }
    }

    private fun stopLiveScreen(disconnect: Boolean) {
        liveJob?.cancel()
        battJob?.cancel()
        liveJob = null
        battJob = null
        repo.stopFlowerCareLive()
        if (disconnect) {
            viewModelScope.launch { repo.disconnect() }
        }
    }
}
