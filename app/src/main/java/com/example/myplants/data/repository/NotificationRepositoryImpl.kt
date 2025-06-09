package com.example.myplants.data.repository

import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.data_source.NotificationDao
import com.example.myplants.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {

    override suspend fun insertNotification(
        plantId: Int,
        plantName: String,
        message: String,
        type: Int
    ) {
        val notification = NotificationEntity(
            plantId = plantId,
            plantName = plantName,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        dao.insert(notification)
    }

    override fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return dao.getAll()
    }

    override suspend fun countTodayNotifications(plantId: Int, since: Long): Int {
        return dao.countNotificationsToday(plantId, since)
    }
}