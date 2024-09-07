package com.example.myplants.ui.addEditPlant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myplants.R
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage


@Composable
fun AddEditPlantScreen(
    navController: NavController,
    viewModel: AddEditPlantViewModel = viewModel()
) {

    val photoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                viewModel.updateImageUri(uri)
            }
        )

    if (viewModel.showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.updateShowDialog(false) },
            title = { Text("Choose Image Source") },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.updateShowDialog(false)
                    }) {
                        Text("Take Photo")
                    }
                    TextButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        viewModel.updateShowDialog(false)
                    }) {
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateShowDialog(false) }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

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
                if (viewModel.imageUri != null) {
                    AsyncImage(
                        model = viewModel.imageUri,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "Background plants"
                    )
                } else {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(id = R.drawable.bg_plants),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "Background plants"
                    )
                }
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
                        contentDescription = "Go Back",
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
                            .height(180.dp)
                            .graphicsLayer(if (viewModel.imageUri == null) 1.0f else 0.0f),
                        painter = painterResource(id = R.drawable.plant),
                        contentScale = ContentScale.Fit,
                        contentDescription = "Single plant"

                    )


                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            viewModel.updateShowDialog(true)
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Image",
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