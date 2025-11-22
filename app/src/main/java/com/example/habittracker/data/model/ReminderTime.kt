package com.example.habittracker.data.model

data class ReminderTime(
    val time: String,
    val days: List<Int> = emptyList()
) {
    constructor(hour: Int, minute: Int, days: List<Int> = emptyList()) : this(
        time = "%02d:%02d".format(hour, minute),
        days = days
    )

    val hour: Int
        get() = time.substringBefore(":").toIntOrNull() ?: 0

    val minute: Int
        get() = time.substringAfter(":").toIntOrNull() ?: 0
}


