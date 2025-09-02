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
        _state.update {
            it.copy(
                scanning = false,
                connectionState = ConnectionState.Idle,
                devices = emptyList()
            )
        }
        viewModelScope.launch { repo.stopScan() }
    }

    fun connectTo(address: String, autoConnect: Boolean = false) {
        // stop scanning when user chooses a device
        stopScan()

        // cancel previous connection & live loop
        connectJob?.cancel()
        liveJob?.cancel()

        connectJob = repo.connect(address, autoConnect)
            .onEach { st ->
                _state.update { it.copy(connectionState = st, error = null) }
                if (st is ConnectionState.ServicesDiscovered) {
                    runFlowerCareSessionOnce()
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
        liveJob?.cancel(); liveJob = null
        viewModelScope.launch { repo.disconnect() }
        _state.update { it.copy(connectionState = ConnectionState.Idle) }
    }

    /**
     * One Flower Care session:
     * 1) observe notify on 1A01
     * 2) write trigger (0xA0,0x1F) to 1A00
     * 3) await first notify payload (timeout)
     * 4) read 1A02 for battery/version
     * 5) update UI
     */
    private fun runFlowerCareSessionOnce(timeoutMs: Long = 3_000) {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            try {
                val svc = BleUuids.SERVICE_FLOWER_CARE
                val chrNotify = BleUuids.CHAR_REALTIME_DATA
                val chrControl = BleUuids.CHAR_CONTROL
                val chrBattVer = BleUuids.CHAR_VERSION_BATTERY

                // 1) subscribe (await CCCD in repo)
                val notifyFlow = repo.observeCharacteristic(svc, chrNotify, enable = true)

                // 2) trigger (await write in repo)
                val wrote = repo.writeCharacteristic(
                    svc, chrControl, byteArrayOf(0xA0.toByte(), 0x1F.toByte())
                )
                if (!wrote) error("Failed to write trigger (1A00)")

                // 3) await first notify payload
                val realtime = withTimeout(timeoutMs) { notifyFlow.first() }

                // 4) tiny delay before reading battery (avoid overlap)
                delay(120)

                // 5) read battery/version (safeRead = optional retry wrapper)
                val battVer = safeRead(svc, chrBattVer)

                // 6) parse & update
                val parsed = parseRealtime(realtime)
                val battery = parseBatteryPercent(battVer)
                val readings = buildMap<String, String> {
                    parsed.temperatureC?.let { put("Temperature", "%.1f °C".format(it)) }
                    parsed.moisturePct?.let { put("Moisture", "$it %") }
                    parsed.lightLux?.let { put("Light", "$it lx") }
                    parsed.conductivity?.let { put("Conductivity", "$it µS/cm") }
                    battery?.let { put("Battery", "$it %") }
                }
                _state.update { it.copy(readings = readings, error = null) }

                // do NOT call repo.disconnect() here; let the device drop (status=19 is normal)
                // since we changed the UI (below), readings remain visible even after disconnect

            } catch (t: Throwable) {
                _state.update { it.copy(error = t.message ?: "Session failed") }
            }
        }
    }

    // ---------- Parsers (Mi Flora / Flower Care) ----------

    private data class RealtimeParsed(
        val temperatureC: Double?,
        val moisturePct: Int?,
        val lightLux: Long?,
        val conductivity: Int?
    )

    /**
     * Common 16-byte realtime format used by Flower Care:
     * [0..1]=temp*10 (LE, signed), [3..6]=light (LE u32), [7]=moisture %, [8..9]=conductivity (LE u16)
     */
    private fun parseRealtime(bytes: ByteArray): RealtimeParsed {
        if (bytes.size < 10) return RealtimeParsed(null, null, null, null)

        val tRaw = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[0].toInt() and 0xFF)
        val tSigned = if ((tRaw and 0x8000) != 0) tRaw or -0x10000 else tRaw
        val tempC = tSigned / 10.0

        val light = if (bytes.size >= 7) {
            ((bytes[6].toLong() and 0xFF) shl 24) or
                    ((bytes[5].toLong() and 0xFF) shl 16) or
                    ((bytes[4].toLong() and 0xFF) shl 8) or
                    (bytes[3].toLong() and 0xFF)
        } else null

        val moisture = bytes.getOrNull(7)?.toInt()?.and(0xFF)
        val cond = if (bytes.size >= 10) {
            ((bytes[9].toInt() and 0xFF) shl 8) or (bytes[8].toInt() and 0xFF)
        } else null

        return RealtimeParsed(
            temperatureC = tempC,
            moisturePct = moisture,
            lightLux = light,
            conductivity = cond
        )
    }

    private suspend fun safeRead(service: UUID, characteristic: UUID): ByteArray {
        return try {
            repo.readCharacteristic(service, characteristic)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("returned false") == true) {
                delay(80)
                repo.readCharacteristic(service, characteristic)
            } else throw e
        }
    }

    /**
     * 1A02 typically starts with battery % in first byte; firmware may follow as ASCII.
     */
    private fun parseBatteryPercent(bytes: ByteArray): Int? =
        bytes.getOrNull(0)?.toInt()?.and(0xFF)
}