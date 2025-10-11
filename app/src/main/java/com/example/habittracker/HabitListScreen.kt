@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.habittracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@Composable
fun HabitListScreen(navController: NavHostController) {
    val habits = remember { mutableStateListOf("Tanulás", "Ivás", "Mozgás", "Olvasás") }
    var newHabit by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

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
                            habits.add(newHabit)
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

    // A fő lista felület
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szokásaim") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Vissza"
                        )
                    }
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(habits) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(
                        text = habit,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
