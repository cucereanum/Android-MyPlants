package com.example.myplants.ui.addEditPlant

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myplants.R
import com.example.myplants.navigation.Route


@Composable
fun AddEditPlantScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .background(MaterialTheme.colorScheme.onBackground)
        ) {
            Box {

                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(id = R.drawable.bg_plants),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = "Background plants"
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 20.dp, y = 60.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White, CircleShape)
                        .clickable {
                            navController.popBackStack()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Cloud Upload",
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center),
                        tint = Color.Black
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(90.dp))
                    Image(
                        modifier = Modifier
                            .width(100.dp)
                            .height(180.dp),
                        painter = painterResource(id = R.drawable.plant),
                        contentScale = ContentScale.Fit,
                        contentDescription = "Single plant"
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            navController.navigate(Route.ADD_EDIT_PLANT)
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Cloud Upload",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp)) // Space between icon and text
                            Text(
                                text = "Add Image",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }


                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {

        }
    }
}