package com.example.myplants.presentation.ble


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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myplants.data.ble.ConnectionState
import java.util.UUID

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
    val fe95 = remember { UUID.fromString("0000FE95-0000-1000-8000-00805F9B34FB") }

    // Request launcher for multiple permissions
    val requestPerms = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val allGranted = grantMap.values.all { it }
        if (allGranted) {
            // Scan specifically for Flower Care devices (FE95)
            viewModel.startScan(filterServiceUuid = fe95)
        } else {
            viewModel.stopScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Devices") },
                actions = { TextButton(onClick = onClose) { Text("Close") } }
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
            // Bluetooth status + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val btStatus = if (state.isBluetoothOn) "Bluetooth: ON" else "Bluetooth: OFF"
                Text(btStatus, style = MaterialTheme.typography.bodyLarge)

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

            // Current connection chip + actions
            val currentAddr: String? = when (val cs = state.connectionState) {
                is ConnectionState.Connecting -> cs.deviceAddress
                is ConnectionState.Connected -> cs.deviceAddress
                is ConnectionState.ServicesDiscovered -> cs.deviceAddress
                is ConnectionState.Disconnected -> cs.deviceAddress
                else -> null
            }

            when (val cs = state.connectionState) {
                is ConnectionState.Connecting ->
                    AssistChip(onClick = {}, label = { Text("Connecting ${cs.deviceAddress}…") })

                is ConnectionState.Connected ->
                    AssistChip(onClick = {}, label = { Text("Connected ${cs.deviceAddress}") })

                is ConnectionState.ServicesDiscovered ->
                    AssistChip(onClick = {}, label = { Text("Services ready ${cs.deviceAddress}") })

                is ConnectionState.Disconnected -> if (cs.cause != null) {
                    Text("Disconnected: ${cs.cause}", color = MaterialTheme.colorScheme.error)
                }

                is ConnectionState.ScanError ->
                    Text("Scan error: ${cs.message}", color = MaterialTheme.colorScheme.error)

                else -> {}
            }

            // Readings card + actions (Refresh/Disconnect)

            if (state.readings.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Plant parameters", style = MaterialTheme.typography.titleMedium)
                        state.readings.forEach { (k, v) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) { Text(k); Text(v) }
                        }
                    }
                }
            } else {
                Text("Reading parameters…")
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = currentAddr != null,
                    onClick = {
                        // Re-run a short session: reconnect → trigger → fetch → (timeout/disconnect)
                        currentAddr?.let { viewModel.connectTo(it, autoConnect = false) }
                    }
                ) { Text("Refresh now") }

                OutlinedButton(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
            }


            if (state.error != null) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()

            // Device list
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

            if (state.connectionState is ConnectionState.Connected ||
                state.connectionState is ConnectionState.ServicesDiscovered
            ) {
                OutlinedButton(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
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