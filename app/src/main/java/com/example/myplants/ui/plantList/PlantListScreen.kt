package com.example.myplants.ui.plantList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplants.R

@Composable
fun PlantListScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()

    ) {
        Box {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = R.drawable.bg_plants),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Background plants"
            )

        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Image(
                modifier = Modifier
                    .width(360.dp)
                    .height(240.dp), painter = painterResource(id = R.drawable.plants_center),
                contentScale = ContentScale.Fit,
                contentDescription = "Cactus plants"
            )
            Spacer(modifier = Modifier.padding(top = 30.dp))
            Text(
                modifier = Modifier.padding(top = 10.dp),
                fontWeight = FontWeight.Medium,
                text = "Sorry.", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp,
                text = "There are no plants in the list, please add your first plant."
            )
        }
    }
}