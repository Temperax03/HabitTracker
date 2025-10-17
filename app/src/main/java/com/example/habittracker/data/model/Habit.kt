package com.example.habittracker.data.model

data class Habit(
    val id: String = "",
    val name: String = "",
    val completedDates: List<String> = emptyList(),
    val streak: Int = 0,

    val icon: String = "🔥",
    val weeklyGoal: Int = 5
)
