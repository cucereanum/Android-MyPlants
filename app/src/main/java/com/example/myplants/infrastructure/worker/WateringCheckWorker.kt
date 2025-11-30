package com.example.myplants.infrastructure.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myplants.domain.usecase.CheckForWateringUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
@RequiresApi(Build.VERSION_CODES.O)
class WateringCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkForWateringUseCase: CheckForWateringUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("Worker", "Started WateringCheckWorker")
        return try {
            //checkForWateringUseCase.execute()
            Result.success()
        } catch (e: Exception) {
            Log.e("WateringCheckWorker", "Worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}
