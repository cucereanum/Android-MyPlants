package com.example.myplants.presentation.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.BleUuids
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

import java.util.UUID
import javax.inject.Inject

data class BleUiState(
    val isBluetoothOn: Boolean = false,
    val scanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Idle,
    val error: String? = null,
    val selectedDeviceName: String? = null,
    val readings: Map<String, String> = emptyMap(), // key -> pretty text
    val lastConnectedAddress: String? = null, // Track last connected device
    val isReconnecting: Boolean = false // True when reconnecting to same device
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
        // Observe Bluetooth adapter state changes
        viewModelScope.launch {
            repo.isBluetoothOn.collect { on ->
                _state.update { it.copy(isBluetoothOn = on) }
            }
        }
    }

    // Helper to check if we're reconnecting to the same device
    fun isReconnectingToSameDevice(address: String): Boolean {
        return _state.value.lastConnectedAddress == address
    }

    // ---------- BLE Scanning ----------

    /** Start scanning for BLE devices (filtered by service UUID if provided) */
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

    /** Stop the ongoing BLE scan */
    fun stopScan() {
        scanJob?.cancel(); scanJob = null
        _state.update { it.copy(scanning = false) }
        viewModelScope.launch { repo.stopScan() }
    }

    // ---------- Device Connection & Real-Time Monitoring ----------

    /**
     * Connect to a MI Flower Care device and start real-time monitoring.
     *
     * Note: Due to MI Flower Care firmware limitations, the device will disconnect
     * after ~6 seconds to save battery. This is expected behavior.
     *
     * @param address The device MAC address
     * @param autoConnect Whether to automatically reconnect if connection drops
     */
    fun connectTo(address: String, autoConnect: Boolean = false) {
        // Stop scanning when user chooses a device
        stopScan()

        // Check if this is a reconnection to the same device (to avoid showing loading state)
        val currentState = _state.value
        val isReconnecting = currentState.lastConnectedAddress == address

        // Cancel previous connection & monitoring jobs
        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null
        battJob?.cancel(); battJob = null

        _state.update {
            it.copy(
                error = null,
                isReconnecting = isReconnecting,
                lastConnectedAddress = address
            )
        }

        connectJob = repo.connect(address, autoConnect)
            .onEach { st ->
                // Only update connection state if not reconnecting, to avoid loading flickering
                if (!isReconnecting || st is ConnectionState.ServicesDiscovered || st is ConnectionState.Disconnected) {
                    _state.update { it.copy(connectionState = st, error = null) }
                }

                when (st) {
                    is ConnectionState.ServicesDiscovered -> {
                        // Mark reconnection complete
                        _state.update { it.copy(isReconnecting = false) }

                        // Start continuous live monitoring
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

    /** Disconnect from the current device and stop monitoring */
    fun disconnect() {
        connectJob?.cancel(); connectJob = null
        stopLiveScreen(disconnect = true)
        _state.update {
            it.copy(
                connectionState = ConnectionState.Idle,
                lastConnectedAddress = null
            )
        }
    }

    // ---------- Private: Monitoring Lifecycle ----------

    /** Start the live sensor data monitoring flow */
    private fun startLiveScreen() {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            repo.startFlowerCareLive()
                .catch { e ->
                    // Parser/transport error; surface but keep the last good readings
                    _state.update { it.copy(error = e.message) }
                }
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

    /** Stop the live monitoring and optionally disconnect from the device */
    private fun stopLiveScreen(disconnect: Boolean) {
        liveJob?.cancel(); liveJob = null
        battJob?.cancel(); battJob = null
        repo.stopFlowerCareLive()
        if (disconnect) {
            viewModelScope.launch { repo.disconnect() }
        }
    }
}