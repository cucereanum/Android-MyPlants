package com.example.myplants.data

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
    }

}