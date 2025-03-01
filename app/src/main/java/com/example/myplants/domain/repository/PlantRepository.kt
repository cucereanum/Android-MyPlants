package com.example.myplants.domain.repository


import com.example.myplants.data.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {

    fun getPlants(): Flow<List<Plant>>

    suspend fun getPlantById(id: Int): Plant?

    suspend fun insertPlant(plant: Plant)

    suspend fun deletePlant(plant: Plant)

    suspend fun updatePlant(plant: Plant)
}