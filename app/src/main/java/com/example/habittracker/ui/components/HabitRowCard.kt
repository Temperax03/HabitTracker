package com.example.habittracker.ui.components

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

import java.time.LocalDate

@Composable
fun HabitRowCard(
    habit: Habit,
    onClick: () -> Unit,
    onToggleToday: () -> Unit
) {
    val today = LocalDate.now().toString()
    val isCompletedToday = remember(habit.completedDates) { habit.completedDates.contains(today) }

    // Heti mini-progress a weeklyGoal-hoz igazítva
    val last7 = remember { lastNDays(7) }
    val last7Done = last7.count { habit.completedDates.contains(it.toString()) }
    val weeklyGoal = habit.weeklyGoal.coerceIn(0, 7)
    val weeklyProgress =
        if (weeklyGoal == 0) 0f else (last7Done / weeklyGoal.toFloat()).coerceIn(0f, 1f)

    val bg by animateColorAsState(
        targetValue = if (isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "check_bg"
    )
    val scale by animateFloatAsState(if (isCompletedToday) 1.05f else 1f, label = "check_scale")

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() },
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(habit.icon, fontSize = 20.sp)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                StatBadge(label = "🔥 ${habit.streak}")
                                StatBadge(label = "🎯 ${habit.weeklyGoal}x / hét")
                            }

                            if (!habit.notes.isNullOrBlank()) {

                                Text(
                                    text = habit.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                }

                Spacer(Modifier.width(12.dp))


                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onToggleToday() },
                    shape = CircleShape,
                    color = bg,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompletedToday) {
                            androidx.compose.material3.Icon(
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
            }


            LinearProgressIndicator(
                progress = { weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
@Composable
private fun StatBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}




private fun lastNDays(n: Int): List<LocalDate> {
    val today = LocalDate.now()
    return (0 until n).map { today.minusDays(it.toLong()) }.reversed()
}
