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
import com.example.habittracker.data.model.Habit
import com.example.habittracker.ui.components.AddHabitBottomSheet
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.habittracker.ui.components.HabitCard

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
                    Button(
                        onClick = { selectedView = view },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedView == view)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
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
                    HabitCard(habit, selectedView, periodDates, viewModel)
                }
            }
        }
    }
}
