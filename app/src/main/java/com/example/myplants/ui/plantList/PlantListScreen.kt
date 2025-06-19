package com.example.myplants.ui.plantList

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myplants.R
import com.example.myplants.infrastructure.worker.WateringCheckWorker
import com.example.myplants.navigation.Route
import com.example.myplants.ui.notifications.RequestNotificationPermission
import kotlinx.coroutines.launch

@Composable
fun PlantListScreen(
    navController: NavController, viewModel: PlantListViewModel = hiltViewModel()
) {

    val plants by viewModel.items.collectAsState()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsState()
    val rows = plants.chunked(2)
    var tapCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    println("hasUnreadNotifications $hasUnreadNotifications")
    LaunchedEffect(Unit) {
        viewModel.getPlants()
    }
    RequestNotificationPermission()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.bg_plants),
            contentScale = ContentScale.FillWidth,
            contentDescription = "Background plants"
        )

        if (viewModel.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 90.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Let's Care \nMy Plants!",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    tapCount++
                                    if (tapCount >= 5) {
                                        tapCount = 0
                                        scope.launch {
                                            val request =
                                                OneTimeWorkRequestBuilder<WateringCheckWorker>().build()
                                            WorkManager.getInstance(context).enqueue(request)
                                            Toast.makeText(
                                                context,
                                                "Debug: Worker Triggered",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            })
                        Box(
                            contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)
                        ) {
                            Box(modifier = Modifier
                                .size(60.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    navController.navigate(Route.NOTIFICATIONS)
                                }) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            if (hasUnreadNotifications) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .offset(x = -2.dp, y = 2.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                    }

                    FilterRow(
                        filterList = viewModel.filterList, selectFilter = { filter ->
                            viewModel.selectFilter(filter as PlantListFilter)
                        }, selectedFilterType = viewModel.selectedFilterType
                    )
                }


                Spacer(modifier = Modifier.padding(top = if (plants.isEmpty()) 40.dp else 0.dp))
                if (plants.isEmpty()) {
                    EmptyState(
                        navController = navController,
                        selectedFilterType = viewModel.selectedFilterType
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)

                    ) {
                        items(rows.size) { rowIndex ->
                            val rowItems = rows[rowIndex]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (rowIndex == rows.size - 1) 30.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { plant ->
                                    PlantListItem(plant,
                                        modifier = Modifier.weight(1f),
                                        onNavigateToPlantDetails = {
                                            navController.navigate(Route.plantDetailsRoute(plant.id))
                                        })
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                }
            }

            if (plants.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .blur(20.dp)
                )
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Route.ADD_EDIT_PLANT)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 30.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

            }

        }
    }
}

@Composable
fun EmptyState(
    navController: NavController, selectedFilterType: PlantListFilter
) {
    val (title, message, buttonText) = when (selectedFilterType) {
        PlantListFilter.UPCOMING -> Triple(
            "No Upcoming Plants",
            "There are no upcoming plants to water. Please add a new plant to keep track of your watering schedule.",
            "Add a Plant"
        )

        PlantListFilter.FORGOT_TO_WATER -> Triple(
            "No Missed Waterings",
            "Great job! You have no plants that you forgot to water. Stay on top of your schedule to keep your plants healthy.",
            "View Plants"
        )

        PlantListFilter.HISTORY -> Triple(
            "No Watering History",
            "Your watering history is empty. Once you water a plant, it will appear here.",
            "View Plants"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .width(360.dp)
                .height(240.dp),
            painter = painterResource(id = R.drawable.plants_center),
            contentScale = ContentScale.Fit,
            contentDescription = "Cactus plants"
        )
        Spacer(modifier = Modifier.padding(top = 30.dp))
        Text(
            modifier = Modifier.padding(top = 10.dp),
            fontWeight = FontWeight.Medium,
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 16.sp,
            text = message
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        if (selectedFilterType === PlantListFilter.UPCOMING) {
            Button(modifier = Modifier
                .width(320.dp)
                .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    navController.navigate(Route.ADD_EDIT_PLANT)
                }) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            }
        }
    }
}
