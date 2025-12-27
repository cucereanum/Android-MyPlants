package com.example.myplants.presentation.addEditPlant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.R
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.ImageStorageRepository
import com.example.myplants.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class AddEditPlantViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val imageStorageRepository: ImageStorageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditPlantState())
    val state: StateFlow<AddEditPlantState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AddEditPlantEffect>()
    val effect: SharedFlow<AddEditPlantEffect> = _effect.asSharedFlow()

    fun onAction(action: AddEditPlantAction) {
        when (action) {
            is AddEditPlantAction.OnPlantNameChanged -> {
                _state.update {
                    it.copy(
                        plantName = action.value,
                        plantNameError = null,
                        errorMessage = null,
                    )
                }
            }

            is AddEditPlantAction.OnWaterAmountChanged -> {
                _state.update {
                    it.copy(
                        waterAmount = action.value,
                        waterAmountError = null,
                        errorMessage = null,
                    )
                }
            }

            is AddEditPlantAction.OnDescriptionChanged -> {
                _state.update {
                    it.copy(
                        description = action.value,
                        descriptionError = null,
                        errorMessage = null,
                    )
                }
            }

            is AddEditPlantAction.OnPlantSizeSelected -> {
                _state.update {
                    it.copy(
                        plantSize = action.value,
                        showPlantSizeDialog = false,
                        errorMessage = null,
                    )
                }
            }

            is AddEditPlantAction.OnDayToggled -> {
                toggleDaySelection(action.day)
            }

            is AddEditPlantAction.OnTimeSelected -> {
                val now = LocalDateTime.now()
                _state.update {
                    it.copy(
                        time = now.withHour(action.hour).withMinute(action.minute),
                        showTimeDialog = false,
                        errorMessage = null,
                    )
                }
            }

            is AddEditPlantAction.OnLensFacingChanged -> {
                _state.update { it.copy(lensFacing = action.lensFacing) }
            }

            is AddEditPlantAction.OnImagePicked -> {
                persistImage(uriDescriptionForLogs = "picked", uri = action.uri)
            }

            is AddEditPlantAction.OnImageCaptured -> {
                persistImage(uriDescriptionForLogs = "captured", uri = action.uri)
                _state.update { it.copy(showCameraView = false) }
            }

            is AddEditPlantAction.SetShowImageSourceDialog -> {
                _state.update { it.copy(showDialog = action.show) }
            }

            is AddEditPlantAction.SetShowCameraView -> {
                _state.update { it.copy(showCameraView = action.show) }
            }

            is AddEditPlantAction.SetShowDatesDialog -> {
                _state.update { it.copy(showDatesDialog = action.show) }
            }

            is AddEditPlantAction.SetShowTimeDialog -> {
                _state.update { it.copy(showTimeDialog = action.show) }
            }

            is AddEditPlantAction.SetShowPlantSizeDialog -> {
                _state.update { it.copy(showPlantSizeDialog = action.show) }
            }

            AddEditPlantAction.OnSaveClicked -> {
                savePlant()
            }
        }
    }

    fun loadPlantForEditing(plantId: Int) {
        if (plantId <= 0) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val plant = plantRepository.getPlantById(plantId)
                if (plant == null) {
                    _state.update { it.copy(isLoading = false) }
                    _effect.emit(
                        AddEditPlantEffect.ShowMessage(
                            R.string.add_edit_plant_error_plant_not_found
                        )
                    )
                    return@launch
                }

                _state.update {
                    it.copy(
                        plantId = plant.id,
                        isWatered = plant.isWatered,
                        plantName = plant.plantName,
                        description = plant.description,
                        waterAmount = plant.waterAmount,
                        imageUri = plant.imageUri,
                        selectedDays = plant.selectedDays,
                        time = localDateTimeFromMillisOfDay(plant.time),
                        plantSize = runCatching {
                            com.example.myplants.data.PlantSizeType.valueOf(plant.size)
                        }.getOrElse { com.example.myplants.data.PlantSizeType.Medium },
                        isLoading = false,
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, errorMessage = t.message) }
                _effect.emit(
                    AddEditPlantEffect.ShowMessage(
                        R.string.add_edit_plant_error_failed_to_load_plant
                    )
                )
            }
        }
    }

    private fun toggleDaySelection(day: DayOfWeek?) {
        val allDays = DayOfWeek.entries.toList()
        val currentDays = _state.value.selectedDays.toMutableList()

        val updatedDays = when {
            day == null && currentDays.containsAll(allDays) -> emptyList()
            day == null -> allDays
            currentDays.contains(day) -> currentDays - day
            else -> currentDays + day
        }

        _state.update { it.copy(selectedDays = updatedDays, errorMessage = null) }
    }

    private fun persistImage(
        uriDescriptionForLogs: String,
        uri: android.net.Uri,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isPersistingImage = true,
                    imageUriError = null,
                    errorMessage = null,
                )
            }

            val result = imageStorageRepository.persistImage(uri)
            result
                .onSuccess { savedPath ->
                    _state.update {
                        it.copy(
                            imageUri = savedPath,
                            isPersistingImage = false,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isPersistingImage = false,
                            errorMessage = throwable.message,
                        )
                    }
                    _effect.emit(
                        AddEditPlantEffect.ShowMessage(
                            R.string.add_edit_plant_error_failed_to_persist_image
                        )
                    )
                }
        }
    }

    private fun savePlant() {
        if (_state.value.isSaving) return
        if (!validateAndUpdateErrors()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val currentState = _state.value
                val timeMillisOfDay = currentState.time.toLocalTime().toSecondOfDay() * 1000L

                val plant = Plant(
                    id = currentState.plantId ?: 0,
                    plantName = currentState.plantName.trim(),
                    description = currentState.description.trim(),
                    waterAmount = currentState.waterAmount.trim(),
                    size = currentState.plantSize.name,
                    imageUri = currentState.imageUri.orEmpty(),
                    time = timeMillisOfDay,
                    selectedDays = currentState.selectedDays,
                    isWatered = currentState.isWatered,
                )

                if (currentState.plantId == null) {
                    plantRepository.insertPlant(plant)
                } else {
                    plantRepository.updatePlant(plant)
                }

                _effect.emit(AddEditPlantEffect.NavigateBack)
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, errorMessage = t.message) }
                _effect.emit(
                    AddEditPlantEffect.ShowMessage(
                        R.string.add_edit_plant_error_failed_to_save_plant
                    )
                )
                return@launch
            }

            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun validateAndUpdateErrors(): Boolean {
        val currentState = _state.value

        val imageUriError =
            if (currentState.imageUri.isNullOrBlank()) R.string.add_edit_plant_validation_image_required else null

        val plantNameError =
            if (currentState.plantName.isBlank()) R.string.add_edit_plant_validation_plant_name_required else null

        val waterAmountError =
            if (currentState.waterAmount.isBlank()) R.string.add_edit_plant_validation_water_amount_required else null

        val descriptionError = when {
            currentState.description.isBlank() -> R.string.add_edit_plant_validation_description_required
            currentState.description.length > 150 -> R.string.add_edit_plant_validation_description_too_long
            else -> null
        }

        val isValid =
            imageUriError == null && plantNameError == null && waterAmountError == null && descriptionError == null

        _state.update {
            it.copy(
                imageUriError = imageUriError,
                plantNameError = plantNameError,
                waterAmountError = waterAmountError,
                descriptionError = descriptionError,
            )
        }

        return isValid
    }

    private fun localDateTimeFromMillisOfDay(millisOfDay: Long): LocalDateTime {
        val secondsOfDay = (millisOfDay / 1000L).coerceIn(0L, 24L * 60L * 60L - 1L)
        val time = LocalTime.ofSecondOfDay(secondsOfDay)
        return LocalDateTime.of(LocalDate.now(), time)
    }
}

sealed interface AddEditPlantEffect {
    data object NavigateBack : AddEditPlantEffect
    data class ShowMessage(@StringRes val messageResId: Int) : AddEditPlantEffect
}
