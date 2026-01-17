package com.example.myplants.data.repository

import com.example.myplants.data.Plant
import com.example.myplants.data.data_source.PlantDao
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.util.AppException
import com.example.myplants.domain.util.Result
import com.example.myplants.domain.util.toAppException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PlantRepositoryImpl(
    private val dao: PlantDao
) : PlantRepository {

    override fun getPlants(): Flow<Result<List<Plant>>> {
        return dao.getPlants()
            .map<List<Plant>, Result<List<Plant>>> { plants ->
                Result.Success(plants)
            }
            .catch { throwable ->
                emit(
                    Result.Error(
                        exception = AppException.DatabaseException(
                            message = "Failed to load plants",
                            cause = throwable
                        ),
                        message = "Failed to load plants"
                    )
                )
            }
    }

    override suspend fun getPlantById(id: Int): Result<Plant> {
        return try {
            val plant = dao.getPlantById(id)
            if (plant != null) {
                Result.Success(plant)
            } else {
                Result.Error(
                    exception = AppException.NotFoundException(
                        message = "Plant not found",
                        resourceId = id
                    ),
                    message = "Plant with ID $id not found"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                exception = AppException.DatabaseException(
                    message = "Failed to load plant",
                    cause = e
                ),
                message = "Failed to load plant with ID $id"
            )
        }
    }

    override suspend fun insertPlant(plant: Plant): Result<Unit> {
        return try {
            dao.insertPlant(plant)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = AppException.DatabaseException(
                    message = "Failed to save plant",
                    cause = e
                ),
                message = "Failed to save ${plant.plantName}"
            )
        }
    }

    override suspend fun deletePlant(plant: Plant): Result<Unit> {
        return try {
            dao.deletePlant(plant)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = AppException.DatabaseException(
                    message = "Failed to delete plant",
                    cause = e
                ),
                message = "Failed to delete ${plant.plantName}"
            )
        }
    }

    override suspend fun updatePlant(plant: Plant): Result<Unit> {
        return try {
            dao.updatePlant(plant)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = AppException.DatabaseException(
                    message = "Failed to update plant",
                    cause = e
                ),
                message = "Failed to update ${plant.plantName}"
            )
        }
    }
}