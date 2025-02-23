package com.example.myplants.data.repository

import com.example.myplants.data.Plant
import com.example.myplants.data.data_source.PlantDao
import com.example.myplants.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow

class PlantRepositoryImpl(
    private val dao: PlantDao
) : PlantRepository {

    override fun getPlants(): Flow<List<Plant>> {
        return dao.getPlants()
    }

    override suspend fun deletePlant(plant: Plant) {
        println("called $plant")
        return dao.deletePlant(plant)
    }

    override suspend fun getPlantById(id: Int): Plant? {
        return dao.getPlantById(id)
    }

    override suspend fun insertPlant(plant: Plant) {
        return dao.insertPlant(plant)
    }
}