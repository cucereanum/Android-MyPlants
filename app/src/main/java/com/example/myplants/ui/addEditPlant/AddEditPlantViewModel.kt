package com.example.myplants.ui.addEditPlant


import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant
import com.example.myplants.data.PlantSizeType
import com.example.myplants.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class AddEditPlantViewModel @Inject constructor(
    private val repository: PlantRepository
) : ViewModel() {
    //todo: 2. add loading state for most of the functionalities!
    // todo 3. events to check if the plant was created; Toasts! Error Messages!
    // todo 4. Blur View on bottom of the screen; Background Blur and not dark for dialogs;
    // todo 5: refactor code!

    var state by mutableStateOf(AddEditPlantState())
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

    fun addPlant() {
        if (validate()) {
            viewModelScope.launch {
                repository.insertPlant(
                    Plant(
                        plantName = state.plantName,
                        description = state.description,
                        waterAmount = state.waterAmount,
                        imageUri = state.imageUri ?: "",
                        time = state.time.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        selectedDays = DayOfWeek.valueOf("Friday")
                    )
                )
            }
        }
    }

    fun getSelectedDaysString(): String {
        return selectedDays.joinToString(", ") { it.dayName }
    }

    fun updateState(event: UpdateEventWithValue) {
        when (event) {
            is UpdateEventWithValue.UpdateTime -> {
                val now = LocalDateTime.now()
                state = state.copy(
                    time = now.withHour(event.hour).withMinute(event.minute),
                    showTimeDialog = false
                )
            }

            is UpdateEventWithValue.UpdateState -> {
                state = when (event.type) {
                    UpdateEvent.IMAGE_URI -> state.copy(imageUri = event.value as String?)
                    UpdateEvent.SHOW_DIALOG -> state.copy(showDialog = event.value as Boolean)
                    UpdateEvent.SHOW_CAMERA_VIEW -> state.copy(showCameraView = event.value as Boolean)
                    UpdateEvent.PLANT_NAME -> state.copy(plantName = event.value as String)
                    UpdateEvent.WATER_AMOUNT -> state.copy(waterAmount = event.value as String)
                    UpdateEvent.DESCRIPTION -> state.copy(description = event.value as String)
                    UpdateEvent.SHOW_DATES_DIALOG -> state.copy(showDatesDialog = event.value as Boolean)
                    UpdateEvent.SHOW_TIME_DIALOG -> state.copy(showTimeDialog = event.value as Boolean)
                    UpdateEvent.SHOW_PLANT_SIZE_DIALOG -> state.copy(showPlantSizeDialog = event.value as Boolean)
                    UpdateEvent.LENS_FACING -> state.copy(lensFacing = event.value as Int)
                    UpdateEvent.TIME -> TODO() // Do nothing here, handled above
                    UpdateEvent.PLANT_SIZE -> state.copy(plantSize = PlantSizeType.valueOf(event.value.toString()))
                }
            }
        }
    }


    fun displaySelectedTime(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return state.time.format(formatter)
    }

    fun validate(): Boolean {
        var isValid = true

        if (state.imageUri.isNullOrEmpty()) {
            state = state.copy(imageUriError = "Image is required.")
            isValid = false
        } else {
            state = state.copy(imageUriError = null)
        }

        if (state.plantName.isBlank()) {
            state = state.copy(plantNameError = "Plant name cannot be empty.")
            isValid = false
        } else {
            state = state.copy(plantNameError = null)
        }

        if (state.waterAmount.isBlank()) {
            state = state.copy(waterAmountError = "Water amount cannot be empty.")
            isValid = false
        } else {
            state = state.copy(waterAmountError = null)
        }

        if (state.description.isBlank()) {
            state = state.copy(descriptionError = "Description cannot be empty.")
            isValid = false
        } else if (state.description.length > 150) {
            state = state.copy(descriptionError = "Description cannot exceed 150 characters.")
            isValid = false
        } else {
            state = state.copy(descriptionError = null)
        }

        return isValid
    }

    private fun getCurrentDayOfWeek(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault()) // "EEEE" for full day name
        return sdf.format(Calendar.getInstance().time)
    }

    fun getErrorMessages(): List<String> {
        return listOfNotNull(
            state.imageUriError,
            state.plantNameError,
            state.waterAmountError,
            state.descriptionError
        )
    }
}