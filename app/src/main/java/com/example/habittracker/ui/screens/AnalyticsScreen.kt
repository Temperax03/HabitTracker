package com.example.habittracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habittracker.ui.components.HabitTimelineGrid
import com.example.habittracker.ui.components.lastNDates
import com.example.habittracker.viewmodel.HabitAttention
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory),
    onBack: () -> Unit
) {
    val analytics = viewModel.analytics
    val habits = viewModel.habits
    val scrollState = rememberScrollState()

    val aggregatedDays = lastNDates(14)
    val aggregatedCompleted = aggregatedDays
        .filter { day -> habits.any { it.completedDates.contains(day.toString()) } }
        .map { it.toString() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Elemzések") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Vissza")
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightSummaryCard(
                total7Days = analytics.totalCheckinsLast7Days,
                total30Days = analytics.totalCheckinsLast30Days
            )

            ProgressOverviewCard(
                weeklyProgress = analytics.averageWeeklyGoalProgress,
                monthlyProgress = analytics.averageMonthlyCompletion
            )

            StreakHighlightCard(
                longestStreak = analytics.longestStreak,
                streakHabits = analytics.longestStreakHabits.map { it.name }
            )

            TrendTimelineCard(
                days = aggregatedDays,
                completed = aggregatedCompleted
            )

            AttentionListCard(analytics.habitsNeedingAttention)
        }
    }
}

@Composable
private fun InsightSummaryCard(total7Days: Int, total30Days: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Összesített bejelentkezések", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBlock(label = "Utolsó 7 nap", value = "$total7Days")
                StatBlock(label = "Utolsó 30 nap", value = "$total30Days")
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun ProgressOverviewCard(weeklyProgress: Float, monthlyProgress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Átlagos teljesítés", style = MaterialTheme.typography.titleMedium)
            ProgressRow(label = "Heti célok", progress = weeklyProgress)
            ProgressRow(label = "Havi kitartás", progress = monthlyProgress)
        }
    }
}

@Composable
private fun ProgressRow(label: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StreakHighlightCard(longestStreak: Int, streakHabits: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Leghosszabb széria", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (longestStreak > 0) "$longestStreak nap" else "Nincs még kialakult széria",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (streakHabits.isNotEmpty()) {
                Text("Érintett szokások", style = MaterialTheme.typography.labelMedium)
                streakHabits.forEach { name ->
                    Text("• $name", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TrendTimelineCard(days: List<LocalDate>, completed: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Haladási trend", style = MaterialTheme.typography.titleMedium)
            Text("Közös naptár: utolsó 14 nap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HabitTimelineGrid(days = days, completed = completed, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AttentionListCard(attention: List<HabitAttention>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Figyelmet igényel", style = MaterialTheme.typography.titleMedium)
            if (attention.isEmpty()) {
                Text(
                    text = "Minden szokás jól halad!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                attention.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${item.habit.icon} ${item.habit.name}", style = MaterialTheme.typography.bodyLarge)
                            Text(text = "${(item.weeklyProgress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        }
                        LinearProgressIndicator(
                            progress = { item.weeklyProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}