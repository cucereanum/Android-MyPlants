package com.example.myplants.data

import java.util.Calendar

enum class DayOfWeek(val dayName: String) {
    Monday("Monday"),
    Tuesday("Tuesday"),
    Wednesday("Wednesday"),
    Thursday("Thursday"),
    Friday("Friday"),
    Saturday("Saturday"),
    Sunday("Sunday");

    companion object {
        fun fromDisplayName(name: String): DayOfWeek? {
            return entries.find { it.dayName.equals(name, ignoreCase = true) }
        }

        fun allDays(): List<DayOfWeek> = entries

        fun today(): DayOfWeek {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> Monday
                Calendar.TUESDAY -> Tuesday
                Calendar.WEDNESDAY -> Wednesday
                Calendar.THURSDAY -> Thursday
                Calendar.FRIDAY -> Friday
                Calendar.SATURDAY -> Saturday
                Calendar.SUNDAY -> Sunday
                else -> throw IllegalStateException("Invalid day of week")
            }
        }
        
    }

}