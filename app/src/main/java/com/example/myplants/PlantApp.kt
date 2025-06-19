package com.example.myplants

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myplants.infrastructure.worker.WateringCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PlantApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        scheduleWateringCheckWorker()
    }

    private fun scheduleWateringCheckWorker() {
        val workRequest = PeriodicWorkRequestBuilder<WateringCheckWorker>(
            30, TimeUnit.MINUTES
        ).addTag("WateringCheckWorker").build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WateringCheckWorker", ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Plant Reminders"
            val descriptionText = "Notifications to remind you to water your plants"
            val importance = NotificationManagerCompat.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("plant_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: android.app.NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}