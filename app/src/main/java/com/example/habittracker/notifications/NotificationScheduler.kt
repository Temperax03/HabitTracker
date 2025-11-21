package com.example.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.edit
import com.example.habittracker.data.model.ReminderTime
import java.time.DayOfWeek

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("notification_scheduler", Context.MODE_PRIVATE)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")
    fun schedule(habitId: String, habitName: String, streak: Int, reminders: List<ReminderTime>) {
        cancel(habitId)
        if (reminders.isEmpty()) return

        val requestCodes = reminders.mapNotNull { reminder ->
            scheduleReminder(habitId, habitName, streak, reminder)

        }
        prefs.edit { putString(habitId, requestCodes.joinToString(",")) }
    }

    private fun scheduleReminder(
        habitId: String,
        habitName: String,
        streak: Int,
        reminder: ReminderTime
    ): Int? {
        val triggerAt = computeNextTrigger(reminder) ?: return null
        val requestCode = buildRequestCode(habitId, reminder)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            buildIntent(context, habitId, habitName, streak, reminder),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        return requestCode
    }

    fun cancel(habitId: String) {
        val storedCodes = prefs.getString(habitId, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            .orEmpty()
        storedCodes.forEach { code ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        prefs.edit { remove(habitId) }
    }

    private fun computeNextTrigger(reminder: ReminderTime): Long? {
        return runCatching {
            val time = LocalTime.parse(reminder.time, formatter)
            val now = LocalDateTime.now()
            val allowedDays = reminder.days.takeIf { it.isNotEmpty() }
                ?.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
                ?.toSet()

            for (offset in 0..13) {
                val candidateDate = now.toLocalDate().plusDays(offset.toLong())
                val dayMatches = allowedDays?.contains(candidateDate.dayOfWeek) ?: true
                val candidateDateTime = candidateDate.atTime(time)
                if (dayMatches && candidateDateTime.isAfter(now)) {
                    return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            null
        }.getOrNull()
    }

    private fun buildRequestCode(habitId: String, reminder: ReminderTime): Int {
        val dayKey = reminder.days.sorted().joinToString(",")
        return (habitId + reminder.time + dayKey).hashCode()
    }


    companion object {
        fun buildIntent(
            context: Context,
            habitId: String,
            habitName: String,
            streak: Int,
            reminder: ReminderTime
        ): Intent {
            return Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.KEY_HABIT_ID, habitId)
                putExtra(NotificationReceiver.KEY_HABIT_NAME, habitName)
                putExtra(NotificationReceiver.KEY_HABIT_STREAK, streak)
                putExtra(NotificationReceiver.KEY_NOTIFICATION_TIME, reminder.time)
                putExtra(NotificationReceiver.KEY_NOTIFICATION_DAYS, reminder.days.toIntArray())
            }
        }
    }
}