package com.example.myplants.infrastructure.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myplants.domain.repository.UserPreferencesRepository
import com.example.myplants.domain.usecase.CheckForWateringUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
@RequiresApi(Build.VERSION_CODES.O)
class WateringCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkForWateringUseCase: CheckForWateringUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("Worker", "Started WateringCheckWorker")
        return try {
            val areNotificationsEnabled = userPreferencesRepository
                .areNotificationsEnabledFlow
                .first()
            if (!areNotificationsEnabled) {
                return Result.success()
            }
            checkForWateringUseCase.execute()
            Result.success()
        } catch (e: Exception) {
            Log.e("WateringCheckWorker", "Worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}
