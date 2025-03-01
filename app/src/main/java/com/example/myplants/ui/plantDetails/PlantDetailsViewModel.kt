package com.example.myplants.ui.plantDetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantDetailsViewModel @Inject constructor(
    private val repository: PlantRepository
) : ViewModel() {

    private val _plant = MutableStateFlow<Plant?>(null)
    val plant: StateFlow<Plant?> = _plant

    fun loadPlant(plantId: Int) {
        viewModelScope.launch {
            val result = repository.getPlantById(plantId)
            _plant.value = result
        }
    }

    fun deletePlant() {
        _plant.value?.let { plant ->
            viewModelScope.launch {
                repository.deletePlant(plant)
                _plant.value = null // Clear state after deletion
            }
        }
    }

    fun onMarkAsWatered() {
        _plant.value?.let { currentPlant ->
            val updatedPlant = currentPlant.copy(isWatered = true)

            viewModelScope.launch {
                repository.updatePlant(updatedPlant)
                _plant.value = updatedPlant // Update state after API call
            }
        }
    }

}