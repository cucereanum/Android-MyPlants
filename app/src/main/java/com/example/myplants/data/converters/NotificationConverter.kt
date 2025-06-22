package com.example.myplants.data.converters

import androidx.room.TypeConverter
import com.example.myplants.data.NotificationType

class NotificationConverter {
    @TypeConverter
    fun fromType(value: NotificationType): String = value.name

    @TypeConverter
    fun toType(value: String): NotificationType = NotificationType.valueOf(value)
}