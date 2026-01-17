package com.example.myplants.presentation.plantList

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myplants.R
import com.example.myplants.infrastructure.worker.WateringCheckWorker
import com.example.myplants.navigation.Route
import com.example.myplants.presentation.notifications.RequestNotificationPermission
import com.example.myplants.presentation.plantList.components.FilterRow
import com.example.myplants.presentation.plantList.components.PlantListItem
import com.example.myplants.presentation.theme.LocalIsDarkTheme
import com.example.myplants.presentation.util.DebounceClick
import kotlinx.coroutines.launch

@Composable
fun PlantListScreen(
    navController: NavController, viewModel: PlantListViewModel = hiltViewModel()
) {


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsStateWithLifecycle()

    val settingsContentDescription = stringResource(id = R.string.settings_title)
    val notificationsContentDescription = stringResource(id = R.string.notifications)

    val onFilterSelected = remember<(PlantListFilter) -> Unit> {
        { filter -> viewModel.selectFilter(filter) }
    }

    val onNavigateToSettings = remember {
        { DebounceClick.debounceClick { navController.navigate(Route.SETTINGS) } }
    }

    val onNavigateToNotifications = remember {
        { DebounceClick.debounceClick { navController.navigate(Route.NOTIFICATIONS) } }
    }

    val onNavigateToAddPlant = remember {
        { DebounceClick.debounceClick { navController.navigate(Route.ADD_EDIT_PLANT) } }
    }

    RequestNotificationPermission()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.bg_plants),
            contentScale = ContentScale.FillWidth,
            contentDescription = stringResource(id = R.string.background_plants)
        )

        if (uiState.errorMessage != null && uiState.plants.isEmpty()) {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = { viewModel.retryLoading() },
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.isLoading) {
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
                        Text(
                            text = stringResource(id = R.string.home_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .clickable { onNavigateToSettings() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = settingsContentDescription,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Box(
                                contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .clickable { onNavigateToNotifications() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = notificationsContentDescription,
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
                    }

                    FilterRow(
                        filterList = viewModel.filterList,
                        selectFilter = { filter ->
                            onFilterSelected(filter as PlantListFilter)
                        },
                        selectedFilterType = uiState.selectedFilterType
                    )
                }


                val selectedIndex = remember(viewModel.filterList, uiState.selectedFilterType) {
                    viewModel.filterList.indexOf(uiState.selectedFilterType)
                }

                // Keep track of previous index to decide direction
                var lastIndex by remember { mutableIntStateOf(selectedIndex) }
                val dir = if (selectedIndex > lastIndex) 1 else -1
                LaunchedEffect(selectedIndex) { lastIndex = selectedIndex }


                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = uiState.plants,
                    transitionSpec = {
                        val enter = slideIn(
                            // fullSize: IntSize -> IntOffset
                            initialOffset = { fullSize -> IntOffset(fullSize.width * dir, 0) },
                            animationSpec = tween(300)
                        ) + fadeIn()

                        val exit = slideOut(
                            // fullSize: IntSize -> IntOffset
                            targetOffset = { fullSize -> IntOffset(-fullSize.width * dir, 0) },
                            animationSpec = tween(300)
                        ) + fadeOut()

                        enter togetherWith exit
                    }
                ) { currentPlants ->

                    Column {
                        Spacer(modifier = Modifier.padding(top = if (currentPlants.isEmpty()) 40.dp else 0.dp))

                        if (currentPlants.isEmpty()) {
                            EmptyState(
                                onNavigateToAddPlant = onNavigateToAddPlant,
                                selectedFilterType = uiState.selectedFilterType
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = currentPlants,
                                    key = { it.id }
                                ) { plant ->
                                    PlantListItem(
                                        plant,
                                        onNavigateToPlantDetails = {
                                            navController.navigate(Route.plantDetailsRoute(plant.id))
                                        }
                                    )
                                }

                                if (uiState.hasMoreToLoad) {
                                    item(span = {
                                        GridItemSpan(
                                            2
                                        )
                                    }) {
                                        LaunchedEffect(Unit) {
                                            viewModel.loadNextPage()
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (uiState.isLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(32.dp),
                                                    strokeWidth = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

            if (uiState.plants.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .blur(20.dp)
                )
                FloatingActionButton(
                    onClick = { onNavigateToAddPlant() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 30.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

            }

        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Oops! Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.error_retry),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun EmptyState(
    onNavigateToAddPlant: () -> Unit,
    selectedFilterType: PlantListFilter
) {
    val title: String
    val message: String
    val buttonText: String

    val isDarkModeEnabled = LocalIsDarkTheme.current


    when (selectedFilterType) {
        PlantListFilter.UPCOMING -> {
            title = stringResource(id = R.string.plant_list_empty_upcoming_title)
            message = stringResource(id = R.string.plant_list_empty_upcoming_message)
            buttonText = stringResource(id = R.string.plant_list_empty_upcoming_button)
        }

        PlantListFilter.FORGOT_TO_WATER -> {
            title = stringResource(id = R.string.plant_list_empty_forgot_title)
            message = stringResource(id = R.string.plant_list_empty_forgot_message)
            buttonText = stringResource(id = R.string.plant_list_empty_view_plants_button)
        }

        PlantListFilter.HISTORY -> {
            title = stringResource(id = R.string.plant_list_empty_history_title)
            message = stringResource(id = R.string.plant_list_empty_history_message)
            buttonText = stringResource(id = R.string.plant_list_empty_view_plants_button)
        }
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
            contentDescription = stringResource(id = R.string.plant_list_empty_state_image_desc)
        )
        Spacer(modifier = Modifier.padding(top = 30.dp))
        Text(
            modifier = Modifier.padding(top = 10.dp),
            fontWeight = FontWeight.Medium,
            text = title,
            color = if (!isDarkModeEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
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
            Button(
                modifier = Modifier
                    .width(320.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToAddPlant
            ) {
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
