package com.example.myplants.domain.repository

import com.example.myplants.data.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun insertNotification(plantId: Int, plantName: String, message: String, type: Int)
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    suspend fun countTodayNotifications(plantId: Int, since: Long): Int
}