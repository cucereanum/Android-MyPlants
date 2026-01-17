package com.example.myplants.domain.repository

import com.example.myplants.data.Plant
import com.example.myplants.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface PlantRepository {

    fun getPlants(): Flow<Result<List<Plant>>>

    suspend fun getPlantById(id: Int): Result<Plant>

    suspend fun insertPlant(plant: Plant): Result<Unit>

    suspend fun deletePlant(plant: Plant): Result<Unit>

    suspend fun updatePlant(plant: Plant): Result<Unit>
}