package com.example.myplants.presentation.addEditPlant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.PlantSizeType
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
data class AddEditPlantState(
    val plantId: Int? = null,
    val isWatered: Boolean = false,
    val imageUri: String? = null,
    val showDialog: Boolean = false,
    val showCameraView: Boolean = false,
    val plantName: String = "",
    val time: LocalDateTime = LocalDateTime.now(),
    val waterAmount: String = "",
    val plantSize: PlantSizeType = PlantSizeType.Medium,
    val description: String = "",
    val showDatesDialog: Boolean = false,
    val showTimeDialog: Boolean = false,
    val showPlantSizeDialog: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val selectedDays: List<DayOfWeek> = listOf(DayOfWeek.today()),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isPersistingImage: Boolean = false,
    val errorMessage: String? = null,
    // Error states
    @StringRes val imageUriError: Int? = null,
    @StringRes val plantNameError: Int? = null,
    @StringRes val waterAmountError: Int? = null,
    @StringRes val descriptionError: Int? = null
)