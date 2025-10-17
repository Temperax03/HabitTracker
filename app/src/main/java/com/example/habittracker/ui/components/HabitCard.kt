
//
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import java.time.LocalDate
//import androidx.compose.foundation.layout.ExperimentalLayoutApi
//import androidx.compose.foundation.layout.FlowRow
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import com.example.habittracker.data.model.Habit
//import com.example.habittracker.viewmodel.HabitViewModel
//
//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//fun AnimatedHabitCard(
//    habit: Habit,
//    selectedView: String,
//    periodDates: List<String>,
//    viewModel: HabitViewModel
//) {
//    val today = LocalDate.now().toString()
//    val isCompletedToday = habit.completedDates.contains(today)
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        elevation = CardDefaults.cardElevation(4.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text(habit.name, fontSize = 18.sp)
//                    Text("🔥 ${habit.streak}", fontSize = 14.sp)
//                }
//
//                IconButton(onClick = { viewModel.deleteHabit(habit.id) }) {
//                    Icon(Icons.Default.Delete, contentDescription = "Törlés")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            when (selectedView) {
//                "Today" -> {
//                    Checkbox(
//                        checked = isCompletedToday,
//                        onCheckedChange = { checked ->
//                            val newDates = habit.completedDates.toMutableList()
//                            val yesterday = LocalDate.now().minusDays(1).toString()
//                            var newStreak = habit.streak
//
//                            if (checked && !isCompletedToday) {
//                                newDates.add(today)
//                                newStreak = if (habit.completedDates.contains(yesterday)) newStreak + 1 else 1
//                            } else if (!checked && isCompletedToday) {
//                                newDates.remove(today)
//                                newStreak = 0
//                            }
//
//                            viewModel.updateHabit(habit, newDates, newStreak)
//                        }
//                    )
//                }
//
//                "Weekly", "Monthly" -> {
//                    FlowRow(
//                        horizontalArrangement = Arrangement.spacedBy(6.dp),
//                        verticalArrangement = Arrangement.spacedBy(6.dp)
//                    ) {
//                        periodDates.forEach { date ->
//                            val isCompleted = habit.completedDates.contains(date)
//                            val isPastOrToday = LocalDate.parse(date).isBefore(LocalDate.now().plusDays(1))
//                            Box(
//                                modifier = Modifier
//                                    .size(28.dp)
//                                    .background(
//                                        if (isCompleted)
//                                            MaterialTheme.colorScheme.primary
//                                        else
//                                            MaterialTheme.colorScheme.surfaceVariant,
//                                        shape = MaterialTheme.shapes.small
//                                    )
//                                    .clickable(enabled = isPastOrToday) {
//                                        val newDates = habit.completedDates.toMutableList()
//                                        var newStreak = habit.streak
//
//                                        if (isCompleted) {
//                                            newDates.remove(date)
//                                            // streak reset, de csak visszamenőleg
//                                            if (date <= today) newStreak = calculateStreak(newDates)
//                                        } else {
//                                            newDates.add(date)
//                                            // csak múltbéli napra növel
//                                            if (date <= today) newStreak = calculateStreak(newDates)
//                                        }
//
//                                        viewModel.updateHabit(habit, newDates, newStreak)
//                                    },
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    LocalDate.parse(date).dayOfMonth.toString(),
//                                    fontSize = 12.sp,
//                                    color = if (isPastOrToday)
//                                        if (isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
//                                    else
//                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//// Segédfüggvény streak számolásra
//fun calculateStreak(completedDates: List<String>): Int {
//    if (completedDates.isEmpty()) return 0
//    val sorted = completedDates.map { LocalDate.parse(it) }.sorted()
//    var streak = 0
//    var maxStreak = 0
//    var previous: LocalDate? = null
//    for (date in sorted) {
//        if (previous == null || previous.plusDays(1) == date) {
//            streak++
//        } else {
//            streak = 1
//        }
//        previous = date
//        if (streak > maxStreak) maxStreak = streak
//    }
//    return maxStreak
//}
