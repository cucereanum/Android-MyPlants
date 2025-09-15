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
    private var liveJob: Job? = null
    private var battJob: Job? = null

    init {
        // Observe adapter state
        viewModelScope.launch {
            repo.isBluetoothOn.collect { on ->
                _state.update { it.copy(isBluetoothOn = on) }
            }
        }
    }

    // ---------- Scan ----------
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
        scanJob?.cancel(); scanJob = null
        _state.update { it.copy(scanning = false) }
        viewModelScope.launch { repo.stopScan() }
    }

    // ---------- Connect / Live ----------
    fun connectTo(address: String, autoConnect: Boolean = false) {
        // stop scanning when user chooses a device
        stopScan()

        // cancel previous connection & live/batt loops
        connectJob?.cancel(); connectJob = null
        liveJob?.cancel(); liveJob = null
        battJob?.cancel(); battJob = null

        connectJob = repo.connect(address, autoConnect)
            .onEach { st ->
                _state.update { it.copy(connectionState = st, error = null) }

                when (st) {
                    is ConnectionState.ServicesDiscovered -> {
                        // Start continuous live polling (read 1A01 every ~2s under the hood)
                        startLiveScreen()

                        // Optional: fetch battery/firmware once after connect
                        startBatteryOnce()
                    }

                    is ConnectionState.Disconnected -> {
                        // Stop jobs; keep last readings visible
                        stopLiveScreen(disconnect = false)
                    }

                    else -> Unit
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
            .launchIn(viewModelScope)
    }

    fun disconnect() {
        connectJob?.cancel(); connectJob = null
        stopLiveScreen(disconnect = true)
        _state.update { it.copy(connectionState = ConnectionState.Idle) }
    }

    // ---------- Live screen lifecycle ----------
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

    private fun stopLiveScreen(disconnect: Boolean) {
        liveJob?.cancel(); liveJob = null
        battJob?.cancel(); battJob = null
        repo.stopFlowerCareLive()
        if (disconnect) {
            viewModelScope.launch { repo.disconnect() }
        }
    }

    // ---------- Battery (1A02) once per connect ----------
    private fun startBatteryOnce() {
        battJob?.cancel()
        battJob = viewModelScope.launch {
            try {
                delay(600) // small spacing from first live reads
                val svc = BleUuids.SERVICE_FLOWER_CARE
                val batt = repo.readCharacteristic(svc, BleUuids.CHAR_VERSION_BATTERY)
                val pct = batt.firstOrNull()?.toInt()?.and(0xFF)
                pct?.let {
                    _state.update { st ->
                        val upd = st.readings.toMutableMap()
                        upd["Battery"] = "$it %"
                        st.copy(readings = upd)
                    }
                }
            } catch (t: Throwable) {
                // non-fatal
                _state.update { it.copy(error = it.error ?: t.message) }
            }
        }
    }
}