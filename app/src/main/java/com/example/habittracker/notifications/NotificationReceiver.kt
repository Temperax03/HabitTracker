package com.example.habittracker.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.R
import com.example.habittracker.ui.MainActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val habitId = intent.getStringExtra(KEY_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(KEY_HABIT_NAME) ?: return
        val streak = intent.getIntExtra(KEY_HABIT_STREAK, 0)
        val time = intent.getStringExtra(KEY_NOTIFICATION_TIME)

        createChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(R.string.notification_body, habitName, streak)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification)

        if (!time.isNullOrBlank()) {
            scheduleNextDay(context, habitId, habitName, streak, time)
        }
    }

    private fun scheduleNextDay(
        context: Context,
        habitId: String,
        habitName: String,
        streak: Int,
        time: String
    ) {
        runCatching {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = LocalTime.parse(time, formatter)
            val tomorrow = LocalDate.now().plusDays(1)
            val triggerAt = tomorrow.atTime(localTime).atZone(java.time.ZoneId.systemDefault()).toInstant()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = NotificationScheduler.buildIntent(context, habitId, habitName, streak, time)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pendingIntent)
        }.getOrElse {
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_NAME = "habit_name"
        const val KEY_HABIT_STREAK = "habit_streak"
        const val KEY_NOTIFICATION_TIME = "notification_time"
    }
}