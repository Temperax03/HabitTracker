package com.example.habittracker.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.habittracker.data.model.Habit
import com.example.habittracker.ui.components.AddHabitBottomSheet
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.habittracker.ui.components.HabitRowCard


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitListScreen(navController: NavHostController, viewModel: HabitViewModel = viewModel()) {

    val habits = viewModel.habits
    var isSheetOpen by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf("Today") }
    val viewOptions = listOf("Today", "Weekly", "Monthly")

    // 🧩 AddHabitBottomSheet hívás
    AddHabitBottomSheet(
        isOpen = isSheetOpen,
        onDismiss = { isSheetOpen = false },
        onSave = { name ->
            viewModel.addHabit(name)
            val prefs = navController.context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("hasHabits", true) }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szokásaim") },
                actions = {
                    IconButton(onClick = { isSheetOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Új szokás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Nézetváltó gombok (Today / Weekly / Monthly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                viewOptions.forEach { view ->
                    val backgroundColor by animateColorAsState(
                        targetValue = if (selectedView == view)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    Button(
                        onClick = { selectedView = view },
                        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
                    ) {
                        Text(view)
                    }
                }
            }

            // Aktuális időszakhoz tartozó dátumok
            val now = LocalDate.now()
            val periodDates = when (selectedView) {
                "Today" -> listOf(now)
                "Weekly" -> {
                    val startOfWeek = now.with(java.time.DayOfWeek.MONDAY)
                    (0..6).map { startOfWeek.plusDays(it.toLong()) }
                }
                "Monthly" -> {
                    val startOfMonth = now.withDayOfMonth(1)
                    val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                    generateSequence(startOfMonth) { date ->
                        if (date.isBefore(endOfMonth)) date.plusDays(1) else null
                    }.toList()
                }
                else -> listOf(now)
            }.map { it.toString() }

            // Szokáslista megjelenítése
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    HabitRowCard(
                        habit = habit,
                        viewModel = viewModel,
                        onClick = { navController.navigate("habit_detail/${habit.id}") }
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimatedHabitCard(
    habit: Habit,
    selectedView: String,
    periodDates: List<String>,
    viewModel: HabitViewModel
) {
    val today = LocalDate.now().toString()
    val isCompletedToday = habit.completedDates.contains(today)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(habit.name, fontSize = 18.sp)
                    Text("🔥 ${habit.streak}", fontSize = 14.sp)
                }

                IconButton(onClick = { viewModel.deleteHabit(habit.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Törlés")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedView) {
                "Today" -> {
                    Checkbox(
                        checked = isCompletedToday,
                        onCheckedChange = { checked ->
                            val newDates = habit.completedDates.toMutableList()
                            val yesterday = LocalDate.now().minusDays(1).toString()
                            var newStreak = habit.streak

                            if (checked && !isCompletedToday) {
                                newDates.add(today)
                                newStreak = if (habit.completedDates.contains(yesterday)) newStreak + 1 else 1
                            } else if (!checked && isCompletedToday) {
                                newDates.remove(today)
                                newStreak = 0
                            }

                            viewModel.updateHabit(habit, newDates, newStreak)
                        }
                    )
                }

                "Weekly", "Monthly" -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        periodDates.forEach { date ->
                            val isCompleted = habit.completedDates.contains(date)
                            val isPastOrToday = LocalDate.parse(date).isBefore(LocalDate.now().plusDays(1))

                            val bgColor by animateColorAsState(
                                targetValue = if (isCompleted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )

                            val textColor = if (isPastOrToday)
                                if (isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(bgColor, shape = MaterialTheme.shapes.small)
                                    .clickable(enabled = isPastOrToday) {
                                        val newDates = habit.completedDates.toMutableList()
                                        var newStreak = habit.streak
                                        if (isCompleted) {
                                            newDates.remove(date)
                                            if (date <= today) newStreak = calculateStreak(newDates)
                                        } else {
                                            newDates.add(date)
                                            if (date <= today) newStreak = calculateStreak(newDates)
                                        }
                                        viewModel.updateHabit(habit, newDates, newStreak)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(LocalDate.parse(date).dayOfMonth.toString(), fontSize = 12.sp, color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun calculateStreak(completedDates: List<String>): Int {
    if (completedDates.isEmpty()) return 0
    val sorted = completedDates.map { LocalDate.parse(it) }.sorted()
    var streak = 0
    var maxStreak = 0
    var previous: LocalDate? = null
    for (date in sorted) {
        if (previous == null || previous.plusDays(1) == date) {
            streak++
        } else {
            streak = 1
        }
        previous = date
        if (streak > maxStreak) maxStreak = streak
    }
    return maxStreak
}
