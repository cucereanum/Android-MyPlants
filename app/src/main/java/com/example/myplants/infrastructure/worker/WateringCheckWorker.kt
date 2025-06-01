package com.example.myplants.infrastructure.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myplants.domain.usecase.CheckForWateringUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WateringCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkForWateringUseCase: CheckForWateringUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("Worker", "Started WateringCheckWorker")
        return try {
            Log.d("WateringCheckWorker", "Executing use case")
            checkForWateringUseCase.execute()
            Log.d("WateringCheckWorker", "Use case complete")
            Result.success()
        } catch (e: Exception) {
            Log.e("WateringCheckWorker", "Worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}