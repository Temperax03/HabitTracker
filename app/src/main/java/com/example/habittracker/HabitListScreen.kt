@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)


package com.example.habittracker

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size

@Composable
fun HabitListScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()

    data class Habit(
        val id: String,
        val name: String,
        val completedDates: List<String> = emptyList(),
        val streak: Int = 0
    )

    val habits = remember { mutableStateListOf<Habit>() }
    var newHabit by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }
    // Firestore frissito

    DisposableEffect(Unit) {

        val listener = firestore.collection("habits")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    habits.clear()
                    for (doc in snapshot.documents) {
                        val name = doc.getString("name") ?: continue
                        val completedDates =
                            doc.get("completedDates") as? List<String> ?: emptyList()
                        val streak = doc.getLong("streak")?.toInt() ?: 0
                        habits.add(Habit(doc.id, name, completedDates, streak))
                    }
                }
            }
        onDispose { listener.remove() }
    }


    // A "bottom sheet" komponens
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Új szokás hozzáadása",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newHabit,
                    onValueChange = { newHabit = it },
                    label = { Text("Szokás neve") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newHabit.isNotBlank()) {
                            val habitData = hashMapOf("name" to newHabit)
                            firestore.collection("habits").add(habitData)

                            // 🔹 Elmentjük, hogy a felhasználó már hozzáadott egy szokást
                            val prefs = navController.context.getSharedPreferences(
                                "HabitPrefs",
                                Context.MODE_PRIVATE
                            )
                            prefs.edit { putBoolean("hasHabits", true) }

                            newHabit = ""
                            coroutineScope.launch { sheetState.hide() }
                                .invokeOnCompletion { isSheetOpen = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mentés")
                }


                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }
                            .invokeOnCompletion { isSheetOpen = false }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mégse")
                }
            }
        }
    }
    var selectedView by remember { mutableStateOf("Today") }
    val viewOptions = listOf("Today", "Weekly", "Monthly")
    // A fő lista felület
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szokásaim") },
                actions = {
                    IconButton(onClick = { isSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Új szokás"
                        )
                    }
                }
            )
        }


    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // 🟦 1️⃣ A gombok most már a "Szokásaim" cím ALATT lesznek
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                viewOptions.forEach { view ->
                    Button(
                        onClick = { selectedView = view },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedView == view)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(view)
                    }
                }
            }

            val now = LocalDate.now()

            val periodDates = when (selectedView) {
                "Today" -> listOf(now)

                //  Heti nézet: hétfőtől vasárnapig
                "Weekly" -> {
                    val startOfWeek = now.with(java.time.DayOfWeek.MONDAY)
                    (0..6).map { startOfWeek.plusDays(it.toLong()) }
                }

                //  Havi nézet: adott hónap 1-től az utolsó napig
                "Monthly" -> {
                    val startOfMonth = now.withDayOfMonth(1)
                    val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                    generateSequence(startOfMonth) { date ->
                        if (date.isBefore(endOfMonth)) date.plusDays(1) else null
                    }.toList()
                }

                else -> listOf(now)
            }.map { it.toString() }

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(habits, key = { it.id }) { habit ->

                    val today = LocalDate.now().toString()
                    val isCompletedToday = habit.completedDates.contains(today)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Szokás neve és streak
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = habit.name, fontSize = 18.sp)
                                    Text(text = "🔥 ${habit.streak}", fontSize = 14.sp)
                                }

                                IconButton(
                                    onClick = {
                                        firestore.collection("habits")
                                            .document(habit.id)
                                            .delete()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Törlés"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            when (selectedView) {

                                "Today" -> {
                                    Checkbox(
                                        checked = isCompletedToday,
                                        onCheckedChange = { checked ->
                                            val newCompletedDates = habit.completedDates.toMutableList()
                                            var newStreak = habit.streak
                                            val yesterday = LocalDate.now().minusDays(1).toString()

                                            if (checked && !isCompletedToday) {
                                                newCompletedDates.add(today)
                                                newStreak = if (habit.completedDates.contains(yesterday)) {
                                                    newStreak + 1
                                                } else 1
                                            } else if (!checked && isCompletedToday) {
                                                newCompletedDates.remove(today)
                                                newStreak = 0
                                            }

                                            firestore.collection("habits").document(habit.id)
                                                .update(
                                                    mapOf(
                                                        "completedDates" to newCompletedDates,
                                                        "streak" to newStreak
                                                    )
                                                )
                                        }
                                    )
                                }


                                "Weekly", "Monthly" -> {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        periodDates.forEach { date ->
                                            val isCompleted = habit.completedDates.contains(date)

                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        color = if (isCompleted)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                                    .clickable {
                                                        val newCompletedDates =
                                                            habit.completedDates.toMutableList()
                                                        var newStreak = habit.streak

                                                        if (isCompleted) {
                                                            newCompletedDates.remove(date)
                                                            newStreak = 0
                                                        } else {
                                                            newCompletedDates.add(date)
                                                            newStreak += 1
                                                        }

                                                        firestore.collection("habits")
                                                            .document(habit.id)
                                                            .update(
                                                                mapOf(
                                                                    "completedDates" to newCompletedDates,
                                                                    "streak" to newStreak
                                                                )
                                                            )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = LocalDate.parse(date).dayOfMonth.toString(),
                                                    fontSize = 12.sp,
                                                    color = if (isCompleted)
                                                        MaterialTheme.colorScheme.onPrimary
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }



                        }

                    }

                }
            }
        }
