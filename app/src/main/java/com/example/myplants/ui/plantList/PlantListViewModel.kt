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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repository: PlantRepository
) : ViewModel() {

    var filterList by mutableStateOf(PlantListFilter.entries)
        private set

    var selectedFilterType by mutableStateOf(PlantListFilter.UPCOMING)
        private set

    private val _items = MutableStateFlow<List<Plant>>(emptyList())

    val items: StateFlow<List<Plant>> = _items.asStateFlow()

    var isLoading by mutableStateOf(false)
        private set

//    init {
//        viewModelScope.launch {
//            getPlants()
//        }
//    }

    fun selectFilter(type: PlantListFilter) {
        selectedFilterType = type
        filterPlants()
    }

    suspend fun getPlants() {
        isLoading = true
        try {
            val itemsList = repository.getPlants().first()
            _items.value = itemsList
            filterPlants()
        } catch (e: Exception) {
            e.message?.let { Log.e("Get Plant List Error", it) }
        } finally {
            isLoading = false
        }
    }

    private fun filterPlants() {
        viewModelScope.launch {
            val allPlants = repository.getPlants().first()
            val currentTime = System.currentTimeMillis()

            val filteredList = when (selectedFilterType) {
                PlantListFilter.UPCOMING -> allPlants.filter {
                    println("time, ${it.time}, currentTime, $currentTime")
                    !it.isWatered && it.time > currentTime
                }

                PlantListFilter.FORGOT_TO_WATER -> allPlants.filter {
                    !it.isWatered && it.time < currentTime
                }

                PlantListFilter.HISTORY -> allPlants.filter {
                    it.isWatered
                }

            }

            _items.value = filteredList
        }
    }
}