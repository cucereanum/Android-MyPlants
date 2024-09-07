package com.example.myplants.ui.plantList

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class PlantListViewModel : ViewModel() {
    // List of filter settings
    var filterList by mutableStateOf(listOf("Upcoming", "Forgot to Water", "History"))
        private set

    // Selected filter index
    var selectedFilterType by mutableStateOf("Upcoming")
        private set

    // Function to update the selected filter
    fun selectFilter(type: String) {
        selectedFilterType = type
    }
}