package com.example.myplants.presentation.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsAppearanceViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val isDarkModeEnabled: StateFlow<Boolean> =
        userPreferencesRepository.isDarkModeEnabledFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    fun setDarkModeEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkModeEnabled(isEnabled)
        }
    }
}
