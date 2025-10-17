package com.example.habittracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onStartClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Üdvözöl a Szokáskövető!", fontSize = 28.sp, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Kezdd el követni a napi céljaidat és szokásaidat!", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStartClicked) { Text("Kezdjük el!") }
    }
}
