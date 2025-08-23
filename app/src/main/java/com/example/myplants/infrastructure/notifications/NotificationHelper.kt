package com.example.myplants.infrastructure.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myplants.MainActivity
import com.example.myplants.R
import com.example.myplants.data.NotificationType
import com.example.myplants.data.Plant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendWaterReminderNotification(plant: Plant, type: NotificationType) {

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_PLANT"
            putExtra("plantId", plant.id)
            putExtra("fromNotification", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }


        val pendingIntent = PendingIntent.getActivity(
            context,
            plant.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (type == NotificationType.FORGOT) {
            "Water your plant!"
        } else {
            "Don't forget to water your plant today!"
        }
        val contentText = if (type == NotificationType.FORGOT) {
            "${plant.plantName} needs watering"
        } else {
            "${plant.plantName} will need watering today"
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, "plant_channel")
            .setSmallIcon(R.drawable.drop)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent) // 👈 required for action
            .setAutoCancel(true) // closes notification on tap
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationManager.notify(plant.id.hashCode(), notification)
    }
}