package com.example.habittracker.data.local

import androidx.room.TypeConverter
import com.example.habittracker.data.model.ReminderTime

class Converters {

    @TypeConverter
    fun fromList(value: List<String>): String =
        value.joinToString(separator = "|")

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList()
        else value.split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    @TypeConverter
    fun fromReminderList(reminders: List<ReminderTime>): String =
        reminders.joinToString(separator = "||") { reminder ->
            val dayPart = reminder.days.joinToString(",")
            "${reminder.time}##$dayPart"
        }

    @TypeConverter
    fun toReminderList(value: String): List<ReminderTime> {
        if (value.isBlank()) return emptyList()
        return value.split("||").mapNotNull { entry ->
            val parts = entry.split("##")
            val time = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val days = parts.getOrNull(1)
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { it.toIntOrNull() }
                ?: emptyList()

            ReminderTime(time = time, days = days)
        }
    }
}
