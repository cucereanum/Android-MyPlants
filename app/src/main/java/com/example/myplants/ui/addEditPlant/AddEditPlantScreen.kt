package com.example.myplants.ui.addEditPlant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myplants.R
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import java.util.concurrent.ExecutorService


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlantScreen(
    navController: NavController,
    outputDirectory: File,
    cameraExecutor: ExecutorService,
    viewModel: AddEditPlantViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val photoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                val newImagePath = saveImageToInternalStorage(context, uri)
                viewModel.updateImageUri(newImagePath)
            }
        )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.updateCameraView(true)
        } else {
            // Handle the case where permission is denied
            Toast.makeText(
                context,
                "Please enable the camera permissions from the settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Function to request camera permission
    fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.CAMERA
            ) -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                // Request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    //todo: update UI accordingly + refactor code
    fun handleImageCapture(uri: Uri) {
        viewModel.updateCameraView(false)
        viewModel.updateImageUri(uri.toString())

    }

    if (viewModel.showTimeDialog) {
        SelectTimeDialog(
            modifier = Modifier.width(400.dp),
            updateTime = { hour, minute ->
                viewModel.updateTime(hour, minute)
            },
            onDismissRequest = {
                viewModel.updateShowTimeDialog(false)
            })
    } else if (viewModel.showDatesDialog) {
        DatesDialog(
            modifier = Modifier.width(400.dp),
            selectedDays = viewModel.selectedDays,
            onDismissRequest = {
                viewModel.updateShowDatesDialog(false)
            }) {
            viewModel.toggleDaySelection(it)
        }
    } else if (viewModel.showPlantSizeDialog) {
        PlantSizeDialog(
            modifier = Modifier.width(400.dp),
            selectedPlant = viewModel.plantSize,
            togglePlantSizeSelection = {
                viewModel.updatePlantSize(it)
            },
            onDismissRequest = {
                viewModel.updateShowPlantSizeDialog(false)
            })
    } else if (viewModel.showDialog) {

        AlertDialog(
            onDismissRequest = { viewModel.updateShowDialog(false) },
            title = { Text("Choose Image Source") },
            text = {
                Column {
                    TextButton(onClick = {
                        requestCameraPermission()
                        viewModel.updateShowDialog(false)
                    }) {
                        Text("Take Photo")
                    }
                    TextButton(onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
    if (viewModel.showCameraView) {
        CameraView(
            outputDirectory = outputDirectory,
            executor = cameraExecutor,
            lensFacing = viewModel.lensFacing,
            updateLensFacing = {
                viewModel.updateLensFacing(it)
            },
            removeCameraView = {
                viewModel.updateCameraView(false)
            },
            onImageCaptured = ::handleImageCapture
        ) {

        }
    } else {
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
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    CustomTextField(
                        value = viewModel.plantName,
                        onValueChange = {
                            viewModel.updatePlantName(it)
                        },
                        label = "Plant name*"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 15.dp)
                                .weight(0.45f)
                        ) {
                            Text(
                                text = "Dates",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium

                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .height(60.dp)
                                    .clip(
                                        RoundedCornerShape(14.dp)
                                    ),
                                singleLine = true,
                                maxLines = 1,
                                readOnly = true,
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect {
                                                if (it is PressInteraction.Release) {
                                                    viewModel.updateShowDatesDialog(true)
                                                }
                                            }
                                        }
                                    },
                                trailingIcon = {
                                    IconButton(onClick = {
                                    }) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Dropdown Icon"
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                value = viewModel.getSelectedDaysString(),
                                onValueChange = {}
                            )


                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .padding(top = 15.dp)
                                .weight(0.45f)
                        ) {
                            Text(
                                text = "Time",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .height(60.dp)
                                    .clip(
                                        RoundedCornerShape(14.dp)
                                    ),
                                singleLine = true,
                                maxLines = 1,
                                readOnly = true,
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect {
                                                if (it is PressInteraction.Release) {
                                                    viewModel.updateShowTimeDialog(true)
                                                }
                                            }
                                        }
                                    },
                                trailingIcon = {
                                    IconButton(onClick = {
                                    }) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Dropdown Icon"
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                value = viewModel.displaySelectedTime(),
                                onValueChange = {}
                            )


                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 15.dp)
                                .weight(0.45f)
                        ) {
                            Text(
                                text = "The amount of water*",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium

                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .height(60.dp)
                                    .clip(
                                        RoundedCornerShape(14.dp)
                                    ),
                                singleLine = true,
                                maxLines = 1,
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                value = viewModel.waterAmount,
                                onValueChange = {
                                    viewModel.updateWaterAmount(it)
                                }
                            )


                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .padding(top = 15.dp)
                                .weight(0.45f)
                        ) {
                            Text(
                                text = "Plant Size*",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .height(60.dp)
                                    .clip(
                                        RoundedCornerShape(14.dp)
                                    ),
                                singleLine = true,
                                maxLines = 1,
                                readOnly = true,
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect {
                                                if (it is PressInteraction.Release) {
                                                    viewModel.updateShowPlantSizeDialog(true)
                                                }
                                            }
                                        }
                                    },
                                trailingIcon = {
                                    IconButton(onClick = {
                                    }) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Dropdown Icon"
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                value = viewModel.plantSize.toString(),
                                onValueChange = {}
                            )


                        }

                    }
                    CustomTextField(
                        value = viewModel.description,
                        onValueChange = {
                            viewModel.updateDescription(it)
                        },
                        label = "Description",
                        multiline = true
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            viewModel.addPlant()
                            navController.popBackStack()
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add a Plant",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }


                    }
                }

            }
        }
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri?): String? {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri!!) ?: return null
    val file = File(context.filesDir, "saved_plant_image_${System.currentTimeMillis()}.jpg")

    file.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }
    return file.absolutePath // Return the path of the saved file
}