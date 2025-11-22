package com.example.habittracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.habittracker.ui.screens.HabitDetailScreen
import com.example.habittracker.ui.screens.HabitListScreen
import com.example.habittracker.ui.screens.WelcomeScreen
import com.example.habittracker.ui.screens.AnalyticsScreen
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
        composable("analytics") {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "habit_detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) { backStack ->
            val habitId = backStack.arguments?.getString("habitId") ?: return@composable
            HabitDetailScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
