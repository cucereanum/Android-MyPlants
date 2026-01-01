package com.example.myplants.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isDarkModeEnabledFlow: Flow<Boolean>
    val areNotificationsEnabledFlow: Flow<Boolean>

    suspend fun setDarkModeEnabled(isEnabled: Boolean)
    suspend fun setNotificationsEnabled(isEnabled: Boolean)
}
