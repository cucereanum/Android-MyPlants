package com.example.myplants.domain.usecase

import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.infrastructure.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckForWateringUseCase @Inject constructor(
    private val repository: PlantRepository, private val notificationHelper: NotificationHelper
) {

    private var previousPlants: List<Plant> = emptyList()

    suspend fun execute() {
        val allPlants = repository.getPlants().first()
        val currentTime = System.currentTimeMillis()

        val transitionedPlants = allPlants.filter { plant ->
            val wasUpcoming = previousPlants.find { it.id == plant.id }?.let {
                !it.isWatered && it.time > currentTime
            } ?: false

            val isNowForgot = !plant.isWatered && plant.time < currentTime

            wasUpcoming && isNowForgot
        }

        transitionedPlants.forEach {
            notificationHelper.sendWaterReminderNotification(it)
        }

        previousPlants = allPlants
    }
}