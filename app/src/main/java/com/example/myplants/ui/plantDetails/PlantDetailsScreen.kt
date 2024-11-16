package com.example.myplants.ui.plantDetails

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myplants.ui.plantList.PlantListViewModel

@Composable
fun PlantDetailsScreen(
    navController: NavController, plantId: Int, viewModel: PlantDetailsViewModel = hiltViewModel()
) {

    // Trigger the plant loading
    LaunchedEffect(plantId) {
        println("Called!!!")
        viewModel.loadPlant(plantId)
    }

    val plant by viewModel.plant.collectAsState()


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
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
        }
    }
}