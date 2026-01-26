package com.example.myplants.presentation.addEditPlant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myplants.R
import coil.compose.AsyncImage
import com.example.myplants.presentation.addEditPlant.components.AppFormField
import com.example.myplants.presentation.addEditPlant.components.DatesDialog
import com.example.myplants.presentation.addEditPlant.components.PlantSizeDialog
import com.example.myplants.presentation.addEditPlant.components.SectionHeader
import com.example.myplants.presentation.addEditPlant.components.SelectTimeDialog
import com.example.myplants.presentation.components.CircularIconButton
import com.example.myplants.presentation.components.PrimaryButton
import com.example.myplants.presentation.util.DebounceClick

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPlantScreen(
    navController: NavController,
    plantId: Int = -1,
    viewModel: AddEditPlantViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AddEditPlantEffect.NavigateBack -> navController.popBackStack()
                is AddEditPlantEffect.ShowMessage -> {
                    Toast.makeText(
                        context,
                        context.getString(effect.messageResId),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
            if (uri != null) {
                viewModel.onAction(AddEditPlantAction.OnImagePicked(uri))
            }
        })

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.onAction(AddEditPlantAction.SetShowCameraView(true))
        } else {
            // Handle the case where permission is denied
            Toast.makeText(
                context,
                context.getString(R.string.camera_permission_denied_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val errorMessages = listOfNotNull(
        state.imageUriError,
        state.plantNameError,
        state.waterAmountError,
        state.descriptionError,
    )
    val scrollState = rememberScrollState()

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
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    if (state.showTimeDialog) {
        SelectTimeDialog(modifier = Modifier.width(400.dp), updateTime = { hour, minute ->
            viewModel.onAction(AddEditPlantAction.OnTimeSelected(hour, minute))
        }, onDismissRequest = {
            viewModel.onAction(AddEditPlantAction.SetShowTimeDialog(false))
        })
    } else if (state.showDatesDialog) {
        DatesDialog(
            modifier = Modifier.width(400.dp),
            selectedDays = state.selectedDays,
            onDismissRequest = {
                viewModel.onAction(AddEditPlantAction.SetShowDatesDialog(false))
            }) {
            viewModel.onAction(AddEditPlantAction.OnDayToggled(it))
        }
    } else if (state.showPlantSizeDialog) {
        PlantSizeDialog(
            modifier = Modifier.width(400.dp),
            selectedPlant = state.plantSize,
            togglePlantSizeSelection = {
                viewModel.onAction(AddEditPlantAction.OnPlantSizeSelected(it))
            },
            onDismissRequest = {
                viewModel.onAction(AddEditPlantAction.SetShowPlantSizeDialog(false))
            })
    } else if (state.showDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(AddEditPlantAction.SetShowImageSourceDialog(false))
            },
            title = { Text(stringResource(id = R.string.add_edit_plant_select_image_dialog_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        requestCameraPermission()
                        viewModel.onAction(AddEditPlantAction.SetShowImageSourceDialog(false))
                    }) {
                        Text(stringResource(id = R.string.add_edit_plant_image_source_camera))
                    }
                    TextButton(onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        viewModel.onAction(AddEditPlantAction.SetShowImageSourceDialog(false))
                    }) {
                        Text(stringResource(id = R.string.add_edit_plant_image_source_gallery))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onAction(AddEditPlantAction.SetShowImageSourceDialog(false))
                }) {
                    Text(
                        stringResource(id = R.string.dialog_cancel), color = Color.White
                    )
                }
            })
    }
    if (state.showCameraView) {
        CameraView(
            onImageCaptured = { uri ->
                viewModel.onAction(AddEditPlantAction.OnImageCaptured(uri))
            },
            onError = { throwable ->
                Toast.makeText(
                    context,
                    throwable.message ?: context.getString(R.string.generic_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onClose = {
                viewModel.onAction(AddEditPlantAction.SetShowCameraView(false))
            }, initialLensFacing = state.lensFacing  // keep your existing front/back preference
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val upEvent = waitForUpOrCancellation()
                        if (upEvent != null) {
                            focusManager.clearFocus()
                        }
                    }
                }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(MaterialTheme.colorScheme.background)
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
                    CircularIconButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc),
                        onClick = {
                            DebounceClick.debounceClick {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .offset(x = 20.dp, y = 60.dp)
                            .align(Alignment.TopStart),
                        iconSize = 30.dp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),

                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Image(
                            modifier = Modifier
                                .width(100.dp)
                                .height(180.dp)
                                .graphicsLayer(if (state.imageUri == null) 1.0f else 0.0f),
                            painter = painterResource(id = R.drawable.plant),
                            contentScale = ContentScale.Fit,
                            contentDescription = stringResource(id = R.string.add_edit_plant_single_plant_image_desc)

                        )


                        Spacer(modifier = Modifier.height(20.dp))
                        PrimaryButton(
                            text = stringResource(
                                id = if (state.imageUri != null)
                                    R.string.add_edit_plant_change_image_button
                                else
                                    R.string.add_edit_plant_add_image_button
                            ),
                            onClick = {
                                viewModel.onAction(AddEditPlantAction.SetShowImageSourceDialog(true))
                            },
                            icon = if (state.imageUri != null) Icons.Outlined.CameraAlt else Icons.Default.CloudUpload,
                            fillMaxWidth = false
                        )
                    }
                }
            }
            // Track keyboard visibility
            val isKeyboardOpen = WindowInsets.isImeVisible

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .padding(bottom = if (isKeyboardOpen) 0.dp else 80.dp) // Space for sticky button when visible
                ) {
                    // Section 1: Basic Information
                    SectionHeader(
                        icon = Icons.Outlined.Edit,
                        title = stringResource(id = R.string.add_edit_plant_section_basic_info)
                    )

                    AppFormField(
                        value = state.plantName,
                        onValueChange = {
                            viewModel.onAction(AddEditPlantAction.OnPlantNameChanged(it))
                        },
                        label = stringResource(id = R.string.add_edit_plant_plant_name_label),
                        singleLine = true,
                        placeholder = "e.g. Monstera Deliciosa"
                    )

                    AppFormField(
                        value = state.description,
                        onValueChange = {
                            viewModel.onAction(AddEditPlantAction.OnDescriptionChanged(it))
                        },
                        label = stringResource(id = R.string.add_edit_plant_description_label),
                        singleLine = false,
                        maxLines = 6,
                        placeholder = stringResource(id = R.string.add_edit_plant_description_placeholder),
                        minLines = 3
                    )

                    // Section 2: Watering Schedule
                    SectionHeader(
                        icon = Icons.Filled.WaterDrop,
                        title = stringResource(id = R.string.add_edit_plant_section_watering)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppFormField(
                                value = state.selectedDays.joinToString(", ") { it.dayName },
                                onValueChange = {},
                                label = stringResource(id = R.string.add_edit_plant_frequency_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.onAction(AddEditPlantAction.SetShowDatesDialog(true))
                                },
                                showDropdownIcon = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AppFormField(
                                value = state.time.toLocalTime().toString().take(5),
                                onValueChange = {},
                                label = stringResource(id = R.string.add_edit_plant_reminder_time_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.onAction(AddEditPlantAction.SetShowTimeDialog(true))
                                },
                                showClockIcon = true
                            )
                        }
                    }

                    // Section 3: Care Specifications
                    SectionHeader(
                        icon = Icons.Outlined.Eco,
                        title = stringResource(id = R.string.add_edit_plant_section_care)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppFormField(
                                value = state.waterAmount,
                                onValueChange = {
                                    viewModel.onAction(AddEditPlantAction.OnWaterAmountChanged(it))
                                },
                                label = stringResource(id = R.string.add_edit_plant_water_amount_label),
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                placeholder = "e.g. 200",
                                suffix = "ml"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AppFormField(
                                value = state.plantSize.plantSize,
                                onValueChange = {},
                                label = stringResource(id = R.string.add_edit_plant_plant_size_label),
                                readOnly = true,
                                onClick = {
                                    viewModel.onAction(
                                        AddEditPlantAction.SetShowPlantSizeDialog(
                                            true
                                        )
                                    )
                                },
                                showDropdownIcon = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessages.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            errorMessages.forEach { errorMessage ->
                                Text(
                                    text = stringResource(id = errorMessage),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Sticky bottom button - hidden when keyboard is open
                if (!isKeyboardOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        PrimaryButton(
                            text = stringResource(
                                id = if (state.plantId != null)
                                    R.string.add_edit_plant_save_plant_button
                                else
                                    R.string.add_edit_plant_create_button
                            ),
                            onClick = {
                                DebounceClick.debounceClick {
                                    viewModel.onAction(AddEditPlantAction.OnSaveClicked)
                                }
                            },
                            icon = if (state.plantId != null) Icons.Default.Check else null
                        )
                    }
                }
            }
        }
    }
}
