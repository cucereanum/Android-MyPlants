package com.example.myplants.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.NotificationType
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.NotificationRepository
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.infrastructure.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class CheckForWateringUseCase @Inject constructor(
    private val repository: PlantRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper
) {

    private var previousPlants: List<Plant> = emptyList()


    suspend fun execute() {
        val allPlants = repository.getPlants().first()
        val now = System.currentTimeMillis()
        val today = DayOfWeek.today()
        val yesterday = DayOfWeek.entries.let { days ->
            val index = days.indexOf(today)
            days[(index - 1 + days.size) % days.size]
        }
        val currentTimeOfDayMillis = LocalTime.now().toSecondOfDay() * 1000L
        val upcomingThreshold = currentTimeOfDayMillis + 12 * 60 * 60 * 1000L
        val twentyHoursAgo = now - 20 * 60 * 60 * 1000L

        val allNotifications = notificationRepository.getAllNotifications().first()
        val forgottenPlants = allPlants.filter { plant ->
            !plant.isWatered && (
                    (plant.selectedDays.contains(today) && plant.time < currentTimeOfDayMillis) ||
                            plant.selectedDays.contains(yesterday)
                    )
        }

        val upcomingPlants = allPlants.filter { plant ->
            !plant.isWatered &&
                    plant.selectedDays.contains(today) &&
                    plant.time in currentTimeOfDayMillis..upcomingThreshold
        }

        val notifications = buildMap {
            forgottenPlants.forEach { put(it.id, it to NotificationType.FORGOT) }
            upcomingPlants.forEach { putIfAbsent(it.id, it to NotificationType.UPCOMING) }
        }.values.toList()
        println("allNotifications: plants: upcoming - ${upcomingPlants.size}, forgottenPlants - ${forgottenPlants.size}, total - ${notifications.size}")
        val plantToNotify = notifications
            .sortedBy { it.second == NotificationType.UPCOMING } // FORGOT will come first
            .firstOrNull { (plant, type) ->
                allNotifications.none {
                    it.plantId == plant.id && it.type == type && it.timestamp > twentyHoursAgo
                }
            }
        plantToNotify?.let { (plant, type) ->
            val message = when (type) {
                NotificationType.UPCOMING -> "${plant.plantName} needs watering soon!"
                NotificationType.FORGOT -> "${plant.plantName} was not watered!"
            }

            notificationRepository.insertNotification(
                plantId = plant.id,
                plantName = plant.plantName,
                message = message,
                type = type
            )

            notificationHelper.sendWaterReminderNotification(plant, type)
        }

        previousPlants = allPlants
    }
}