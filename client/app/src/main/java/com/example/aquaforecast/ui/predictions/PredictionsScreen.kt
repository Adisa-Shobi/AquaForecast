package com.example.aquaforecast.ui.predictions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

private const val TAG = "PredictionsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Predictions") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Predictions - Coming Soon")
        }
    }
}