package com.example.myplants.presentation.analytics

import com.example.myplants.domain.model.PlantAnalytics

/**
 * UI state for Analytics screen
 */
data class AnalyticsUiState(
    val analytics: PlantAnalytics? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
