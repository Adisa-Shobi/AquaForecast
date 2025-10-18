package com.example.aquaforecast.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a user
 */
@Serializable
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)