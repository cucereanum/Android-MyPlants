package com.example.myplants.presentation.plantDetails

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.myplants.presentation.util.DebounceClick
import kotlinx.coroutines.launch

//todo: create reusable components

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun PlantDetailsScreen(
    navController: NavController, plantId: Int, viewModel: PlantDetailsViewModel = hiltViewModel()
) {
    var showModal by remember { mutableStateOf(false) }
    val linkedSensor by viewModel.linkedSensor.collectAsState()
    val connectionState by viewModel.bleConnectionState.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()

    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect {
            viewModel.refreshLinkedSensor(plantId)
        }
    }

    LaunchedEffect(linkedSensor?.deviceId) {
        if (linkedSensor != null) {
            viewModel.connectToLinkedSensor()
        }
    }

    val plant by viewModel.plant.collectAsState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val scope = rememberCoroutineScope()
    val tabs = remember { listOf("Details", "Sensor") }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

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
                contentDescription = plant?.plantName, // Assuming plantName is sufficient for CD
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            navController.popBackStack()
                        }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc),
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center),
                        tint = Color.Black
                    )
                }
                Row {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                DebounceClick.debounceClick {
                                    navController.navigate("${Route.ADD_EDIT_PLANT}/${plantId}")
                                }
                            }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(id = R.string.plant_details_edit_desc),
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center),
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                showModal = true
                            }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.plant_details_delete_desc),
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center),
                            tint = Color.Black
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, Color.White.copy(alpha = 0.45f), Color.White
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
                        .background(Color.White)

                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_size_label),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6F),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            plant?.size?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_water_label),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6F),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            plant?.waterAmount.let {
                                Text(
                                    text = "$it${stringResource(id = R.string.plant_details_water_amount_suffix)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_details_frequency_label),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6F),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            plant?.selectedDays?.joinToString(", ") { it.toString().take(3) }?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
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
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            plant?.plantName?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            plant?.description?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    1 -> {
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
                                    val sensorName = linkedSensor?.name
                                    val sensorAddress = linkedSensor?.deviceId

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Plant sensor",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        val connectionLabel = when (val cs = connectionState) {
                                            is ConnectionState.Connecting -> "Connecting"
                                            is ConnectionState.Connected -> "Connected"
                                            is ConnectionState.ServicesDiscovered -> "Reading"
                                            is ConnectionState.Disconnected -> "Idle"
                                            else -> "Idle"
                                        }

                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = { Text(connectionLabel) },
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

                                    if (linkedSensor == null) {
                                        Text(
                                            text = "No sensor linked",
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = {
                                                DebounceClick.debounceClick {
                                                    navController.navigate(
                                                        Route.bleLinkRoute(
                                                            plantId
                                                        )
                                                    )
                                                }
                                            }
                                        ) {
                                            Text(text = "Link sensor", color = Color.White)
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

                                        if (sensorReadings.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                sensorReadings.forEach { (label, value) ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Text(
                                                            text = value,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "No sensor data yet",
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = { viewModel.connectToLinkedSensor() }
                                            ) {
                                                Text("Refresh")
                                            }
                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    DebounceClick.debounceClick {
                                                        viewModel.unlinkSensor(plantId)
                                                    }
                                                }
                                            ) {
                                                Text("Unlink")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (plant?.isWatered != true) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        viewModel.onMarkAsWatered()
                        navController.popBackStack()
                    }) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.plant_details_mark_as_watered_button),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    if (showModal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.5f))
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(
                        100f, 100f, Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
                .clickable { showModal = false })

        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable { // This clickable on the dialog itself might be unintentional
                        showModal = true
                    }
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.plant_details_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.plant_details_delete_dialog_message),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { showModal = false },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1F)
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
                        onClick = {
                            DebounceClick.debounceClick {
                                plant?.let { viewModel.deletePlant() }
                                navController.popBackStack()
                                showModal = false
                            }
                        },
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
}
