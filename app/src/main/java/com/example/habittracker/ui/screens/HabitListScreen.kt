package com.example.habittracker.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.habittracker.ui.components.AddHabitBottomSheet
import com.example.habittracker.ui.components.HabitRowCard
import com.example.habittracker.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    navController: NavHostController,
    viewModel: HabitViewModel = viewModel()
) {
    val habits = viewModel.habits
    var isSheetOpen by remember { mutableStateOf(false) }

    AddHabitBottomSheet(
        isOpen = isSheetOpen,
        onDismiss = { isSheetOpen = false },
        onSave = { name, icon, weeklyGoal ->
            viewModel.addHabit(name, icon, weeklyGoal)
            val prefs = navController.context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("hasHabits", true) }
            isSheetOpen = false
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
        if (habits.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAddClick = { isSheetOpen = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("Még nincs szokásod", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Kezdd el egy új szokás felvételével.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddClick) { Text("Új szokás hozzáadása") }
    }
}
