package com.example.myplants.presentation.plantDetails

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myplants.R
import com.example.myplants.data.ble.ConnectionState
import com.example.myplants.navigation.Route
import com.example.myplants.presentation.components.CircularIconButton
import com.example.myplants.presentation.components.PrimaryButton
import com.example.myplants.presentation.util.DebounceClick
import com.example.myplants.widget.WidgetUtils
import androidx.compose.material.icons.filled.WaterDrop
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun PlantDetailsScreen(
    navController: NavController, plantId: Int, viewModel: PlantDetailsViewModel = hiltViewModel()
) {
    var showModal by remember { mutableStateOf(false) }

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val plant = uiState.plant

    val context = LocalContext.current

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(id = R.string.plant_details_tab_details),
        stringResource(id = R.string.plant_details_tab_sensor),
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, plantId) {
        viewModel.loadPlant(plantId)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLinkedSensor(plantId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.disconnectSensor()
        }
    }

    // Handle BLE device selection from navigation result
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val deviceAddress = savedStateHandle?.get<String>(Route.KEY_SELECTED_DEVICE_ADDRESS)
    val deviceName = savedStateHandle?.get<String>(Route.KEY_SELECTED_DEVICE_NAME)

    LaunchedEffect(deviceAddress) {
        if (deviceAddress != null) {
            savedStateHandle.remove<String>(Route.KEY_SELECTED_DEVICE_ADDRESS)
            savedStateHandle.remove<String>(Route.KEY_SELECTED_DEVICE_NAME)
            viewModel.linkAndConnectSensor(plantId, deviceAddress, deviceName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PlantDetailsEffect.NavigateBack -> navController.popBackStack()
                is PlantDetailsEffect.ShowMessage -> {
                    Toast.makeText(
                        context, context.getString(effect.messageResId), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.55f)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            AsyncImage(
                model = plant?.imageUri,
                contentDescription = plant?.plantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularIconButton(
                    icon = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc),
                    onClick = { navController.popBackStack() },
                    iconSize = 30.dp
                )
                Row {
                    CircularIconButton(
                        icon = Icons.Outlined.Widgets,
                        contentDescription = stringResource(id = R.string.plant_details_add_widget),
                        onClick = {
                            DebounceClick.debounceClick {
                                if (WidgetUtils.isWidgetPinningSupported(context)) {
                                    scope.launch {
                                        WidgetUtils.requestPinSinglePlantWidget(context, plantId)
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please add widgets manually from your home screen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = stringResource(id = R.string.plant_details_edit_desc),
                        onClick = {
                            DebounceClick.debounceClick {
                                navController.navigate("${Route.ADD_EDIT_PLANT}/${plantId}")
                            }
                        },
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = stringResource(id = R.string.plant_details_delete_desc),
                        onClick = { showModal = true },
                        iconSize = 22.dp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(160.dp)
            ) {
                Column(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .height(70.dp)
                        .padding(horizontal = 30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_size_label).uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            plant?.size?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_water_label).uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            plant?.waterAmount?.let { waterAmount ->
                                Text(
                                    text = "$waterAmount${stringResource(id = R.string.plant_details_water_amount_suffix)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_frequency_label).uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            plant?.selectedDays?.joinToString(", ") { it.toString().take(3) }?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.55f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            plant?.plantName?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { scope.launch { pagerState.animateScrollToPage(index) } },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState, modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> DetailsTab(plant)
                    1 -> SensorTab(
                        uiState = uiState,
                        plantId = plantId,
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }

            if (plant?.isWatered != true) {
                PrimaryButton(
                    text = stringResource(id = R.string.plant_details_mark_as_watered_button),
                    onClick = {
                        viewModel.onMarkAsWatered()
                        navController.popBackStack()
                    },
                    icon = Icons.Filled.WaterDrop,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }

    if (uiState.isLoading) {
        LoadingOverlay()
    }

    if (!uiState.isLoading && plant == null && uiState.errorMessage != null) {
        ErrorState(
            message = uiState.errorMessage ?: "Failed to load plant",
            onRetry = { viewModel.loadPlant(plantId) })
    }

    if (uiState.isDeleting) {
        LoadingOverlay()
    }

    if (showModal) {
        DeleteConfirmationDialog(onDismiss = { showModal = false }, onConfirm = {
            DebounceClick.debounceClick {
                showModal = false
                viewModel.deletePlant()
            }
        })
    }
}

@Composable
private fun DetailsTab(plant: com.example.myplants.data.Plant?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.add_edit_plant_description_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = plant?.description ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SensorTab(
    uiState: PlantDetailsUiState,
    plantId: Int,
    navController: NavController,
    viewModel: PlantDetailsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val sensorName = uiState.linkedSensor?.name
                val sensorAddress = uiState.linkedSensor?.deviceId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.plant_details_sensor_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val connectionLabelResId = when (uiState.connectionState) {
                        is ConnectionState.Connecting -> R.string.plant_details_sensor_status_connecting
                        is ConnectionState.Connected -> R.string.plant_details_sensor_status_connected
                        is ConnectionState.ServicesDiscovered -> R.string.plant_details_sensor_status_reading
                        else -> R.string.plant_details_sensor_status_idle
                    }

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(id = connectionLabelResId)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (!sensorName.isNullOrBlank()) {
                    Text(
                        text = sensorName,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (uiState.linkedSensor == null) {
                    Text(
                        text = stringResource(id = R.string.plant_details_sensor_no_linked),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            DebounceClick.debounceClick {
                                navController.navigate(Route.bleLinkRoute(plantId))
                            }
                        }) {
                        Text(
                            text = stringResource(id = R.string.plant_details_sensor_link_button),
                            color = Color.White
                        )
                    }
                } else {
                    if (!sensorAddress.isNullOrBlank()) {
                        Text(
                            text = sensorAddress,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val parsed = uiState.sensorReadings
                    if (parsed != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            parsed.temperatureC?.let {
                                SensorReadingRow(
                                    label = stringResource(id = R.string.plant_details_sensor_label_temperature),
                                    value = stringResource(
                                        id = R.string.plant_details_sensor_value_temperature, it
                                    )
                                )
                            }
                            parsed.moisturePct?.let {
                                SensorReadingRow(
                                    label = stringResource(id = R.string.plant_details_sensor_label_moisture),
                                    value = stringResource(
                                        id = R.string.plant_details_sensor_value_percent, it
                                    )
                                )
                            }
                            parsed.lightLux?.let {
                                SensorReadingRow(
                                    label = stringResource(id = R.string.plant_details_sensor_label_light),
                                    value = stringResource(
                                        id = R.string.plant_details_sensor_value_lux, it
                                    )
                                )
                            }
                            parsed.conductivity?.let {
                                SensorReadingRow(
                                    label = stringResource(id = R.string.plant_details_sensor_label_conductivity),
                                    value = stringResource(
                                        id = R.string.plant_details_sensor_value_conductivity, it
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.plant_details_sensor_no_data),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.connectToLinkedSensor() }) {
                            Text(stringResource(id = R.string.plant_details_sensor_refresh_button))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f), onClick = {
                                DebounceClick.debounceClick { viewModel.unlinkSensor(plantId) }
                            }) {
                            Text(stringResource(id = R.string.plant_details_sensor_unlink_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 6.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            .clickable { onDismiss() })

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable { }
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.plant_details_delete_dialog_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(id = R.string.plant_details_delete_dialog_message),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1F)
                    ),
                    modifier = Modifier
                        .width(140.dp)
                        .height(40.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    )
                }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .width(140.dp)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = stringResource(id = R.string.plant_details_delete_dialog_confirm_button),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorReadingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.secondary)
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
