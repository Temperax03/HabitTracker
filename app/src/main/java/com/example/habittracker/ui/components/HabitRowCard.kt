package com.example.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.data.model.Habit
import com.example.habittracker.viewmodel.HabitViewModel
import java.time.LocalDate

@Composable
fun HabitRowCard(
    habit: Habit,
    viewModel: HabitViewModel,
    onClick: () -> Unit, // részletek megnyitása
) {
    val today = LocalDate.now().toString()
    val isCompletedToday = remember(habit.completedDates) { habit.completedDates.contains(today) }

    // Animációk: pipa háttere és mérete
    val bg by animateColorAsState(
        targetValue = if (isCompletedToday)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        label = "check_bg"
    )
    val scale by animateFloatAsState(targetValue = if (isCompletedToday) 1.05f else 1f, label = "check_scale")

    // Utolsó 7 napos arány a kis progress-hez
    val last7 = remember(habit.completedDates) { lastNDays(7) }
    val last7Progress = remember(habit.completedDates) {
        val done = last7.count { habit.completedDates.contains(it.toString()) }
        done / 7f
    }

    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bal: cím + streak chip
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = onClick,              // részletek
                            onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } // finom haptika
                        ),
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
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text(text = "🔥 ${habit.streak}") }
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Jobb: kerek pipa — CSAK a mai napot kapcsolja
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(bg, CircleShape)
                        .combinedClickable(
                            onClick = {
                                val newDates = habit.completedDates.toMutableList()
                                var newStreak = habit.streak
                                val yesterday = LocalDate.now().minusDays(1).toString()

                                if (!isCompletedToday) {
                                    newDates.add(today)
                                    newStreak = if (habit.completedDates.contains(yesterday)) newStreak + 1 else 1
                                } else {
                                    newDates.remove(today)
                                    newStreak = 0
                                }
                                viewModel.updateHabit(habit, newDates, newStreak)
                            },
                            onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                        )
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

            // Vékony, minimalista progress csík alul (utolsó 7 nap)
            LinearProgressIndicator(
                progress = { last7Progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Az utolsó N nap LocalDate listája (ma is benne). */
private fun lastNDays(n: Int): List<LocalDate> {
    val today = LocalDate.now()
    return (0 until n).map { today.minusDays(it.toLong()) }.reversed()
}
