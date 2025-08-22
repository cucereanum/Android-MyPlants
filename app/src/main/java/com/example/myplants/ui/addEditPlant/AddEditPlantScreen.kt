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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myplants.R
import coil.compose.AsyncImage
import com.example.myplants.ui.addEditPlant.components.AppFormField
import com.example.myplants.ui.addEditPlant.components.DatesDialog
import com.example.myplants.ui.addEditPlant.components.PlantSizeDialog
import com.example.myplants.ui.addEditPlant.components.SelectTimeDialog
import com.example.myplants.ui.util.DebounceClick
import java.io.File


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlantScreen(
    navController: NavController,
    plantId: Int = -1,
    viewModel: AddEditPlantViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri !== null) {
                val newImagePath = saveImageToInternalStorage(context, uri)
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(
                        UpdateEvent.IMAGE_URI, newImagePath ?: ""
                    )
                )
            }
        })

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.updateState(
                UpdateEventWithValue.UpdateState(
                    UpdateEvent.SHOW_CAMERA_VIEW, true
                )
            )
        } else {
            // Handle the case where permission is denied
            Toast.makeText(
                context,
                context.getString(R.string.camera_permission_denied_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val errorMessages = viewModel.getErrorMessages()
    val scrollState = rememberScrollState()

    LaunchedEffect(errorMessages) {
        if (errorMessages.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(plantId) {
        if (plantId != -1) {
            viewModel.loadPlantForEditing(plantId)
        }
    }

    // Function to request camera permission
    fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity, Manifest.permission.CAMERA
            ) -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                // Request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    if (state.showTimeDialog) {
        SelectTimeDialog(modifier = Modifier.width(400.dp), updateTime = { hour, minute ->
            viewModel.updateState(UpdateEventWithValue.UpdateTime(hour, minute))
        }, onDismissRequest = {
            viewModel.updateState(
                UpdateEventWithValue.UpdateState(
                    UpdateEvent.SHOW_TIME_DIALOG, false
                )
            )
        })
    } else if (state.showDatesDialog) {
        DatesDialog(
            modifier = Modifier.width(400.dp),
            selectedDays = viewModel.state.selectedDays,
            onDismissRequest = {
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(
                        UpdateEvent.SHOW_DATES_DIALOG, false
                    )
                )
            }) {
            viewModel.toggleDaySelection(it)
        }
    } else if (state.showPlantSizeDialog) {
        PlantSizeDialog(
            modifier = Modifier.width(400.dp),
            selectedPlant = state.plantSize,
            togglePlantSizeSelection = {
                viewModel.updateState(UpdateEventWithValue.UpdateState(UpdateEvent.PLANT_SIZE, it))
            },
            onDismissRequest = {
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(
                        UpdateEvent.SHOW_PLANT_SIZE_DIALOG, false
                    )
                )
            })
    } else if (state.showDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(
                        UpdateEvent.SHOW_DIALOG, false
                    )
                )
            },
            title = { Text(stringResource(id = R.string.add_edit_plant_select_image_dialog_title)) },
            text = { // TODO: Check if this string is correct
                Column {
                    TextButton(onClick = {
                        requestCameraPermission()
                        viewModel.updateState(
                            UpdateEventWithValue.UpdateState(
                                UpdateEvent.SHOW_DIALOG, false
                            )
                        )
                    }) {
                        Text(stringResource(id = R.string.add_edit_plant_image_source_camera))
                    }
                    TextButton(onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        viewModel.updateState(
                            UpdateEventWithValue.UpdateState(
                                UpdateEvent.SHOW_DIALOG, false
                            )
                        )
                    }) {
                        Text(stringResource(id = R.string.add_edit_plant_image_source_gallery))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateState(
                        UpdateEventWithValue.UpdateState(
                            UpdateEvent.SHOW_DIALOG, false
                        )
                    )
                }) {
                    Text(
                        stringResource(id = R.string.dialog_cancel),
                        color = Color.White
                    )
                }
            })
    }
    if (state.showCameraView) {
        CameraView(
            onImageCaptured = { uri ->
                println("Image captured: $uri")
                // Save in VM and close the camera sheet/view
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(UpdateEvent.IMAGE_URI, uri.toString())
                )
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(UpdateEvent.SHOW_CAMERA_VIEW, false)
                )
            },
            onClose = {
                viewModel.updateState(
                    UpdateEventWithValue.UpdateState(UpdateEvent.SHOW_CAMERA_VIEW, false)
                )
            },
            initialLensFacing = state.lensFacing  // keep your existing front/back preference
        )
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
                    if (state.imageUri != null) {
                        AsyncImage(
                            model = state.imageUri,
                            contentDescription = stringResource(id = R.string.background_plants),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                    } else {
                        Image(
                            modifier = Modifier.fillMaxWidth(),
                            painter = painterResource(id = R.drawable.bg_plants),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = stringResource(id = R.string.background_plants)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 20.dp, y = 60.dp)
                            .align(Alignment.TopStart)
                            .background(Color.White, CircleShape)
                            .clickable {
                                DebounceClick.debounceClick {
                                    navController.popBackStack()
                                }
                            }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc),
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
                                .graphicsLayer(if (state.imageUri == null) 1.0f else 0.0f),
                            painter = painterResource(id = R.drawable.plant),
                            contentScale = ContentScale.Fit,
                            contentDescription = stringResource(id = R.string.add_edit_plant_single_plant_image_desc)

                        )


                        Spacer(modifier = Modifier.height(30.dp))
                        Button(shape = RoundedCornerShape(12.dp), onClick = {
                            viewModel.updateState(
                                UpdateEventWithValue.UpdateState(
                                    UpdateEvent.SHOW_DIALOG, true
                                )
                            )
                        }) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = stringResource(id = R.string.add_edit_plant_upload_image_icon_desc),
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp)) // Space between icon and text
                                Text(
                                    text = stringResource(id = R.string.add_edit_plant_add_image_button),
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
                    .verticalScroll(scrollState)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    AppFormField(
                        value = state.plantName,
                        onValueChange = {
                            viewModel.updateState(
                                UpdateEventWithValue.UpdateState(
                                    UpdateEvent.PLANT_NAME,
                                    it
                                )
                            )
                        },
                        label = stringResource(id = R.string.add_edit_plant_plant_name_label),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                        ) {
                            AppFormField(
                                value = viewModel.getSelectedDaysString(),
                                onValueChange = {}, // ignored for selector
                                label = stringResource(id = R.string.add_edit_plant_dates_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.updateState(
                                        UpdateEventWithValue.UpdateState(
                                            UpdateEvent.SHOW_DATES_DIALOG,
                                            true
                                        )
                                    )
                                }
                            )


                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                        ) {
                            AppFormField(
                                value = viewModel.displaySelectedTime(),
                                onValueChange = {},
                                label = stringResource(id = R.string.add_edit_plant_time_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.updateState(
                                        UpdateEventWithValue.UpdateState(
                                            UpdateEvent.SHOW_TIME_DIALOG,
                                            true
                                        )
                                    )
                                }
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
                                .weight(0.45f)
                        ) {
                            AppFormField(
                                value = state.waterAmount,
                                onValueChange = {
                                    viewModel.updateState(
                                        UpdateEventWithValue.UpdateState(
                                            UpdateEvent.WATER_AMOUNT,
                                            it
                                        )
                                    )
                                },
                                label = stringResource(id = R.string.add_edit_plant_water_amount_label),
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )

                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                        ) {
                            AppFormField(
                                value = state.plantSize.toString(),
                                onValueChange = {},
                                label = stringResource(id = R.string.add_edit_plant_plant_size_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.updateState(
                                        UpdateEventWithValue.UpdateState(
                                            UpdateEvent.SHOW_PLANT_SIZE_DIALOG,
                                            true
                                        )
                                    )
                                }
                            )

                        }

                    }
                    AppFormField(
                        value = state.description,
                        onValueChange = {
                            viewModel.updateState(
                                UpdateEventWithValue.UpdateState(
                                    UpdateEvent.DESCRIPTION,
                                    it
                                )
                            )
                        },
                        label = stringResource(id = R.string.add_edit_plant_description_label),
                        singleLine = false,
                        maxLines = 6   // or whatever you prefer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (errorMessages.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            errorMessages.forEach { errorMessage ->
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))


                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            DebounceClick.debounceClick {
                                if (viewModel.validate()) {
                                    viewModel.addPlant()
                                    navController.popBackStack()
                                }
                            }
                        }) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.dialog_save), // Changed from "Add a Plant"
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
