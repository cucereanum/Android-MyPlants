package com.example.myplants.presentation.addEditPlant

import android.net.Uri
import androidx.camera.core.CameraSelector
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.PlantSizeType

sealed interface AddEditPlantAction {
    data class OnPlantNameChanged(val value: String) : AddEditPlantAction
    data class OnWaterAmountChanged(val value: String) : AddEditPlantAction
    data class OnDescriptionChanged(val value: String) : AddEditPlantAction

    data class OnPlantSizeSelected(val value: PlantSizeType) : AddEditPlantAction
    data class OnDayToggled(val day: DayOfWeek?) : AddEditPlantAction
    data class OnTimeSelected(val hour: Int, val minute: Int) : AddEditPlantAction

    data class OnLensFacingChanged(@CameraSelector.LensFacing val lensFacing: Int) :
        AddEditPlantAction

    data class OnImagePicked(val uri: Uri) : AddEditPlantAction
    data class OnImageCaptured(val uri: Uri) : AddEditPlantAction

    data class SetShowImageSourceDialog(val show: Boolean) : AddEditPlantAction
    data class SetShowCameraView(val show: Boolean) : AddEditPlantAction
    data class SetShowDatesDialog(val show: Boolean) : AddEditPlantAction
    data class SetShowTimeDialog(val show: Boolean) : AddEditPlantAction
    data class SetShowPlantSizeDialog(val show: Boolean) : AddEditPlantAction

    data object OnSaveClicked : AddEditPlantAction
}