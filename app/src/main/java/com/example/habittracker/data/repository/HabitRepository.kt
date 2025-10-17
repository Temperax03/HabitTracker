package com.example.habittracker.data.repository

import android.R.attr.icon
import com.example.habittracker.data.model.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.protobuf.LazyStringArrayList.emptyList
import kotlin.collections.emptyList



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


                    val icon = doc.getString("icon") ?: "🔥"
                    val weeklyGoal = doc.getLong("weeklyGoal")?.toInt() ?: 5
                    Habit(
                        id = doc.id,
                        name = name,
                        completedDates = completedDates,
                        streak = streak,
                        icon = icon,
                        weeklyGoal = weeklyGoal
                    )
                }
                onChange(habits)
            }
        }
    }

    fun addHabit(name: String,
                 icon: String = "🔥",
                 weeklyGoal: Int = 5  ) {
        val habitData = hashMapOf(
            "name" to name,
            "completedDates" to emptyList<String>(),
            "streak" to 0,
            "icon" to icon,
            "weeklyGoal" to weeklyGoal
        )
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
    fun updateHabitName(id: String, name: String) {
        collection.document(id).update(mapOf("name" to name))
    }
    fun updateHabitIcon(id: String, icon: String) {          // ÚJ
        collection.document(id).update("icon", icon)
    }

    fun updateHabitWeeklyGoal(id: String, weeklyGoal: Int) { // ÚJ
        collection.document(id).update("weeklyGoal", weeklyGoal)
    }
}
