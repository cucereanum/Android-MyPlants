package com.example.myplants.presentation.plantList

import androidx.compose.runtime.Immutable
import com.example.myplants.data.Plant

@Immutable
data class PlantListUiState(
    val plants: List<Plant> = emptyList(),
    val selectedFilterType: PlantListFilter = PlantListFilter.UPCOMING,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreToLoad: Boolean = true,
    val errorMessage: String? = null,
)
