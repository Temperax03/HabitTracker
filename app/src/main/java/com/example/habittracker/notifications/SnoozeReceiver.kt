package com.example.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.data.model.ReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val habitId = intent.getStringExtra(NotificationReceiver.KEY_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(NotificationReceiver.KEY_HABIT_NAME) ?: return
        val streak = intent.getIntExtra(NotificationReceiver.KEY_HABIT_STREAK, 0)
        val originalTime = intent.getStringExtra(NotificationReceiver.KEY_NOTIFICATION_TIME)
        val days = intent.getIntArrayExtra(NotificationReceiver.KEY_NOTIFICATION_DAYS)?.toList().orEmpty()

        val pendingResult = goAsync()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val snoozedTime = runCatching { originalTime?.let { LocalTime.parse(it, formatter) } }
            .getOrNull()
            ?.let { it.plusMinutes(SNOOZE_MINUTES) }
            ?: LocalDateTime.now().plusMinutes(SNOOZE_MINUTES).toLocalTime()
        val reminder = ReminderTime(time = snoozedTime.format(formatter), days = days)

        CoroutineScope(Dispatchers.IO).launch {
            NotificationScheduler(context.applicationContext).scheduleSnooze(
                habitId = habitId,
                habitName = habitName,
                streak = streak,
                reminder = reminder,
                delayMinutes = SNOOZE_MINUTES
            )
            NotificationManagerCompat.from(context).cancel(habitId.hashCode())
            pendingResult.finish()
        }
    }

    companion object {
        private const val SNOOZE_MINUTES = 30L
    }
}