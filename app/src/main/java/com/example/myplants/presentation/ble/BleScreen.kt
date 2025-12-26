package com.example.myplants.presentation.ble


import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myplants.data.ble.BleUuids
import com.example.myplants.data.ble.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScreen(
    navController: NavController,
    viewModel: BleViewModel = hiltViewModel(),
    onClose: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Xiaomi FE95 service UUID (scan filter)
    val fe95 = remember { BleUuids.SERVICE_XIAOMI_FE95 }

    // Make sure we disconnect if user leaves the screen (back/close)
    val latestOnClose by rememberUpdatedState(onClose)
    BackHandler {
        viewModel.disconnect()
        latestOnClose()
    }

    // Also disconnect on disposal (e.g., navigate away)
    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    // Permission launcher
    val requestPerms = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val allGranted = grantMap.values.all { it }
        if (allGranted) {
            viewModel.startScan(filterServiceUuid = fe95)
        } else {
            viewModel.stopScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Devices") },
                actions = {
                    TextButton(onClick = {
                        viewModel.disconnect()
                        onClose()
                    }) { Text("Close") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Bluetooth status + scan action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (state.isBluetoothOn) "Bluetooth: ON" else "Bluetooth: OFF",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (state.scanning) {
                    OutlinedButton(onClick = { viewModel.stopScan() }) { Text("Stop scan") }
                } else {
                    Button(
                        enabled = state.isBluetoothOn,
                        onClick = {
                            if (BlePermissions.hasAll(context)) {
                                viewModel.startScan(filterServiceUuid = fe95)
                            } else {
                                requestPerms.launch(BlePermissions.required())
                            }
                        }
                    ) { Text("Scan (Flower Care)") }
                }
            }

            // Connection state chip / errors
            when (val cs = state.connectionState) {
                is ConnectionState.Connecting ->
                    AssistChip(onClick = {}, label = { Text("Connecting ${cs.deviceAddress}…") })

                is ConnectionState.Connected ->
                    AssistChip(onClick = {}, label = { Text("Connected ${cs.deviceAddress}") })

                is ConnectionState.ServicesDiscovered ->
                    AssistChip(onClick = {}, label = { Text("Services ready ${cs.deviceAddress}") })

                is ConnectionState.Disconnected -> {
                    // Don't show error message on disconnection (auto-reconnect handles it)
                }

                is ConnectionState.ScanError ->
                    Text("Scan error: ${cs.message}", color = MaterialTheme.colorScheme.error)

                else -> {}
            }

            // Live readings card
            if (state.readings.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Plant parameters (live)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (state.isReconnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            }
                        }
                        state.readings.forEach { (k, v) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) { Text(k); Text(v) }
                        }
                    }
                }
            } else {
                Text(
                    when {
                        state.isReconnecting -> "Reconnecting…"
                        state.connectionState is ConnectionState.ServicesDiscovered ||
                                state.connectionState is ConnectionState.Connected -> "Reading live data…"

                        state.connectionState is ConnectionState.Connecting -> "Connecting…"
                        else -> "No data yet"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Controls: refresh (reconnect) & disconnect
            val currentAddr: String? =
                state.lastConnectedAddress ?: when (val cs = state.connectionState) {
                    is ConnectionState.Connecting -> cs.deviceAddress
                    is ConnectionState.Connected -> cs.deviceAddress
                    is ConnectionState.ServicesDiscovered -> cs.deviceAddress
                    is ConnectionState.Disconnected -> cs.deviceAddress
                    else -> null
                }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = currentAddr != null && !state.isReconnecting,
                    onClick = {
                        currentAddr?.let { viewModel.connectTo(it, autoConnect = false) }
                    }
                ) { Text(if (state.isReconnecting) "Reconnecting..." else "Refresh now") }

                OutlinedButton(
                    enabled = currentAddr != null,
                    onClick = { viewModel.disconnect() }
                ) { Text("Disconnect") }
            }

            HorizontalDivider()

            // Device list (tap to connect; live will start automatically after services)
            if (state.devices.isEmpty() && state.scanning) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.devices, key = { it.address }) { d ->
                        DeviceRow(
                            name = d.name ?: "Unnamed",
                            address = d.address,
                            rssi = d.rssi,
                            onClick = { viewModel.connectTo(d.address, autoConnect = false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    address: String,
    rssi: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(address, style = MaterialTheme.typography.bodySmall)
            }
            if (rssi != null) {
                Text("$rssi dBm", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}