package com.example.myplants.ui.plantList

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myplants.R
import com.example.myplants.data.Plant

@Composable
fun PlantListItem(plant: Plant, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .height(200.dp)
            .width(100.dp)
            .padding(top = 20.dp)
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(8.dp))
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
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            error = painterResource(R.drawable.ic_launcher_background)
        )


    }
}