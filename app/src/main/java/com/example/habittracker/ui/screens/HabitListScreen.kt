package com.example.habittracker.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.habittracker.data.model.Habit
import com.example.habittracker.ui.components.AddHabitBottomSheet
import com.example.habittracker.ui.components.HabitRowCard
import com.example.habittracker.ui.components.HabitTimelineGrid
import com.example.habittracker.ui.components.lastNDates
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate

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
            var selectedViewMode by rememberSaveable { mutableStateOf(HabitViewMode.Daily) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HabitViewModeSelector(
                    selectedMode = selectedViewMode,
                    onModeSelected = { selectedViewMode = it }
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        when (selectedViewMode) {
                            HabitViewMode.Daily -> {
                                HabitRowCard(
                                    habit = habit,
                                    viewModel = viewModel,
                                    onClick = { navController.navigate("habit_detail/${habit.id}") }
                                )
                            }
                            HabitViewMode.Weekly -> {
                                WeeklyHabitCard(
                                    habit = habit,
                                    onClick = { navController.navigate("habit_detail/${habit.id}") }
                                )
                            }
                            HabitViewMode.Monthly -> {
                                MonthlyHabitCard(
                                    habit = habit,
                                    onClick = { navController.navigate("habit_detail/${habit.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class HabitViewMode(val label: String, val description: String) {
    Daily(label = "Napi nézet", description = "Napi nézet kiválasztása"),
    Weekly(label = "Heti nézet", description = "Heti nézet kiválasztása"),
    Monthly(label = "Havi nézet", description = "Havi nézet kiválasztása")
}

@Composable
private fun HabitViewModeSelector(
    selectedMode: HabitViewMode,
    onModeSelected: (HabitViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HabitViewModeButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Today,
            mode = HabitViewMode.Daily,
            isSelected = selectedMode == HabitViewMode.Daily,
            onClick = { onModeSelected(HabitViewMode.Daily) }
        )
        HabitViewModeButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CalendarViewWeek,
            mode = HabitViewMode.Weekly,
            isSelected = selectedMode == HabitViewMode.Weekly,
            onClick = { onModeSelected(HabitViewMode.Weekly) }
        )
        HabitViewModeButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CalendarMonth,
            mode = HabitViewMode.Monthly,
            isSelected = selectedMode == HabitViewMode.Monthly,
            onClick = { onModeSelected(HabitViewMode.Monthly) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitViewModeButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    mode: HabitViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "HabitViewModeButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "HabitViewModeButtonContent"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (isSelected) 6.dp else 1.dp,
        color = containerColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.description,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun WeeklyHabitCard(
    habit: Habit,
    onClick: () -> Unit
) {
    val days = lastNDates(7)
    val completedCount = countCompletedInRange(habit, days)
    val goal = habit.weeklyGoal.coerceIn(0, 7)
    val denominator = if (goal > 0) goal else days.size
    val progress = if (denominator == 0) 0f else (completedCount / denominator.toFloat()).coerceIn(0f, 1f)
    val summaryText = if (goal > 0) {
        "$completedCount / $goal nap a kitűzött célból"
    } else {
        "$completedCount nap megjelölve a ${days.size} napból"
    }

    HabitSummaryTimelineCard(
        habit = habit,
        title = "Heti előrehaladás",
        rangeLabel = "Utolsó 7 nap",
        summaryText = summaryText,
        progress = progress,
        days = days,
        onClick = onClick
    )
}

@Composable
private fun MonthlyHabitCard(
    habit: Habit,
    onClick: () -> Unit
) {
    val days = lastNDates(30)
    val completedCount = countCompletedInRange(habit, days)
    val progress = if (days.isEmpty()) 0f else (completedCount / days.size.toFloat()).coerceIn(0f, 1f)
    val summaryText = "$completedCount nap megjelölve a ${days.size} napból"

    HabitSummaryTimelineCard(
        habit = habit,
        title = "Havi összegzés",
        rangeLabel = "Utolsó 30 nap",
        summaryText = summaryText,
        progress = progress,
        days = days,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitSummaryTimelineCard(
    habit: Habit,
    title: String,
    rangeLabel: String,
    summaryText: String,
    progress: Float,
    days: List<LocalDate>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = habit.icon, fontSize = 20.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Részletek megnyitása",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HabitTimelineGrid(
                days = days,
                completed = habit.completedDates,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun countCompletedInRange(habit: Habit, days: List<LocalDate>): Int =
    days.count { habit.completedDates.contains(it.toString()) }

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
