package com.example.habittracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val weeklyGoal: Int,
    val completedDates: List<String>,
    val streak: Int,
    val ownerId: String,
    val notificationTime: String?
)