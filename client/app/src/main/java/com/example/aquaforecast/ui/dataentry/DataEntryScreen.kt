package com.example.aquaforecast.ui.dataentry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEntryScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Data Entry") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Data Entry - Coming Soon")
        }
    }
}