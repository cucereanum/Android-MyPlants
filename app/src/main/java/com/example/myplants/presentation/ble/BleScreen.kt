package com.example.myplants.presentation.ble

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myplants.R
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.ble.BleUuids
import com.example.myplants.data.ble.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScreen(
    navController: NavController,
    viewModel: BleViewModel = hiltViewModel(),
    onClose: () -> Unit = { navController.popBackStack() },
    onDeviceSelected: ((BleDevice) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val fe95 = remember { BleUuids.SERVICE_XIAOMI_FE95 }
    val isLinkMode = onDeviceSelected != null

    val latestOnClose by rememberUpdatedState(onClose)
    val closeScreen = remember(viewModel, isLinkMode) {
        {
            if (isLinkMode) viewModel.stopScan() else viewModel.disconnect()
            latestOnClose()
        }
    }

    BackHandler { closeScreen() }

    DisposableEffect(isLinkMode) {
        onDispose {
            if (isLinkMode) viewModel.stopScan() else viewModel.disconnect()
        }
    }

    val requestPerms = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        if (grantMap.values.all { it }) {
            viewModel.startScan(filterServiceUuid = fe95)
        } else {
            viewModel.stopScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BLE Devices",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 32.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = closeScreen) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
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

            when (val cs = state.connectionState) {
                is ConnectionState.Connecting ->
                    AssistChip(onClick = {}, label = { Text("Connecting ${cs.deviceAddress}…") })

                is ConnectionState.Connected ->
                    AssistChip(onClick = {}, label = { Text("Connected ${cs.deviceAddress}") })

                is ConnectionState.ServicesDiscovered ->
                    AssistChip(onClick = {}, label = { Text("Services ready ${cs.deviceAddress}") })

                is ConnectionState.ScanError ->
                    Text("Scan error: ${cs.message}", color = MaterialTheme.colorScheme.error)

                else -> {}
            }

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
                    onClick = { currentAddr?.let { viewModel.connectTo(it, autoConnect = false) } }
                ) { Text(if (state.isReconnecting) "Reconnecting..." else "Refresh now") }

                OutlinedButton(
                    enabled = currentAddr != null,
                    onClick = { viewModel.disconnect() }
                ) { Text("Disconnect") }
            }

            HorizontalDivider()

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
                            onClick = {
                                if (onDeviceSelected != null) {
                                    onDeviceSelected(d)
                                } else {
                                    viewModel.connectTo(d.address, autoConnect = false)
                                }
                            }
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
