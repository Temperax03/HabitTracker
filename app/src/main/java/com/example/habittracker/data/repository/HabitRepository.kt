package com.example.habittracker.data.repository

import com.example.habittracker.data.local.HabitDao
import com.example.habittracker.data.local.toDomain
import com.example.habittracker.data.local.toEntity
import com.example.habittracker.data.model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
class HabitRepository(
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun ensureUserId(): String = withContext(dispatcher) {
        val cachedUser = auth.currentUser
        if (cachedUser != null) return@withContext cachedUser.uid

        val localOwnerId = habitDao.getHabits().firstOrNull()?.ownerId
        if (!localOwnerId.isNullOrBlank()) return@withContext localOwnerId

        runCatching { auth.signInAnonymously().await() }
            .mapCatching { result ->
                result.user?.uid ?: throw IllegalStateException("Anonymous authentication failed")
            }
            .getOrElse { UUID.randomUUID().toString() }
    }

    private fun habitCollection(userId: String) =
        firestore.collection("users").document(userId).collection("habits")

    suspend fun loadCachedHabits(): List<Habit> = withContext(dispatcher) {
        habitDao.getHabits().map { it.toDomain() }
    }

    suspend fun replaceCache(habits: List<Habit>, ownerId: String) = withContext(dispatcher) {
        habitDao.clear()
        if (habits.isNotEmpty()) {
            habitDao.upsertAll(habits.map { it.toEntity(ownerId) })
        }
    }

    fun listenToHabits(
        userId: String,
        onChange: (List<Habit>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return habitCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

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
                    weeklyGoal = weeklyGoal,
                    ownerId = userId
                )
            }

            onChange(habits)
        }
    }

    suspend fun addHabit(userId: String, habit: Habit): Habit = withContext(dispatcher) {
        val weeklyGoal = habit.weeklyGoal.coerceIn(1, 7)
        val data = mapOf(
            "name" to habit.name,
            "completedDates" to habit.completedDates,
            "streak" to habit.streak,
            "icon" to habit.icon,
            "weeklyGoal" to weeklyGoal,
            "ownerId" to userId
        )
        val habitId = runCatching { habitCollection(userId).add(data).await().id }
            .getOrElse { habit.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString() }

        val storedHabit = habit.copy(id = habitId, weeklyGoal = weeklyGoal, ownerId = userId)
        habitDao.upsert(storedHabit.toEntity(userId))
        storedHabit
    }

    suspend fun deleteHabit(userId: String, id: String) = withContext(dispatcher) {
        habitDao.deleteById(id)
        habitCollection(userId).document(id).delete().await()
    }

    suspend fun updateHabit(userId: String, habit: Habit) = withContext(dispatcher) {
        val document = habitCollection(userId).document(habit.id)
        val weeklyGoal = habit.weeklyGoal.coerceIn(1, 7)
        val payload = mapOf(
            "name" to habit.name,
            "completedDates" to habit.completedDates,
            "streak" to habit.streak,
            "icon" to habit.icon,
            "weeklyGoal" to weeklyGoal,
            "ownerId" to userId
        )
        document.set(payload).await()
        habitDao.upsert(habit.toEntity(userId))
    }

    suspend fun validateUniqueness(name: String, icon: String, excludeId: String?): String? = withContext(dispatcher) {
        val normalized = name.trim().lowercase()
        val cached = habitDao.getHabits()
        if (cached.any { it.id != excludeId && it.name.trim().lowercase() == normalized }) {
            return@withContext "Már létezik ilyen nevű szokás."
        }
        if (cached.any { it.id != excludeId && it.icon == icon }) {
            return@withContext "Válassz másik ikont, ez már használatban van."
        }
        null
    }

    fun calculateStreak(completedDates: List<String>): Int {
        if (completedDates.isEmpty()) return 0
        val dates = completedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sortedDescending()
        var streak = 0
        var cursor = LocalDate.now()
        for (date in dates) {
            if (date == cursor) {
                streak++
                cursor = cursor.minusDays(1)
            } else if (date.isBefore(cursor)) {
                break
            }
        }
        return streak
    }
}