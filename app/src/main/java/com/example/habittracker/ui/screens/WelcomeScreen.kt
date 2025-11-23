package com.example.habittracker.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.habittracker.data.model.ReminderTime
import com.example.habittracker.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import com.example.habittracker.viewmodel.HabitViewModel.HabitInput

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

private data class PresetHabit(
    val name: String,
    val icon: String,
    val weeklyGoal: Int,
    val reminderTime: ReminderTime? = null,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)

@Composable

fun OnboardingScreen(
    navController: NavHostController,
    viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory)
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val presetHabits = remember {
        listOf(
            PresetHabit(
                name = "Reggeli jóga",
                icon = "🧘",
                weeklyGoal = 4,
                reminderTime = ReminderTime(hour = 7, minute = 30),
                description = "Rövid nyújtás a nap indításához"
            ),
            PresetHabit(
                name = "Vízivás",
                icon = "💧",
                weeklyGoal = 7,
                reminderTime = ReminderTime(hour = 10, minute = 0),
                description = "Legalább 8 pohár naponta"
            ),
            PresetHabit(
                name = "Séta ebéd után",
                icon = "🚶",
                weeklyGoal = 5,
                reminderTime = ReminderTime(hour = 13, minute = 0),
                description = "Rövid 15 perces séta"
            )
        )
    }
    val selectedHabitNames = remember { mutableStateListOf<String>() }

    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Üdv a Szokáskövetőben!",
                description = "Állíts be célokat és figyeld a fejlődésed gyönyörű statisztikákkal.",
                icon = { Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ),
            OnboardingPage(
                title = "Emlékeztetők, hogy úton maradj",
                description = "Okos értesítések segítenek, hogy ne felejtsd el a napi rutinjaidat.",
                icon = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ),
            OnboardingPage(
                title = "Engedélyezd az értesítéseket",
                description = "Kérjük, kapcsold be az értesítéseket, hogy időben jelezhessünk.",
                icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ),
            OnboardingPage(
                title = "Válassz ki induló szokásokat",
                description = "Kezdd a legnépszerűbb rutinokkal vagy adj hozzá sajátot később.",
                icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        )
    }

    var notificationsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = granted
    }

    LaunchedEffect(Unit) {
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Szokáskövető",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (pagerState.currentPage < pages.lastIndex) {
                    Text(
                        text = "Kihagyás",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            scope.launch { pagerState.scrollToPage(pages.lastIndex) }
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0, 1 -> FeaturePageContent(page = pages[page])
                    2 -> NotificationPage(
                        page = pages[page],
                        notificationsGranted = notificationsGranted,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    else -> PresetHabitPage(
                        page = pages[page],
                        presets = presetHabits,
                        selectedHabitNames = selectedHabitNames,
                        onToggle = { habitName ->
                            if (selectedHabitNames.contains(habitName)) {
                                selectedHabitNames.remove(habitName)
                            } else {
                                selectedHabitNames.add(habitName)
                            }
                        }
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DotsIndicator(totalDots = pages.size, selectedIndex = pagerState.currentPage)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        Text(
                            text = "Vissza",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    val isLastPage = pagerState.currentPage == pages.lastIndex
                    Button(
                        onClick = {
                            if (isLastPage) {
                                scope.launch {
                                    val selectedPresets = presetHabits.filter { selectedHabitNames.contains(it.name) }
                                    val habitInputs = selectedPresets.map { preset ->
                                        HabitInput(
                                            name = preset.name,
                                            icon = preset.icon,
                                            weeklyGoal = preset.weeklyGoal,
                                            reminders = preset.reminderTime?.let { listOf(it) } ?: emptyList()
                                        )
                                    }
                                    viewModel.addHabitsBlocking(habitInputs)
                                    context.getSharedPreferences("HabitPrefs", android.content.Context.MODE_PRIVATE).edit {
                                        putBoolean("onboarding_complete", true)
                                        putBoolean("hasHabits", selectedPresets.isNotEmpty())
                                    }
                                    navController.navigate("habit_list") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = if (isLastPage) "Kezdjük!" else "Tovább", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePageContent(page: OnboardingPage) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingIcon(modifier = Modifier.height(96.dp), content = page.icon)
        Spacer(Modifier.height(16.dp))
        Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationPage(page: OnboardingPage, notificationsGranted: Boolean, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingIcon(modifier = Modifier.height(96.dp), content = page.icon)
        Spacer(Modifier.height(16.dp))
        Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        if (!notificationsGranted) {
            Button(onClick = onRequestPermission) {
                Text("Engedély megadása")
            }
        } else {
            Text(
                text = "Az értesítések engedélyezve vannak!",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PresetHabitPage(
    page: OnboardingPage,
    presets: List<PresetHabit>,
    selectedHabitNames: List<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        presets.forEach { preset ->
            val isSelected = selectedHabitNames.contains(preset.name)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onToggle(preset.name) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${preset.icon} ${preset.name}", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        preset.reminderTime?.let { reminder ->
                            Text(
                                text = "Emlékeztető: ${reminder.hour.toString().padStart(2, '0')}:${reminder.minute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelected) "✔" else "",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DotsIndicator(totalDots: Int, selectedIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun OnboardingIcon(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
