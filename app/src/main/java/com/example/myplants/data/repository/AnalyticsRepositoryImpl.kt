package com.example.myplants.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant
import com.example.myplants.data.WateringHistory
import com.example.myplants.data.data_source.WateringHistoryDao
import com.example.myplants.domain.model.DayData
import com.example.myplants.domain.model.PlantAnalytics
import com.example.myplants.domain.model.StreakData
import com.example.myplants.domain.repository.AnalyticsRepository
import com.example.myplants.domain.repository.PlantRepository
import com.example.myplants.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class AnalyticsRepositoryImpl @Inject constructor(
    private val plantRepository: PlantRepository,
    private val wateringHistoryDao: WateringHistoryDao
) : AnalyticsRepository {

    private companion object {
        const val HISTORY_LIMIT = 1000
        val ONE_WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)
        val ONE_MONTH_MILLIS = TimeUnit.DAYS.toMillis(30)
        val THIRTY_DAYS = 30
    }


    override fun getAnalytics(): Flow<Result<PlantAnalytics>> =
        combine(
            plantRepository.getPlants(),
            wateringHistoryDao.getRecentHistory(limit = HISTORY_LIMIT)
        ) { plantsResult, history ->
            try {
                when (plantsResult) {
                    is Result.Success<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val plants = plantsResult.data as List<Plant>
                        val analytics = calculateAnalytics(plants, history)
                        Result.Success(analytics)
                    }
                    is Result.Error -> {
                        Result.Error(plantsResult.exception, plantsResult.message)
                    }
                    is Result.Loading -> {
                        Result.Loading
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.Error(e, "Failed to calculate analytics: ${e.message}")
            }
        }

    override suspend fun getStreakData(): Result<StreakData> {
        return try {
            Result.Success(StreakData(0, 0, null))
        } catch (e: Exception) {
            Result.Error(e, "Failed to get streak data: ${e.message}")
        }
    }


    private fun calculateAnalytics(
        plants: List<Plant>,
        history: List<WateringHistory>
    ): PlantAnalytics {
        // Pre-calculate timestamps
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - ONE_WEEK_MILLIS
        val oneMonthAgo = now - ONE_MONTH_MILLIS

        // Pre-convert dates once (performance optimization)
        val historyWithDates = history.map { event ->
            EventWithDate(event, event.wateredAt.toLocalDate())
        }

        // Calculate streak ONCE and reuse
        val streakData = calculateStreak(historyWithDates)
        val healthScore = calculateHealthScore(plants, historyWithDates, streakData, oneWeekAgo)
        val last30Days = getLast30DaysData(plants, historyWithDates)

        // Efficient filtering
        val wateredThisWeek = historyWithDates.count { it.event.wateredAt >= oneWeekAgo }
        val wateredThisMonth = historyWithDates.count { it.event.wateredAt >= oneMonthAgo }

        // Find insights
        val mostCaredForPlant = findMostCaredForPlant(plants, historyWithDates, oneMonthAgo)
        val needsAttentionPlant = findNeedsAttentionPlant(plants, historyWithDates, oneMonthAgo)

        return PlantAnalytics(
            totalPlants = plants.size,
            activePlants = plants.count { plant ->
                plant.selectedDays.contains(DayOfWeek.today())
            },
            totalWaterings = history.size,
            currentStreak = streakData.currentStreak,
            longestStreak = streakData.longestStreak,
            healthScore = healthScore,
            wateredThisWeek = wateredThisWeek,
            wateredThisMonth = wateredThisMonth,
            missedWaterings = 0, // TODO: Calculate based on schedule
            mostCaredForPlant = mostCaredForPlant,
            needsAttentionPlant = needsAttentionPlant,
            last30Days = last30Days
        )
    }

    /**
     * Helper data class to avoid repeated date conversions
     */
    private data class EventWithDate(
        val event: WateringHistory,
        val date: LocalDate
    )

    /**
     * Extension function for date conversion (DRY principle)
     */
    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    /**
     * Calculate streak - optimized algorithm
     */
    private fun calculateStreak(historyWithDates: List<EventWithDate>): StreakData {
        if (historyWithDates.isEmpty()) return StreakData(0, 0, null)

        // Get unique dates, sorted descending
        val datesWatered = historyWithDates
            .map { it.date }
            .distinct()
            .sortedDescending()

        // Calculate current streak
        var currentStreak = 0
        var expectedDate = LocalDate.now()

        for (date in datesWatered) {
            val daysDiff = ChronoUnit.DAYS.between(date, expectedDate)
            if (daysDiff in 0L..1L) {
                currentStreak++
                expectedDate = date.minusDays(1)
            } else break
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 1

        for (i in 1 until datesWatered.size) {
            val daysDiff = ChronoUnit.DAYS.between(datesWatered[i], datesWatered[i - 1])
            if (daysDiff == 1L) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
        }

        return StreakData(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            lastWateredDate = datesWatered.firstOrNull()
        )
    }

    /**
     * Calculate health score - accepts pre-calculated streak to avoid duplication
     */
    /**
     * Calculate health score based on scheduled watering days (not arbitrary daily watering)
     * 
     * Formula:
     * - Adherence (50%): Did you water plants on THEIR scheduled days?
     * - Completion (30%): What % of plants got all scheduled waterings?
     * - Streak (20%): Consecutive days watered (encourages daily check-ins)
     */
    private fun calculateHealthScore(
        plants: List<Plant>,
        historyWithDates: List<EventWithDate>,
        streakData: StreakData,
        oneWeekAgo: Long
    ): Float {
        if (plants.isEmpty()) return 0f

        val lastWeekHistory = historyWithDates.filter { it.event.wateredAt >= oneWeekAgo }
        if (lastWeekHistory.isEmpty()) return 0f

        val today = LocalDate.now()
        
        // Factor 1: Adherence (50%) - Schedule-aware!
        // Check if plants were watered on THEIR scheduled days
        var scheduledWaterings = 0
        var completedWaterings = 0
        val plantAdherence = mutableMapOf<Int, Pair<Int, Int>>() // plantId -> (completed, total)
        
        plants.forEach { plant ->
            var plantScheduled = 0
            var plantCompleted = 0
            
            // Check each day in last 7 days
            for (daysAgo in 0 until 7) {
                val date = today.minusDays(daysAgo.toLong())
                val dayOfWeek = date.dayOfWeek.value.toDayOfWeek()
                
                // Is this a scheduled watering day for this plant?
                if (plant.selectedDays.contains(dayOfWeek)) {
                    plantScheduled++
                    scheduledWaterings++
                    
                    // Was it actually watered on this day?
                    val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    
                    val wasWatered = lastWeekHistory.any { 
                        it.event.plantId == plant.id && 
                        it.event.wateredAt in dayStart until dayEnd 
                    }
                    
                    if (wasWatered) {
                        plantCompleted++
                        completedWaterings++
                    }
                }
            }
            
            if (plantScheduled > 0) {
                plantAdherence[plant.id] = Pair(plantCompleted, plantScheduled)
            }
        }
        
        val adherence = if (scheduledWaterings > 0) {
            (completedWaterings.toFloat() / scheduledWaterings) * 100f
        } else {
            100f // No scheduled waterings = perfect score
        }
        
        // Factor 2: Completion (30%) - What % of plants got ALL their scheduled waterings?
        val plantsFullyCaredFor = plantAdherence.count { (_, pair) ->
            pair.first == pair.second // completed == total scheduled
        }
        
        val completion = if (plantAdherence.isNotEmpty()) {
            (plantsFullyCaredFor.toFloat() / plantAdherence.size) * 100f
        } else {
            100f
        }
        
        // Factor 3: Streak bonus (20%) - Maxes out at 10-day streak
        // Need to reach 100 before multiplying by 0.2 to get 20 points
        val streakBonus = minOf(streakData.currentStreak * 10f, 100f)
        
        return ((adherence * 0.5f) + (completion * 0.3f) + (streakBonus * 0.2f))
            .coerceIn(0f, 100f)
    }

    /**
     * Get chart data for last 30 days
     */
    private fun getLast30DaysData(
        plants: List<Plant>,
        historyWithDates: List<EventWithDate>
    ): List<DayData> {
        val today = LocalDate.now()

        return (0 until THIRTY_DAYS).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd =
                date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val plantsWatered = historyWithDates.count {
                it.event.wateredAt in dayStart until dayEnd
            }

            val dayOfWeek = date.dayOfWeek.value.toDayOfWeek()
            val plantsDue = plants.count { it.selectedDays.contains(dayOfWeek) }

            DayData(date, plantsWatered, plantsDue)
        }.reversed() // Oldest to newest for chart
    }

    /**
     * Find most cared for plant - extracted for clarity
     */
    private fun findMostCaredForPlant(
        plants: List<Plant>,
        historyWithDates: List<EventWithDate>,
        since: Long
    ): Plant? {
        if (plants.isEmpty()) return null

        val wateringCounts = plants.associateWith { plant ->
            historyWithDates.count { it.event.plantId == plant.id && it.event.wateredAt >= since }
        }

        return wateringCounts.maxByOrNull { it.value }?.key
    }

    /**
     * Find plant needing attention - extracted for clarity
     */
    private fun findNeedsAttentionPlant(
        plants: List<Plant>,
        historyWithDates: List<EventWithDate>,
        since: Long
    ): Plant? {
        if (plants.size <= 1) return null

        val wateringCounts = plants.associateWith { plant ->
            historyWithDates.count { it.event.plantId == plant.id && it.event.wateredAt >= since }
        }

        return wateringCounts.minByOrNull { it.value }?.key
    }

    /**
     * Helper to convert DayOfWeek value to app's DayOfWeek enum
     */
    private fun Int.toDayOfWeek(): DayOfWeek = when (this) {
        1 -> DayOfWeek.Monday
        2 -> DayOfWeek.Tuesday
        3 -> DayOfWeek.Wednesday
        4 -> DayOfWeek.Thursday
        5 -> DayOfWeek.Friday
        6 -> DayOfWeek.Saturday
        7 -> DayOfWeek.Sunday
        else -> DayOfWeek.Monday
    }
}
