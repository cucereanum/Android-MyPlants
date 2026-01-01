package com.example.myplants.infrastructure.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WateringReminderScheduler {
    private const val uniqueWorkName: String = "WateringCheckWorker"

    fun schedule(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WateringCheckWorker>(
            15,
            TimeUnit.MINUTES,
        )
            .addTag(uniqueWorkName)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
    }
}
