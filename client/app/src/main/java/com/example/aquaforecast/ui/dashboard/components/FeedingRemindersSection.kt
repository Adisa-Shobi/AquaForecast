package com.example.aquaforecast.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.IconCard
import com.example.aquaforecast.ui.components.SectionHeader

@Composable
fun FeedingRemindersSection(
    feedingReminders: List<FeedingReminder>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Feeding Reminders")

        Spacer(modifier = Modifier.height(16.dp))

        feedingReminders.forEach { reminder ->
            IconCard(
                icon = Icons.Default.Restaurant,
                title = reminder.pondName,
                subtitle = reminder.time,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

data class FeedingReminder(
    val pondName: String,
    val time: String
)
