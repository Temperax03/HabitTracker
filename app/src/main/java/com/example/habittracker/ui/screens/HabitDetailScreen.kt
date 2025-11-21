package com.example.habittracker.ui.screens
import com.example.habittracker.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habittracker.ui.components.HabitTimelineCard
import com.example.habittracker.ui.components.lastNDates
import com.example.habittracker.viewmodel.HabitViewModel
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.habittracker.data.model.ReminderTime
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory),
    onBack: () -> Unit
) {
    val habit = viewModel.habits.firstOrNull { it.id == habitId }
    var name by remember(habit) { mutableStateOf(habit?.name.orEmpty()) }
    var icon by remember(habit) { mutableStateOf(habit?.icon ?: "🔥") }
    var weeklyGoal by remember(habit) { mutableStateOf((habit?.weeklyGoal ?: 5).toFloat()) }
    val reminders = remember(habit) {
        mutableStateListOf<ReminderTime>().apply { addAll(habit?.reminders ?: emptyList()) }
    }
    var selectedDays by remember(habit) { mutableStateOf(setOf<DayOfWeek>()) }
    val context = LocalContext.current
    fun showTimePicker() {
        val now = LocalTime.now()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val timeString = String.format("%02d:%02d", hour, minute)
                reminders.add(ReminderTime(timeString, selectedDays.map { it.value }))
            },
            now.hour,
            now.minute,
            true
        ).show()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szokás részletei") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Vissza") }
                },
                actions = {
                    IconButton(onClick = {
                        habit ?: return@IconButton
                        if (name.isNotBlank()) {
                            viewModel.updateHabitDetails(
                                habit,
                                name.trim(),
                                icon,
                                weeklyGoal.toInt(),
                                reminders.toList()
                            )
                        }
                    }) { Icon(Icons.Outlined.Save, "Mentés") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nem található a szokás.")
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 100) name = it },
                label = { Text("Szokás neve") },
                modifier = Modifier.fillMaxWidth()
            )

            // Ikonválasztó
            Text("Ikon", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val icons = listOf("🔥","✅","💧","📚","🏃‍♂️","🧘","🕗","🥦","☕","🎯")
                icons.forEach { emoji ->
                    val selected = icon == emoji
                    AssistChip(
                        onClick = { icon = emoji },
                        label = { Text(emoji) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Heti cél
            Text("Heti cél: ${weeklyGoal.toInt()} nap / hét", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = weeklyGoal,
                onValueChange = { weeklyGoal = it.coerceIn(0f, 7f) },
                valueRange = 0f..7f,
                steps = 6
            )
            Text("Értesítések", style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(id = R.string.notification_days_hint), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = selectedDays.contains(day)
                    AssistChip(
                        onClick = {
                            selectedDays = if (selected) selectedDays - day else selectedDays + day
                        },
                        label = { Text(day.name.take(3)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            Text(text = stringResource(id = R.string.notification_all_days_hint), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { showTimePicker() }) {
                Text(stringResource(id = R.string.add_notification_time))
            }

            reminders.forEachIndexed { index, reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayLabel = if (reminder.days.isEmpty()) {
                        stringResource(id = R.string.notification_every_day)
                    } else {
                        reminder.days.joinToString(",") { DayOfWeek.of(it).name.take(3) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = reminder.time, style = MaterialTheme.typography.bodyLarge)
                        Text(text = dayLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { reminders.removeAt(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.remove_label))
                    }
                }
            }

            // Heti mini-kártya
            HabitTimelineCard(
                title = "Utolsó 7 nap",
                days = lastNDates(7),
                completed = habit.completedDates
            )

            // Havi mini-kártya
            HabitTimelineCard(
                title = "Utolsó 30 nap",
                days = lastNDates(30),
                completed = habit.completedDates
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.deleteHabit(habit.id); onBack() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Delete, "Törlés")
                Spacer(Modifier.width(8.dp))
                Text("Szokás törlése")
            }
        }
    }
}
