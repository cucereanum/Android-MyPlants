package com.example.myplants.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.domain.repository.AnalyticsRepository
import com.example.myplants.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    /**
     * UI state that automatically updates when analytics data changes
     * Uses StateFlow with WhileSubscribed(5000) for efficient lifecycle handling
     */
    val uiState: StateFlow<AnalyticsUiState> = analyticsRepository
        .getAnalytics()
        .map { result ->
            when (result) {
                is Result.Success -> AnalyticsUiState(
                    analytics = result.data,
                    isLoading = false,
                    errorMessage = null
                )
                is Result.Error -> AnalyticsUiState(
                    analytics = null,
                    isLoading = false,
                    errorMessage = result.message ?: "Failed to load analytics"
                )
                is Result.Loading -> AnalyticsUiState(
                    analytics = null,
                    isLoading = true,
                    errorMessage = null
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState(isLoading = true)
        )

    /**
     * Retry loading analytics (called when user taps retry button)
     */
    fun retryLoading() {
        // Analytics automatically reload due to reactive Flow
        // This function exists for future error handling if needed
    }
}
