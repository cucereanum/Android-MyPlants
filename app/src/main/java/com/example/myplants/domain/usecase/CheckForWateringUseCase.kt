package com.example.myplants.domain.usecase

import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.infrastructure.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject

class CheckForWateringUseCase @Inject constructor(
    private val repository: PlantRepository, private val notificationHelper: NotificationHelper
) {

    private var previousPlants: List<Plant> = emptyList()

    suspend fun execute() {
        val allPlants = repository.getPlants().first()
        val today = DayOfWeek.today()
        val yesterday = DayOfWeek.entries.let { days ->
            val index = days.indexOf(today)
            days[(index - 1 + days.size) % days.size]
        }
        val currentTimeOfDayMillis = LocalTime.now().toSecondOfDay() * 1000L

        val forgottenPlants = allPlants.filter { plant ->
            !plant.isWatered && ((plant.selectedDays.contains(today) && plant.time < currentTimeOfDayMillis)
                    || (plant.selectedDays.contains(yesterday)))
        }

        forgottenPlants.forEach {
            notificationHelper.sendWaterReminderNotification(it)
        }

        previousPlants = allPlants
    }
}