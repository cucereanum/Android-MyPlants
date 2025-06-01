package com.example.myplants.infrastructure.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myplants.R
import com.example.myplants.data.Plant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendWaterReminderNotification(plant: Plant) {
        val notificationManager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, "plant_channel")
            .setSmallIcon(R.drawable.drop)
            .setContentTitle("Water your plant!")
            .setContentText("${plant.plantName} needs watering")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(plant.id.hashCode(), notification)
    }
}