package com.example.myplants.ui.addEditPlant

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class AddEditPlantViewModel : ViewModel() {

    var imageUri by mutableStateOf<Uri?>(null)
        private set

    var showDialog by mutableStateOf<Boolean>(false)
        private set

    var showCameraView by mutableStateOf<Boolean>(false)
        private set

    var plantName by mutableStateOf<String>("")
        private set

    var dates by mutableStateOf<List<String>>(listOf(""))
        private set

    var time by mutableStateOf<Date>(Date())
        private set

    var waterAmount by mutableStateOf<String>("")
        private set

    var plantSize by mutableStateOf<String>("")
        private set

    var description by mutableStateOf<String>("")
        private set


    fun updateCameraView(value: Boolean) {
        showCameraView = value
    }

    fun updateImageUri(uri: Uri?) {
        imageUri = uri;
    }

    fun updateShowDialog(value: Boolean) {
        showDialog = value
    }

    fun updatePlantName(value: String) {
        plantName = value
    }

    fun updateDates(value: List<String>) {
        dates = value
    }

    fun updateTime(value: Date) {
        time = value
    }

    fun updateWaterAmount(value: String) {
        waterAmount = value
    }

    fun updatePlantSize(value: String) {
        plantSize = value
    }

    fun updateDescription(value: String) {
        description = value
    }


}