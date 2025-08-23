package com.example.myplants.presentation.addEditPlant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.PlantSizeType
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
data class AddEditPlantState(
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
    // Error states
    val imageUriError: String? = null,
    val plantNameError: String? = null,
    val waterAmountError: String? = null,
    val descriptionError: String? = null
)