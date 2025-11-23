package com.example.habittracker.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.model.Habit
import com.example.habittracker.data.repository.HabitRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.habittracker.notifications.NotificationScheduler
import com.example.habittracker.data.model.ReminderTime
import androidx.compose.runtime.derivedStateOf
import java.time.temporal.ChronoUnit
class HabitViewModel(
    application: Application,
    private val repository: HabitRepository
) : AndroidViewModel(application) {

    data class HabitInput(
        val name: String,
        val icon: String = "🔥",
        val weeklyGoal: Int = 5,
        val reminders: List<ReminderTime> = emptyList()
    )
    private val _habits = mutableStateListOf<Habit>()
    val habits: List<Habit> get() = _habits

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private val analyticsState = derivedStateOf { calculateAnalytics(_habits) }
    val analytics: HabitAnalytics get() = analyticsState.value
    private var currentUserId: String? = null
    private var listener: ListenerRegistration? = null

    private val notificationScheduler = NotificationScheduler(application)

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        try {
            isLoading = true
            val cached = repository.loadCachedHabits()
            _habits.clear(); _habits.addAll(cached)
            ensureUserAvailable()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ismeretlen hiba történt"
        } finally {
            isLoading = false
        }
    }

    private suspend fun ensureUserAvailable(): String? {
        currentUserId?.let { return it }
        val userId = runCatching { repository.ensureUserId() }
            .onFailure { errorMessage = "Nem sikerült bejelentkezni: ${it.message}" }
            .getOrNull()
        if (userId != null) {
            currentUserId = userId
            startListening(userId)
        }
        return userId
    }

    private fun startListening(userId: String) {
        listener?.remove()
        listener = repository.listenToHabits(
            userId = userId,
            onChange = { habits ->
                viewModelScope.launch {
                    repository.replaceCache(habits, userId)
                    _habits.clear(); _habits.addAll(habits)
                }
            },
            onError = { throwable ->
                errorMessage = throwable.message ?: "Nem sikerült frissíteni a szokásokat."
            }
        )
    }

    fun addHabit(
        name: String,
        icon: String = "🔥",
        weeklyGoal: Int = 5,
        reminders: List<ReminderTime> = emptyList()
    ) {
        viewModelScope.launch { addHabitInternal(name, icon, weeklyGoal, reminders) }
    }

    suspend fun addHabitsBlocking(habits: List<HabitInput>) {
        habits.forEach { habit ->
            addHabitInternal(
                name = habit.name,
                icon = habit.icon,
                weeklyGoal = habit.weeklyGoal,
                reminders = habit.reminders
            )

        }
    }
    private suspend fun addHabitInternal(
        name: String,
        icon: String,
        weeklyGoal: Int,
        reminders: List<ReminderTime>
    ) {
        val userId = ensureUserAvailable() ?: return
        errorMessage = null
        val sanitizedGoal = weeklyGoal.coerceIn(1, 7)
        val validationError = repository.validateUniqueness(name, icon, excludeId = null)
        if (name.isBlank()) {
            errorMessage = "A név nem lehet üres."
            return
        }
        if (validationError != null) {
            errorMessage = validationError
            return
        }
        val habit = Habit(
            name = name.trim(),
            icon = icon,
            weeklyGoal = sanitizedGoal,
            ownerId = userId,
            reminders = reminders
        )
        runCatching { repository.addHabit(userId, habit) }
            .onSuccess { savedHabit ->
                _habits.removeAll { it.id == savedHabit.id }
                _habits.add(savedHabit)
                notificationScheduler.schedule(
                    habitId = savedHabit.id,
                    habitName = savedHabit.name,
                    streak = savedHabit.streak,
                    reminders = savedHabit.reminders
                )
            }
            .onFailure { errorMessage = it.message ?: "Nem sikerült menteni a szokást." }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            val userId = ensureUserAvailable() ?: return@launch
            notificationScheduler.cancel(id)
            runCatching { repository.deleteHabit(userId, id) }
                .onSuccess {
                    _habits.removeAll { it.id == id }
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült törölni a szokást." }
        }
    }

    fun toggleCompletion(habit: Habit, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val userId = ensureUserAvailable() ?: return@launch
            val dates = habit.completedDates.toMutableSet()
            val dateString = date.toString()
            if (dates.contains(dateString)) {
                dates.remove(dateString)
            } else {
                dates.add(dateString)
            }
            val sortedDates = dates.map { it }.sorted()
            val streak = repository.calculateStreak(sortedDates)
            val updated = habit.copy(completedDates = sortedDates, streak = streak,)
            runCatching { repository.updateHabit(userId, updated) }
                .onSuccess {
                    val index = _habits.indexOfFirst { it.id == updated.id }
                    if (index >= 0) {
                        _habits[index] = updated
                    }
                    notificationScheduler.schedule(
                        habitId = updated.id,
                        habitName = updated.name,
                        streak = updated.streak,
                        reminders = updated.reminders
                    )
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült frissíteni a szokást." }
        }
    }

    fun updateHabitDetails(
        habit: Habit,
        name: String,
        icon: String,
        weeklyGoal: Int,
        reminders: List<ReminderTime>
    ) {
        viewModelScope.launch {
            val userId = ensureUserAvailable() ?: return@launch
            val validationError = repository.validateUniqueness(name, icon, excludeId = habit.id)
            if (validationError != null) {
                errorMessage = validationError
                return@launch
            }
            val sanitizedGoal = weeklyGoal.coerceIn(1, 7)
            val updated = habit.copy(
                name = name.trim(),
                streak = repository.calculateStreak(habit.completedDates),
                icon = icon,
                weeklyGoal = sanitizedGoal,
                reminders = reminders
            )
            runCatching { repository.updateHabit(userId, updated) }
                .onSuccess {
                    val index = _habits.indexOfFirst { it.id == updated.id }
                    if (index >= 0) {
                        _habits[index] = updated
                    }
                    notificationScheduler.schedule(
                        habitId = updated.id,
                        habitName = updated.name,
                        streak = updated.streak,
                        reminders = updated.reminders
                    )
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült frissíteni a szokást." }
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
    private fun calculateAnalytics(habits: List<Habit>): HabitAnalytics {
        if (habits.isEmpty()) return HabitAnalytics()

        val last7Days = generateDateRange(7)
        val last30Days = generateDateRange(30)

        fun Habit.countCompleted(days: List<LocalDate>): Int =
            days.count { completedDates.contains(it.toString()) }

        val total7 = habits.sumOf { it.countCompleted(last7Days) }
        val total30 = habits.sumOf { it.countCompleted(last30Days) }

        val weeklyProgress = habits.map { habit ->
            val goal = habit.weeklyGoal.coerceAtLeast(1)
            val completed = habit.countCompleted(last7Days)
            (completed / goal.toFloat()).coerceIn(0f, 1f)
        }
        val averageWeeklyProgress = weeklyProgress.takeIf { it.isNotEmpty() }
            ?.average()?.toFloat() ?: 0f

        val monthlyProgress = habits.map { habit ->
            habit.countCompleted(last30Days) / 30f
        }
        val averageMonthlyProgress = monthlyProgress.takeIf { it.isNotEmpty() }
            ?.average()?.toFloat()?.coerceIn(0f, 1f) ?: 0f

        val longestStreak = habits.maxOfOrNull { it.streak } ?: 0
        val longestHabits = habits.filter { it.streak == longestStreak && longestStreak > 0 }

        val attentionList = habits.mapNotNull { habit ->
            val goal = habit.weeklyGoal.coerceAtLeast(1)
            val completed = habit.countCompleted(last7Days)
            val progress = (completed / goal.toFloat()).coerceIn(0f, 1f)
            if (progress < 0.6f) HabitAttention(habit, progress) else null
        }

        return HabitAnalytics(
            totalCheckinsLast7Days = total7,
            totalCheckinsLast30Days = total30,
            averageWeeklyGoalProgress = averageWeeklyProgress,
            averageMonthlyCompletion = averageMonthlyProgress,
            longestStreak = longestStreak,
            longestStreakHabits = longestHabits,
            habitsNeedingAttention = attentionList
        )
    }

    private fun generateDateRange(days: Long): List<LocalDate> {
        val today = LocalDate.now()
        return (0 until days).map { today.minus(it, ChronoUnit.DAYS) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                val database = HabitDatabase.getInstance(application)
                val repository = HabitRepository(database.habitDao())
                HabitViewModel(application, repository)
            }
        }
    }
}