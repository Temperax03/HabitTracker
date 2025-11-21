package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, weeklyGoal: Int) -> Unit,
    initialText: String = ""
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // ÚJ: ikonok + heti cél
    val icons = listOf("🔥","✅","💧","📚","🏃‍♂️","🧘","🕗","🥦","☕","🎯")
    var selectedIcon by remember { mutableStateOf("🔥") }
    var weeklyGoal by remember { mutableStateOf(5f) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                onSave(name, selectedIcon, weeklyGoal.toInt())
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    text = ""
                                    error = null
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
