package com.example.habittracker.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.habittracker.data.model.Habit
import com.example.habittracker.data.repository.HabitRepository
import com.google.firebase.firestore.ListenerRegistration

class HabitViewModel : ViewModel() {

    private val repository = HabitRepository()
    private val _habits = mutableStateListOf<Habit>()
    val habits: List<Habit> get() = _habits

    private var listener: ListenerRegistration? = null

    init {
        listener = repository.getHabits { newHabits ->
            _habits.clear()
            _habits.addAll(newHabits)
        }
    }

    fun addHabit(name: String) = repository.addHabit(name)
    fun deleteHabit(id: String) = repository.deleteHabit(id)

    fun updateHabit(habit: Habit, completedDates: List<String>, streak: Int) {
        repository.updateHabit(habit.id, completedDates, streak)
    }
    fun updateHabitName(id: String, name: String) {
        repository.updateHabitName(id, name)
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
