package com.example.habittracker.viewmodel

import com.example.habittracker.data.model.Habit

data class HabitAttention(
    val habit: Habit,
    val weeklyProgress: Float
)

data class HabitAnalytics(
    val totalCheckinsLast7Days: Int = 0,
    val totalCheckinsLast30Days: Int = 0,
    val averageWeeklyGoalProgress: Float = 0f,
    val averageMonthlyCompletion: Float = 0f,
    val longestStreak: Int = 0,
    val longestStreakHabits: List<Habit> = emptyList(),
    val habitsNeedingAttention: List<HabitAttention> = emptyList()
)