package com.example.myplants.presentation.plantList.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myplants.R
import com.example.myplants.data.Plant

@Composable
fun PlantListItem(
    plant: Plant,
    modifier: Modifier = Modifier,
    onNavigateToPlantDetails: (plantId: Int) -> Unit
) {

    Box(
        modifier = modifier
            .width(100.dp)
            .padding(top = 20.dp)
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFD9D9D9).copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier.clickable {
                onNavigateToPlantDetails(plant.id)
            }
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
                    .fillMaxSize()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_background)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(MaterialTheme.colorScheme.onBackground),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(end = 10.dp)
                    ) {
                        Text(
                            text = plant.plantName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = plant.description,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Box(
                            modifier = Modifier

                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color = MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}