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

class HabitViewModel(
    application: Application,
    private val repository: HabitRepository
) : AndroidViewModel(application) {

    private val _habits = mutableStateListOf<Habit>()
    val habits: List<Habit> get() = _habits

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

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

    fun addHabit(name: String, icon: String = "🔥", weeklyGoal: Int = 5, notificationTime: String? = null) {
        viewModelScope.launch {
            val userId = ensureUserAvailable() ?: return@launch
            errorMessage = null
            val sanitizedGoal = weeklyGoal.coerceIn(1, 7)
            val validationError = repository.validateUniqueness(name, icon, excludeId = null)
            if (name.isBlank()) {
                errorMessage = "A név nem lehet üres."
                return@launch
            }
            if (validationError != null) {
                errorMessage = validationError
                return@launch
            }
            val habit = Habit(
                name = name.trim(),
                icon = icon,
                weeklyGoal = sanitizedGoal,
                ownerId = userId,
                notificationTime = notificationTime
            )
            runCatching { repository.addHabit(userId, habit) }
                .onSuccess { savedHabit ->
                    _habits.removeAll { it.id == savedHabit.id }
                    _habits.add(savedHabit)
                    notificationScheduler.schedule(
                        habitId = savedHabit.id,
                        habitName = savedHabit.name,
                        streak = savedHabit.streak,
                        notificationTime = savedHabit.notificationTime
                    )
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült menteni a szokást." }
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            val userId = ensureUserAvailable() ?: return@launch
            notificationScheduler.cancel(id)
            runCatching { repository.deleteHabit(userId, id) }
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
                    notificationScheduler.schedule(
                        habitId = updated.id,
                        habitName = updated.name,
                        streak = updated.streak,
                        notificationTime = updated.notificationTime
                    )
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült frissíteni a szokást." }
        }
    }

    fun updateHabitDetails(habit: Habit, name: String, icon: String, weeklyGoal: Int) {
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
            )
            runCatching { repository.updateHabit(userId, updated) }
                .onSuccess {
                    notificationScheduler.schedule(
                        habitId = updated.id,
                        habitName = updated.name,
                        streak = updated.streak,
                        notificationTime = updated.notificationTime
                    )
                }
                .onFailure { errorMessage = it.message ?: "Nem sikerült frissíteni a szokást." }
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
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