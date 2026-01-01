package com.example.myplants.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myplants.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepositoryImpl(
    appContext: Context,
) : UserPreferencesRepository {

    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
    )

    private val isDarkModeEnabledStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(
        sharedPreferences.getBoolean(Keys.isDarkModeEnabled, false),
    )
    private val areNotificationsEnabledStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(
        sharedPreferences.getBoolean(Keys.areNotificationsEnabled, true),
    )

    override val isDarkModeEnabledFlow: Flow<Boolean> = isDarkModeEnabledStateFlow.asStateFlow()
    override val areNotificationsEnabledFlow: Flow<Boolean> =
        areNotificationsEnabledStateFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Keys.isDarkModeEnabled -> {
                isDarkModeEnabledStateFlow.value =
                    prefs.getBoolean(Keys.isDarkModeEnabled, false)
            }

            Keys.areNotificationsEnabled -> {
                areNotificationsEnabledStateFlow.value =
                    prefs.getBoolean(Keys.areNotificationsEnabled, true)
            }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override suspend fun setDarkModeEnabled(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(Keys.isDarkModeEnabled, isEnabled)
        }
    }

    override suspend fun setNotificationsEnabled(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(Keys.areNotificationsEnabled, isEnabled)
        }
    }

    private companion object {
        private const val PREFERENCES_FILE_NAME: String = "user_preferences"

        private object Keys {
            const val isDarkModeEnabled: String = "is_dark_mode_enabled"
            const val areNotificationsEnabled: String = "are_notifications_enabled"
        }
    }
}
