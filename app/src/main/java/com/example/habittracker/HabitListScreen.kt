@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.habittracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


@Composable
fun HabitListScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    data class Habit(val id: String, val name: String)
    val habits = remember {mutableStateListOf<Habit>()}
    var newHabit by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }
    // Firestore frissito

    DisposableEffect(Unit) {
        var listener: ListenerRegistration? = null

        listener = firestore.collection("habits")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    habits.clear()
                    for (doc in snapshot.documents) {
                        val name = doc.getString("name")
                        val id = doc.id
                        if (name != null) habits.add(Habit(id, name))
                    }
                }
            }

        onDispose { listener?.remove() }
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
                            // Create a map of data to send to Firestore
                            val habitToSave = hashMapOf(
                                "name" to newHabit
                            )

                            // Add the new habit to the "habits" collection
                            firestore.collection("habits")
                                .add(habitToSave)
                                .addOnSuccessListener {
                                    // This part runs if the save is successful
                                    println("Habit saved successfully!")
                                }
                                .addOnFailureListener { e ->
                                    // This part runs if there's an error
                                    println("Error saving habit: $e")
                                }

                            // Reset the input field and close the sheet
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
            items(habits, key = { it.id }) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = habit.name,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )

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
                }

            }
        }
    }
}
