package com.example.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.habittracker.notifications.NotificationReceiver.Companion.KEY_HABIT_ID

class QuickCheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val habitId = intent.getStringExtra(KEY_HABIT_ID) ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val database = HabitDatabase.getInstance(appContext)
                val repository = HabitRepository(database.habitDao())
                val updatedHabit = repository.toggleCompletionById(habitId)
                if (updatedHabit != null) {
                    NotificationScheduler(appContext).schedule(
                        habitId = updatedHabit.id,
                        habitName = updatedHabit.name,
                        streak = updatedHabit.streak,
                        reminders = updatedHabit.reminders
                    )
                }
            }
            NotificationManagerCompat.from(appContext).cancel(habitId.hashCode())
            pendingResult.finish()
        }
    }
}