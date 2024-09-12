package com.example.myplants.ui.addEditPlant

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEditPlantViewModel : ViewModel() {

    var imageUri by mutableStateOf<Uri?>(null)
        private set

    var showDialog by mutableStateOf<Boolean>(false)
        private set

    var showCameraView by mutableStateOf<Boolean>(false)
        private set

    var plantName by mutableStateOf<String>("")
        private set


    var time by mutableStateOf<Date>(Date())
        private set

    var waterAmount by mutableStateOf<String>("")
        private set

    var plantSize by mutableStateOf<String>("")
        private set

    var description by mutableStateOf<String>("")
        private set

    var showDatesDialog by mutableStateOf<Boolean>(false)
        private set

    var lensFacing by mutableStateOf<Int>(CameraSelector.LENS_FACING_BACK)
        private set


    var selectedDays = mutableStateListOf<DayOfWeek>().apply {
        val currentDay = getCurrentDayOfWeek()
        add(DayOfWeek.valueOf(currentDay))
    }

    fun toggleDaySelection(selectedDay: String) {
        when (selectedDay) {
            "EveryDay" -> {
                if (selectedDays.isEmpty() || !selectedDays.containsAll(DayOfWeek.allDays())) {
                    selectedDays.clear()
                    selectedDays.addAll(DayOfWeek.allDays())
                } else {
                    selectedDays.clear()
                }
            }

            else -> {
                val day = DayOfWeek.fromDisplayName(selectedDay)
                day?.let {
                    if (selectedDays.contains(it)) {
                        selectedDays.remove(it)

                        if (selectedDays.size < DayOfWeek.allDays().size) {
                            selectedDays.removeIf { it != it }
                        }
                    } else {
                        selectedDays.add(it)
                        if (selectedDays.size == DayOfWeek.allDays().size) {
                            selectedDays.remove(DayOfWeek.fromDisplayName("EveryDay"))
                        }
                    }
                }
            }
        }
    }

    // Convert the selected days to a comma-separated string
    fun getSelectedDaysString(): String {
        return selectedDays.joinToString(", ") { it.dayName }
    }

    fun updateShowDatesDialog(value: Boolean) {
        showDatesDialog = value
    }

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

    // Create a function to get the current day as a string
    fun getCurrentDayOfWeek(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault()) // "EEEE" for full day name
        return sdf.format(Calendar.getInstance().time)
    }

    fun updateLensFacing(value: Int) {
        lensFacing = value
    }

}