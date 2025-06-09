package com.example.myplants.data.converters

import androidx.room.TypeConverter
import com.example.myplants.data.DayOfWeek

class Converters {

    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekList(data: String): List<DayOfWeek> {
        return if (data.isBlank()) emptyList()
        else data.split(",").map { DayOfWeek.valueOf(it) }
    }
}