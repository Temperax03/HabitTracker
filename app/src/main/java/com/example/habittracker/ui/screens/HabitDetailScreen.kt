package com.example.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitViewModel = viewModel(),
    onBack: () -> Unit
) {
    val habit = viewModel.habits.firstOrNull { it.id == habitId }
    var name by remember(habit) { mutableStateOf(habit?.name.orEmpty()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szokás részletei") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Vissza")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (habit != null && name.isNotBlank()) {
                                viewModel.updateHabitName(habit.id, name.trim())
                                onBack()
                            }
                        }
                    ) { Icon(Icons.Outlined.Save, contentDescription = "Mentés") }
                }
            )
        }
    ) { padding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nem található a szokás.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // Heti mini-kártya
            SummaryMiniCard(
                title = "Utolsó 7 nap",
                days = lastNDates(7),
                completed = habit.completedDates
            )

            // Havi mini-kártya
            SummaryMiniCard(
                title = "Utolsó 30 nap",
                days = lastNDates(30),
                completed = habit.completedDates
            )

            Spacer(Modifier.height(8.dp))

            // Törlés gomb (itt van a kukázás)
            OutlinedButton(
                onClick = {
                    viewModel.deleteHabit(habit.id)
                    onBack()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Törlés")
                Spacer(Modifier.width(8.dp))
                Text("Szokás törlése")
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    days: List<LocalDate>,
    completed: List<String>
) {
    // progress arány
    val doneCount = days.count { completed.contains(it.toString()) }
    val progress = (doneCount.toFloat() / days.size).coerceIn(0f, 1f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            // Kis rács: napok
            DayGrid(days = days, completed = completed)
        }
    }
}

@Composable
private fun DayGrid(
    days: List<LocalDate>,
    completed: List<String>
) {
    // 7 oszlop (heti ritmusra áll jól)
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false
    ) {
        items(days) { date ->
            val isFuture = date.isAfter(LocalDate.now())
            val isDone = completed.contains(date.toString())
            val boxColor =
                when {
                    isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    isDone -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            val textColor =
                if (isDone) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(boxColor)
                    .then(
                        // részletekben nem modosítunk itt; csak jelzés
                        Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = textColor
                )
            }
        }
    }
}

/** ma is beleértve: utolsó N nap */
private fun lastNDates(n: Int): List<LocalDate> {
    val today = LocalDate.now()
    return (0 until n).map { today.minusDays(it.toLong()) }.reversed()
}
