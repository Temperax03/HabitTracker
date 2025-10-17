package com.example.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.data.model.Habit
import com.example.habittracker.ui.screens.HabitViewMode
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HabitRowCard(
    habit: Habit,
    viewModel: HabitViewModel,
    viewMode: HabitViewMode,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val todayKey = today.toString()
    val completedDates = habit.completedDates.toSet()
    val isCompletedToday = completedDates.contains(todayKey)

    // Heti mini-progress a weeklyGoal-hoz igazítva
    val last7 = remember { lastNDays(7) }
    val last7Done = last7.count { completedDates.contains(it.toString()) }
    val weeklyGoal = habit.weeklyGoal.coerceIn(0, 7)
    val weeklyProgress = if (weeklyGoal == 0) 0f else (last7Done / weeklyGoal.toFloat()).coerceIn(0f, 1f)

    val bg by animateColorAsState(
        targetValue = if (isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "check_bg"
    )
    val scale by animateFloatAsState(if (isCompletedToday) 1.05f else 1f, label = "check_scale")

    fun toggleDate(date: LocalDate) {
        if (date.isAfter(today)) return
        val dateKey = date.toString()
        val newDates = habit.completedDates.toMutableList()
        if (newDates.contains(dateKey)) {
            newDates.remove(dateKey)
        } else {
            newDates.add(dateKey)
        }
        newDates.sort()
        val newStreak = calculateStreak(newDates)
        viewModel.updateHabit(habit, newDates, newStreak)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading ikon (emoji körben)
                Box(
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(habit.icon, fontSize = 18.sp) }

                Spacer(Modifier.width(12.dp))

                // Cím + streak chip (kattintva részletek)
                Column(
                    modifier = Modifier.weight(1f).clickable { onClick() },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    AssistChip(onClick = {}, label = { Text("🔥 ${habit.streak}") })
                }

                Spacer(Modifier.width(12.dp))

                // Kerek pipa – mai nap toggle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(bg, CircleShape)
                        .clickable { toggleDate(today) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompletedToday) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Kész",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                        )
                    }
                }
            }

            when (viewMode) {
                HabitViewMode.Daily -> {
                    LinearProgressIndicator(
                        progress = { weeklyProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HabitViewMode.Weekly -> {
                    WeeklyCompletionRow(
                        today = today,
                        completedDates = completedDates,
                        onToggleDate = ::toggleDate
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HabitViewMode.Monthly -> {
                    MonthlyCompletionGrid(
                        today = today,
                        completedDates = completedDates,
                        onToggleDate = ::toggleDate
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyCompletionRow(
    today: LocalDate,
    completedDates: Set<String>,
    onToggleDate: (LocalDate) -> Unit
) {
    val startOfWeek = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val locale = remember { Locale("hu") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DayOfWeek.values().forEach { dayOfWeek ->
            val date = startOfWeek.plusDays(dayOfWeek.ordinal.toLong())
            val isFuture = date.isAfter(today)
            val isChecked = completedDates.contains(date.toString())
            WeeklyDayToggle(
                modifier = Modifier.weight(1f),
                dayLabel = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                date = date,
                isChecked = isChecked,
                enabled = !isFuture,
                locale = locale,
                onToggleDate = onToggleDate
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyDayToggle(
    modifier: Modifier = Modifier,
    dayLabel: String,
    date: LocalDate,
    isChecked: Boolean,
    enabled: Boolean,
    locale: Locale,
    onToggleDate: (LocalDate) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayLabel.uppercase(locale), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { onToggleDate(date) },
            enabled = enabled,
            shape = CircleShape,
            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = if (isChecked) 4.dp else 0.dp
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MonthlyCompletionGrid(
    today: LocalDate,
    completedDates: Set<String>,
    onToggleDate: (LocalDate) -> Unit
) {
    val locale = remember { Locale("hu") }
    val currentMonth = remember(today) { YearMonth.from(today) }
    val firstDay = remember(currentMonth) { currentMonth.atDay(1) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val leadingEmpty = firstDay.dayOfWeek.ordinal

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = buildString {
                append(currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() })
                append(" ")
                append(currentMonth.year)
            },
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayOfWeek.values().forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val weeks = remember(currentMonth) {
            buildMonthMatrix(currentMonth, leadingEmpty, daysInMonth)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            MonthDayToggle(
                                modifier = Modifier.weight(1f),
                                date = date,
                                isChecked = completedDates.contains(date.toString()),
                                enabled = !date.isAfter(today),
                                onToggleDate = onToggleDate
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDayToggle(
    modifier: Modifier,
    date: LocalDate,
    isChecked: Boolean,
    enabled: Boolean,
    onToggleDate: (LocalDate) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = { onToggleDate(date) },
            enabled = enabled,
            shape = CircleShape,
            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = if (isChecked) 4.dp else 0.dp
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun buildMonthMatrix(
    currentMonth: YearMonth,
    leadingEmpty: Int,
    daysInMonth: Int
): List<List<LocalDate?>> {
    val weeks = mutableListOf<List<LocalDate?>>()
    val buffer = mutableListOf<LocalDate?>()
    repeat(leadingEmpty.coerceAtLeast(0)) { buffer.add(null) }
    for (day in 1..daysInMonth) {
        buffer.add(currentMonth.atDay(day))
        if (buffer.size == 7) {
            weeks.add(buffer.toList())
            buffer.clear()
        }
    }
    if (buffer.isNotEmpty()) {
        while (buffer.size < 7) {
            buffer.add(null)
        }
        weeks.add(buffer.toList())
    }
    return weeks
}

private fun calculateStreak(completedDates: List<String>): Int {
    val today = LocalDate.now()
    val completed = completedDates.toSet()
    var streak = 0
    var cursor = today
    while (completed.contains(cursor.toString())) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun lastNDays(n: Int): List<LocalDate> {
    val today = LocalDate.now()
    return (0 until n).map { today.minusDays(it.toLong()) }.reversed()
}
