package com.example.habittracker.ui
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        requestNotificationPermission()

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
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

