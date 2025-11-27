package com.example.habittracker.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.habittracker.data.model.HabitIcons
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.habittracker.R
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf
import com.example.habittracker.data.model.ReminderTime
import java.time.DayOfWeek
import java.time.LocalTime
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, weeklyGoal: Int, reminders: List<ReminderTime>) -> Unit,
    initialText: String = "",
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var error by remember { mutableStateOf<String?>(null) }


    val icons = HabitIcons.all
    var selectedIcon by remember { mutableStateOf(HabitIcons.default) }
    var weeklyGoal by remember { mutableStateOf(5f) }
    val reminders = remember { mutableStateListOf<ReminderTime>() }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

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


    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text("Új szokás hozzáadása", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.length <= 100) { text = it; error = null }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Szokás neve") },
                singleLine = true
            )

            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            Text("Ikon", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                icons.forEach { emoji ->
                    val selected = selectedIcon == emoji
                    AssistChip(
                        onClick = { selectedIcon = emoji },
                        label = { Text(emoji) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Heti cél: ${weeklyGoal.toInt()} nap / hét", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = weeklyGoal,
                onValueChange = { weeklyGoal = it.coerceIn(0f, 7f) },
                valueRange = 0f..7f,
                steps = 6
            )
            Spacer(Modifier.height(12.dp))
            Text(text = stringResource(id = R.string.notification_time_label), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = stringResource(id = R.string.notification_days_hint), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.notification_all_days_hint), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showTimePicker() }) {
                Text(text = stringResource(id = R.string.add_notification_time))
            }

            reminders.forEachIndexed { index, reminder ->
                Spacer(Modifier.height(8.dp))
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
                        androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.remove_label))
                    }
                }
            }



            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                OutlinedButton(
                    onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
                ) { Text("Mégse") }

                Button(
                    onClick = {
                        val name = text.trim()
                        when {
                            name.isEmpty() -> error = "A név nem lehet üres."
                            name.length < 2 -> error = "Legalább 2 karakter."
                            weeklyGoal < 1f -> error = "A heti cél legyen legalább 1 nap."
                            else -> {
                                onSave(name, selectedIcon, weeklyGoal.toInt(), reminders.toList())
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    text = ""
                                    error = null
                                    reminders.clear()
                                    selectedDays = emptySet()
                                    selectedIcon = HabitIcons.default
                                    onDismiss()
                                }
                            }
                        }
                    }
                ) { Text("Mentés") }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
