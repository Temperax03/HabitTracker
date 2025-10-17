package com.example.habittracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.habittracker.ui.screens.HabitListScreen
import com.example.habittracker.ui.screens.WelcomeScreen

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("welcome") {
            WelcomeScreen(onStartClicked = {
                navController.navigate("habit_list") {
                    popUpTo("welcome") { inclusive = true }
                }
            })
        }
        composable("habit_list") {
            HabitListScreen(navController = navController)
        }
    }
}
