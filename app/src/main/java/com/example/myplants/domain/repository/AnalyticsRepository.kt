package com.example.myplants.domain.repository

import com.example.myplants.domain.model.PlantAnalytics
import com.example.myplants.domain.model.StreakData
import com.example.myplants.domain.util.Result
import kotlinx.coroutines.flow.Flow


interface AnalyticsRepository {

    fun getAnalytics(): Flow<Result<PlantAnalytics>>

    suspend fun getStreakData(): Result<StreakData>
}
