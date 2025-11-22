package com.example.habittracker.data.model

data class ReminderTime(
    val hour: Int,
    val minute: Int,
    val days: List<Int> = emptyList()
) {

    val time: String
        get() = "%02d:%02d".format(hour, minute)
}