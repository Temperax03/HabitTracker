package com.example.habittracker.data.model

data class ReminderTime(
    val time: String = "",
    val days: List<Int> = emptyList()
)