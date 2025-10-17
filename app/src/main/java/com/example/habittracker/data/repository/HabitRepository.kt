package com.example.habittracker.data.repository

import com.example.habittracker.data.model.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HabitRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("habits")

    fun getHabits(onChange: (List<Habit>) -> Unit): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val habits = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val completedDates = doc.get("completedDates") as? List<String> ?: emptyList()
                    val streak = doc.getLong("streak")?.toInt() ?: 0
                    Habit(doc.id, name, completedDates, streak)
                }
                onChange(habits)
            }
        }
    }

    fun addHabit(name: String) {
        val habitData = hashMapOf("name" to name)
        collection.add(habitData)
    }

    fun deleteHabit(id: String) {
        collection.document(id).delete()
    }

    fun updateHabit(id: String, completedDates: List<String>, streak: Int) {
        collection.document(id).update(
            mapOf("completedDates" to completedDates, "streak" to streak)
        )
    }
}
