package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    initialText: String = ""
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val scale by animateFloatAsState(targetValue = if (isOpen) 1f else 0.95f)

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(text = "Új szokás hozzáadása", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.length <= 100) {
                        text = it
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Szokás neve") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus(force = true) }
                )
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }
                            .invokeOnCompletion { onDismiss() }
                    }
                ) {
                    Text("Mégse")
                }

                Button(
                    onClick = {
                        val name = text.trim()
                        when {
                            name.isEmpty() -> error = "A név nem lehet üres."
                            name.length < 2 -> error = "Legalább 2 karakter."
                            else -> {
                                onSave(name)
                                coroutineScope.launch { sheetState.hide() }
                                    .invokeOnCompletion {
                                        text = ""
                                        error = null
                                        onDismiss()
                                    }
                            }
                        }
                    }
                ) {
                    Text("Mentés")
                }
            }
        }
    }
}
