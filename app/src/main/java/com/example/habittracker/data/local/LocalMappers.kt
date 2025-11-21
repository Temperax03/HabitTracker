package com.example.habittracker.data.local

import com.example.habittracker.data.model.Habit

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    completedDates = completedDates,
    streak = streak,
    icon = icon,
    weeklyGoal = weeklyGoal,
    ownerId = ownerId,
    reminders = reminders
)

fun Habit.toEntity(ownerIdOverride: String = ownerId): HabitEntity = HabitEntity(
    id = id,
    name = name,
    completedDates = completedDates,
    streak = streak,
    icon = icon,
    weeklyGoal = weeklyGoal,
    ownerId = ownerIdOverride,
    reminders = reminders,
)
