package com.example.myplants.presentation.plantList.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myplants.R
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant

sealed class WateringStatus(val isWatered: Boolean = false) {
    data object Today : WateringStatus()
    class Tomorrow(isWatered: Boolean = false) : WateringStatus(isWatered)
    data object Overdue : WateringStatus()
    class Upcoming(val dayName: String, isWatered: Boolean = false) : WateringStatus(isWatered)
}

@Composable
fun PlantListItem(
    plant: Plant,
    modifier: Modifier = Modifier,
    onNavigateToPlantDetails: (plantId: Int) -> Unit
) {
    val wateringStatus = remember(plant.selectedDays, plant.isWatered) {
        getWateringStatus(plant)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onNavigateToPlantDetails(plant.id) }
    ) {
        Column {
            // Image container with padding and status label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(plant.imageUri)
                        .crossfade(true)
                        .build(),
                    onError = {
                        Log.e(
                            "Coil",
                            "Error loading image for URI: ${plant.imageUri}",
                            it.result.throwable
                        )
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_background)
                )

                // Watering status label
                WateringStatusLabel(
                    status = wateringStatus,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Plant info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = plant.plantName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plant.description,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // Water drop button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = "Water",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WateringStatusLabel(
    status: WateringStatus,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val grayColor = Color.Gray

    val (textColor, icon, text) = when (status) {
        is WateringStatus.Today -> {
            Triple(
                primaryColor,
                Icons.Outlined.Schedule,
                stringResource(R.string.plant_card_status_today)
            )
        }

        is WateringStatus.Tomorrow -> {
            Triple(
                if (status.isWatered) grayColor else primaryColor,
                Icons.Outlined.Schedule,
                stringResource(R.string.plant_card_status_tomorrow)
            )
        }

        is WateringStatus.Overdue -> {
            Triple(
                errorColor,
                Icons.Outlined.Warning,
                stringResource(R.string.plant_card_status_overdue)
            )
        }

        is WateringStatus.Upcoming -> {
            Triple(
                if (status.isWatered) grayColor else primaryColor,
                Icons.Outlined.Schedule,
                status.dayName
            )
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

private fun getWateringStatus(plant: Plant): WateringStatus {
    val today = DayOfWeek.today()
    val allDays = DayOfWeek.allDays()
    val todayIndex = allDays.indexOf(today)
    val selectedDays = plant.selectedDays

    if (selectedDays.isEmpty()) {
        return WateringStatus.Upcoming("", plant.isWatered)
    }

    if (plant.isWatered) {
        return findNextWateringDay(selectedDays, todayIndex, allDays, isWatered = true)
    }

    if (selectedDays.contains(today)) {
        return WateringStatus.Today
    }


    for (i in 1..6) {
        val pastIndex = (todayIndex - i + 7) % 7
        val pastDay = allDays[pastIndex]
        if (selectedDays.contains(pastDay)) {
            return WateringStatus.Overdue
        }

        val nextIndex = (todayIndex + i) % 7
        if (selectedDays.contains(allDays[nextIndex])) {
            break
        }
    }

    return findNextWateringDay(selectedDays, todayIndex, allDays, isWatered = false)
}

private fun findNextWateringDay(
    selectedDays: List<DayOfWeek>,
    todayIndex: Int,
    allDays: List<DayOfWeek>,
    isWatered: Boolean
): WateringStatus {
    for (i in 1..7) {
        val nextIndex = (todayIndex + i) % 7
        val nextDay = allDays[nextIndex]
        if (selectedDays.contains(nextDay)) {
            return if (i == 1) {
                WateringStatus.Tomorrow(isWatered)
            } else {
                WateringStatus.Upcoming(nextDay.dayName, isWatered)
            }
        }
    }
    return WateringStatus.Upcoming("", isWatered)
}
