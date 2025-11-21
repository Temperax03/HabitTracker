package com.example.habittracker.ui.components


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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, weeklyGoal: Int, notificationTime: String?) -> Unit,
    initialText: String = "",
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var error by remember { mutableStateOf<String?>(null) }


    val icons = listOf("🔥","✅","💧","📚","🏃‍♂️","🧘","🕗","🥦","☕","🎯")
    var selectedIcon by remember { mutableStateOf("🔥") }
    var weeklyGoal by remember { mutableStateOf(5f) }
    val notificationOptions = remember {
        listOf(
            context.getString(R.string.notification_option_none),
            "08:00",
            "12:00",
            "18:00",
            "20:00",
            "21:00"
        )
    }
    var selectedNotification by remember { mutableStateOf(notificationOptions[3]) }
    var isNotificationExpanded by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(12.dp))
            Text(text = stringResource(id = R.string.notification_time_label), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = isNotificationExpanded,
                onExpandedChange = { isNotificationExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = selectedNotification,
                    onValueChange = { },
                    label = { Text(stringResource(id = R.string.notification_time_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNotificationExpanded) },
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = isNotificationExpanded,
                    onDismissRequest = { isNotificationExpanded = false }
                ) {
                    notificationOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedNotification = option
                                isNotificationExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
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
                                val notificationTime = selectedNotification.takeIf {
                                    it != context.getString(R.string.notification_option_none)
                                }
                                onSave(name, selectedIcon, weeklyGoal.toInt(), notificationTime)
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
