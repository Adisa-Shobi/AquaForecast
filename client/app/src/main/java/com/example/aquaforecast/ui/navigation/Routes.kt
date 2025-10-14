package com.example.aquaforecast.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute

@Serializable
data class PredictionScreenRoute(
    val name: String,
    val age: Int
)