package com.example.myplants.ui.plantDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun PlantDetailsScreen(
    navController: NavController, plantId: Int, viewModel: PlantDetailsViewModel = hiltViewModel()
) {


    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    val plant by viewModel.plant.collectAsState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.65f)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            AsyncImage(
                model = plant?.imageUri,
                contentScale = ContentScale.FillWidth,
                contentDescription = plant?.plantName,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .clickable {
                        navController.popBackStack()
                    }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Go Back",
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center),
                        tint = Color.Black
                    )
                }
                Row {
                    Box(modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            navController.popBackStack()
                        }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center),
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            plant?.let { viewModel.deletePlant(it) }
                            navController.popBackStack()
                        }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center),
                            tint = Color.Black
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.5f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(all = 20.dp)
                .verticalScroll(rememberScrollState())
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
                    modifier = Modifier.padding(top = 10.dp),
                    text = it,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

        }
    }
}