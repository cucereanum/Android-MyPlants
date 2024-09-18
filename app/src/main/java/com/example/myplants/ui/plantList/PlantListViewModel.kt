package com.example.myplants.ui.plantList

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repository: PlantRepository
) : ViewModel() {
    // List of filter settings
    var filterList by mutableStateOf(listOf("Upcoming", "Forgot to Water", "History"))
        private set

    // Selected filter index
    var selectedFilterType by mutableStateOf("Upcoming")
        private set

    // Backing property for state
    private val _items = MutableStateFlow<List<Plant>>(emptyList())

    // Expose the list of items as StateFlow (immutable)
    val items: StateFlow<List<Plant>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            getPlants()
        }
    }

    // Function to update the selected filter
    fun selectFilter(type: String) {
        selectedFilterType = type
    }

    private suspend fun getPlants() {

        try {
            repository.getPlants()
                .collect { itemsList ->
                    _items.value = itemsList
                }
        } catch (e: Exception) {
            e.message?.let { Log.e("Get Plant List Error", it) }
        }

    }
}