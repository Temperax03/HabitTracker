package com.example.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(habitId: String, habitName: String, streak: Int, notificationTime: String?) {
        if (notificationTime.isNullOrBlank()) return
        runCatching {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = LocalTime.parse(notificationTime, formatter)
            val now = LocalDateTime.now()
            var triggerDateTime = now.toLocalDate().atTime(localTime)
            if (triggerDateTime.isBefore(now)) {
                triggerDateTime = triggerDateTime.plusDays(1)
            }
            val triggerInstant = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                buildIntent(context, habitId, habitName, streak, notificationTime),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerInstant.toEpochMilli(),
                pendingIntent
            )
        }.getOrElse {
        }
    }

    fun cancel(habitId: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            Intent(context, NotificationReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        fun buildIntent(
            context: Context,
            habitId: String,
            habitName: String,
            streak: Int,
            notificationTime: String
        ): Intent {
            return Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.KEY_HABIT_ID, habitId)
                putExtra(NotificationReceiver.KEY_HABIT_NAME, habitName)
                putExtra(NotificationReceiver.KEY_HABIT_STREAK, streak)
                putExtra(NotificationReceiver.KEY_NOTIFICATION_TIME, notificationTime)
            }
        }
    }
}