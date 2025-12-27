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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
        val startOfTodayMillis = LocalDate
            .now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val today = DayOfWeek.today()
        val yesterday = DayOfWeek.entries.let { days ->
            val index = days.indexOf(today)
            days[(index - 1 + days.size) % days.size]
        }
        val currentTimeOfDayMillis = LocalTime.now().toSecondOfDay() * 1000L
        val oneHourFromNowMillisOfDay = currentTimeOfDayMillis + 60 * 60 * 1000L

        val allNotifications = notificationRepository.getAllNotifications().first()
        val notificationsFromToday = allNotifications.filter { it.timestamp >= startOfTodayMillis }

        fun hasNotificationToday(plantId: Int, type: NotificationType): Boolean {
            return notificationsFromToday.any { it.plantId == plantId && it.type == type }
        }

        fun hasUpcomingNotificationToday(plantId: Int): Boolean {
            return hasNotificationToday(plantId = plantId, type = NotificationType.UPCOMING)
        }

        fun hasForgotNotificationToday(plantId: Int): Boolean {
            return hasNotificationToday(plantId = plantId, type = NotificationType.FORGOT)
        }

        val forgottenPlants = allPlants.filter { plant ->
            !plant.isWatered && (
                    (plant.selectedDays.contains(today) && plant.time < currentTimeOfDayMillis) ||
                            plant.selectedDays.contains(yesterday)
                    )
        }

        val upcomingPlantsWithinOneHour = allPlants.filter { plant ->
            !plant.isWatered &&
                    plant.selectedDays.contains(today) &&
                    plant.time in currentTimeOfDayMillis..oneHourFromNowMillisOfDay
        }

        val hasSentUpcomingNotificationToday = notificationsFromToday
            .any { it.type == NotificationType.UPCOMING }
        val hasSentForgotNotificationToday = notificationsFromToday
            .any { it.type == NotificationType.FORGOT }

        val upcomingPlantToNotify = upcomingPlantsWithinOneHour
            .filter { plant ->
                !hasUpcomingNotificationToday(plantId = plant.id) &&
                        !hasForgotNotificationToday(plantId = plant.id)
            }
            .minByOrNull { plant -> plant.time }

        val plantToNotify = when {
            !hasSentUpcomingNotificationToday && upcomingPlantToNotify != null -> {
                upcomingPlantToNotify to NotificationType.UPCOMING
            }

            !hasSentForgotNotificationToday -> {
                forgottenPlants
                    .filter { plant ->
                        !hasUpcomingNotificationToday(plantId = plant.id) &&
                                !hasForgotNotificationToday(plantId = plant.id)
                    }
                    .minByOrNull { plant -> plant.time }
                    ?.let { plant -> plant to NotificationType.FORGOT }
            }

            else -> null
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