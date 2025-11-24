package com.example.habittracker.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitListScreen(
    navController: NavHostController,
    viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory)
) {
    val habits = viewModel.habits
    val isLoading = viewModel.isLoading
    val analytics = viewModel.analytics
    val errorMessage = viewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    var isSheetOpen by remember { mutableStateOf(false) }

    AddHabitBottomSheet(
        isOpen = isSheetOpen,
        onDismiss = { isSheetOpen = false },
        onSave = { name, icon, weeklyGoal, reminders ->
            viewModel.addHabit(name, icon, weeklyGoal, reminders)
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
                    IconButton(onClick = {
                        navController.navigate("analytics") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("habit_list") {
                                saveState = true
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.Insights, contentDescription = "Elemzések")
                    }
                    IconButton(onClick = { isSheetOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Új szokás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 1. Ág: még tölt, és még nincs adat
                isLoading && habits.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // 2. Ág: nincs egyetlen szokás sem
                habits.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        onAddClick = { isSheetOpen = true }
                    )
                }

                // 3. Ág: van adat
                else -> {
                    var selectedViewMode by rememberSaveable { mutableStateOf(HabitViewMode.Daily) }
                    var searchQuery by rememberSaveable { mutableStateOf("") }
                    var statusFilter by rememberSaveable { mutableStateOf(HabitStatusFilter.All) }
                    var reminderFilter by rememberSaveable { mutableStateOf(ReminderFilter.All) }

                    val today = remember { LocalDate.now().toString() }
                    val filteredHabits = habits.filter { habit ->
                        val matchesSearch = habit.name.contains(searchQuery, ignoreCase = true)
                        val matchesStatus = when (statusFilter) {
                            HabitStatusFilter.All -> true
                            HabitStatusFilter.CompletedToday -> habit.completedDates.contains(today)
                            HabitStatusFilter.PendingToday -> !habit.completedDates.contains(today)
                        }
                        val matchesReminder = when (reminderFilter) {
                            ReminderFilter.All -> true
                            ReminderFilter.WithReminder -> habit.reminders.isNotEmpty()
                            ReminderFilter.WithoutReminder -> habit.reminders.isEmpty()
                        }

                        matchesSearch && matchesStatus && matchesReminder
                    }
                    val listState = rememberLazyListState()
                    var draggingHabitId by remember { mutableStateOf<String?>(null) }
                    var dragOffsetY by remember { mutableStateOf(0f) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TodayOverviewCard(
                            habits = filteredHabits,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        HabitViewModeSelector(
                            selectedMode = selectedViewMode,
                            onModeSelected = { selectedViewMode = it }
                        )

                        Spacer(Modifier.height(8.dp))
                        SearchAndFilterBar(
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            statusFilter = statusFilter,
                            onStatusChange = { statusFilter = it },
                            reminderFilter = reminderFilter,
                            onReminderChange = { reminderFilter = it },
                            onReset = {
                                searchQuery = ""
                                statusFilter = HabitStatusFilter.All
                                reminderFilter = ReminderFilter.All
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredHabits, key = { _, habit -> habit.id }) { _, habit ->
                                val isDragging = draggingHabitId == habit.id
                                val itemModifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        shadowElevation = if (isDragging) 12f else 0f
                                    }
                                    .pointerInput(habit.id, filteredHabits) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingHabitId = habit.id
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingHabitId = null
                                                dragOffsetY = 0f
                                                viewModel.persistHabitOrder()
                                            },
                                            onDragEnd = {
                                                draggingHabitId = null
                                                dragOffsetY = 0f
                                                viewModel.persistHabitOrder()
                                            },
                                            onDrag = { change, dragAmount ->
                                                dragOffsetY += dragAmount.y
                                                val targetIndex =
                                                    listState.findTargetIndex(habit.id, dragOffsetY)
                                                val targetHabitId =
                                                    targetIndex?.let { filteredHabits.getOrNull(it)?.id }
                                                if (targetHabitId != null && targetHabitId != habit.id) {
                                                    viewModel.moveHabit(habit.id, targetHabitId)
                                                }
                                            }
                                        )
                                    }

                                when (selectedViewMode) {
                                    HabitViewMode.Daily -> {
                                        Box(modifier = itemModifier) {
                                            HabitRowCard(
                                                habit = habit,
                                                onClick = {
                                                    navController.navigate("habit_detail/${habit.id}") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onToggleToday = { viewModel.toggleCompletion(habit) }
                                            )
                                        }
                                    }

                                    HabitViewMode.Weekly -> {
                                        Box(modifier = itemModifier) {
                                            WeeklyHabitCard(
                                                habit = habit,
                                                onClick = {
                                                    navController.navigate("habit_detail/${habit.id}") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    HabitViewMode.Monthly -> {
                                        Box(modifier = itemModifier) {
                                            MonthlyHabitCard(
                                                habit = habit,
                                                onClick = {
                                                    navController.navigate("habit_detail/${habit.id}") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading && habits.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun LazyListState.findTargetIndex(itemKey: Any, dragOffset: Float): Int? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val currentItem = visibleItems.find { it.key == itemKey } ?: return null
    val middle = currentItem.offset + dragOffset + (currentItem.size / 2f)
    return visibleItems.minByOrNull { item ->
        abs(middle - (item.offset + item.size / 2f))
    }?.index
}

@Composable
private fun TodayOverviewCard(habits: List<Habit>, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now().toString() }
    val completedCount = habits.count { it.completedDates.contains(today) }
    val completionRatio =
        if (habits.isEmpty()) 0f else (completedCount / habits.size.toFloat()).coerceIn(0f, 1f)
    val completionPercent = (completionRatio * 100).roundToInt()
    val topStreak = habits.maxOfOrNull { it.streak } ?: 0

    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val start = colorScheme.primary
    val end = if (isDark) colorScheme.primaryContainer else colorScheme.secondary

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val gradient = remember(completedCount, start, end) {
            Brush.horizontalGradient(listOf(start, end))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(gradient)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Mai lendület",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completedCount / ${habits.size} szokás kész",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("Cél: ${habits.size}") }
                    )
                }
                LinearProgressIndicator(
                    progress = { completionRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Aktív sorozat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$topStreak nap",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$completionPercent% kész",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    statusFilter: HabitStatusFilter,
    onStatusChange: (HabitStatusFilter) -> Unit,
    reminderFilter: ReminderFilter,
    onReminderChange: (ReminderFilter) -> Unit,
    onReset: () -> Unit
) {
    val hasActiveFilters = searchQuery.isNotBlank() ||
            statusFilter != HabitStatusFilter.All ||
            reminderFilter != ReminderFilter.All
    var isMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Keresés") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Keresés törlése"
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        "Keresés név szerint",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            Box {
                BadgedBox(badge = { if (hasActiveFilters) Badge() }) {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Szűrők",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    Text(
                        text = "Státusz",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DropdownMenuItem(
                        text = { Text("Összes") },
                        onClick = {
                            onStatusChange(HabitStatusFilter.All)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (statusFilter == HabitStatusFilter.All) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ma teljesített") },
                        onClick = {
                            onStatusChange(HabitStatusFilter.CompletedToday)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (statusFilter == HabitStatusFilter.CompletedToday) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Még hátra van mára") },
                        onClick = {
                            onStatusChange(HabitStatusFilter.PendingToday)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (statusFilter == HabitStatusFilter.PendingToday) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )

                    Divider()
                    Text(
                        text = "Emlékeztetők",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DropdownMenuItem(
                        text = { Text("Összes szokás") },
                        onClick = {
                            onReminderChange(ReminderFilter.All)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (reminderFilter == ReminderFilter.All) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Van emlékeztető") },
                        onClick = {
                            onReminderChange(ReminderFilter.WithReminder)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (reminderFilter == ReminderFilter.WithReminder) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nincs emlékeztető") },
                        onClick = {
                            onReminderChange(ReminderFilter.WithoutReminder)
                            isMenuExpanded = false
                        },
                        leadingIcon = {
                            if (reminderFilter == ReminderFilter.WithoutReminder) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }

        if (hasActiveFilters) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Visszaállítás")
            }
        }
    }
}

private enum class HabitViewMode(val label: String, val description: String) {
    Daily(label = "Napi nézet", description = "Napi nézet kiválasztása"),
    Weekly(label = "Heti nézet", description = "Heti nézet kiválasztása"),
    Monthly(label = "Havi nézet", description = "Havi nézet kiválasztása")
}

private enum class HabitStatusFilter { All, CompletedToday, PendingToday }

private enum class ReminderFilter { All, WithReminder, WithoutReminder }

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
    val progress =
        if (denominator == 0) 0f else (completedCount / denominator.toFloat()).coerceIn(0f, 1f)
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
    val progress =
        if (days.isEmpty()) 0f else (completedCount / days.size.toFloat()).coerceIn(0f, 1f)
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Még nincs szokásod", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Kezdd el egy új szokás felvételével.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddClick) { Text("Új szokás hozzáadása") }
    }
}
