package com.example.habittracker.ui.screens
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habittracker.R
import com.example.habittracker.data.model.ReminderTime
import com.example.habittracker.ui.components.HabitTimelineCard
import com.example.habittracker.ui.components.lastNDates
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import com.example.habittracker.data.model.HabitIcons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory),
    onBack: () -> Unit
) {
    val habit = viewModel.habits.firstOrNull { it.id == habitId }
    var name by remember(habit) { mutableStateOf(habit?.name.orEmpty()) }
    var icon by remember(habit) { mutableStateOf(habit?.icon ?: HabitIcons.default) }
    var weeklyGoal by remember(habit) { mutableFloatStateOf((habit?.weeklyGoal ?: 5).toFloat()) }
    val reminders = remember(habit) {
        mutableStateListOf<ReminderTime>().apply { addAll(habit?.reminders ?: emptyList()) }
    }
    var notes by remember(habit) { mutableStateOf(habit?.notes.orEmpty()) }
    var selectedDays by remember(habit) { mutableStateOf(setOf<DayOfWeek>()) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Vissza") }
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
                                reminders.toList(),
                                notes
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
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 100) name = it },
                label = { Text("Szokás neve") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 250) notes = it },
                label = { Text("Jegyzet") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${notes.length}/250 karakter") },
                minLines = 3,
                maxLines = 5
            )

            // Ikonválasztó
            Text("Ikon", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val icons = HabitIcons.all
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
