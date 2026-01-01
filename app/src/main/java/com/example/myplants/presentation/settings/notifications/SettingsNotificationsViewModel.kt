package com.example.myplants.presentation.settings.notifications

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
class SettingsNotificationsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val areNotificationsEnabled: StateFlow<Boolean> =
        userPreferencesRepository.areNotificationsEnabledFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true,
            )

    fun setNotificationsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(isEnabled)
        }
    }
}
