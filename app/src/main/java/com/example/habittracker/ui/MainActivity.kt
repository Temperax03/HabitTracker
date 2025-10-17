package com.example.habittracker.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.ui.navigation.AppNavigation
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val prefs = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        val hasHabits = prefs.getBoolean("hasHabits", false)

        setContent {
            HabitTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val startDestination = if (hasHabits) "habit_list" else "welcome"
                    AppNavigation(navController, startDestination)
                }
            }
        }
    }
}
