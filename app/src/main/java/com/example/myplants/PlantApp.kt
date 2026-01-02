package com.example.myplants

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.myplants.domain.repository.UserPreferencesRepository
import com.example.myplants.infrastructure.worker.WateringReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class PlantApp : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build(),
        )
        applyDefaultAppLanguageIfNotChosen()
        createNotificationChannel(this)
        applyNotificationSchedulingFromPreferences()
    }

    private fun applyNotificationSchedulingFromPreferences() {
        val areNotificationsEnabled = runBlocking {
            userPreferencesRepository.areNotificationsEnabledFlow.first()
        }

        if (areNotificationsEnabled) {
            WateringReminderScheduler.schedule(this)
        } else {
            WateringReminderScheduler.cancel(this)
        }
    }

    private fun applyDefaultAppLanguageIfNotChosen() {
        val currentAppLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentAppLocales.isEmpty) return

        val primaryDeviceLanguage =
            resources.configuration.locales[0].language.lowercase(Locale.ROOT)

        val languageToApply = when (primaryDeviceLanguage) {
            "de" -> "de"
            "ro" -> "ro"
            else -> "en"
        }

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageToApply)
        )
    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Plant Reminders"
            val descriptionText = "Notifications to remind you to water your plants"
            val importance = NotificationManagerCompat.IMPORTANCE_HIGH
            val channel = NotificationChannel("plant_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}