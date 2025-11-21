package com.example.habittracker.ui.components

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
    val weeklyProgress = if (weeklyGoal == 0) 0f else (last7Done / weeklyGoal.toFloat()).coerceIn(0f, 1f)

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
                    modifier = Modifier.size(34.dp).background(bg, CircleShape).clickable { onToggleToday() }.padding(6.dp),

                    contentAlignment = Alignment.Center
                ) {
                    if (isCompletedToday) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Kész",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }
                        )
                    }
                }
            }

            // Mini progress csík alul (heti célhoz igazítva)
            LinearProgressIndicator(
                progress = { weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}



private fun lastNDays(n: Int): List<LocalDate> {
    val today = LocalDate.now()
    return (0 until n).map { today.minusDays(it.toLong()) }.reversed()
}
