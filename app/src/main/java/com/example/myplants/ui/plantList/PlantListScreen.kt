package com.example.myplants.ui.plantList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 90.dp,
                    )
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    text = "Let's Care \nMy Plants!",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                    lineHeight = 32.sp
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { /* Handle button click */ }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
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
                text = "Sorry.",
                color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp,
                text = "There are no plants in the list, please add your first plant."
            )
            Spacer(modifier = Modifier.padding(top = 20.dp))
            Button(
                modifier = Modifier
                    .width(320.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = { /*TODO*/ }) {
                Text(
                    text = "Add Your First Plant",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )

            }
        }
    }
}