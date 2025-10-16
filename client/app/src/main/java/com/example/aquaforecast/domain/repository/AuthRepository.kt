package com.example.aquaforecast.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Save user's phone number for authentication
     * Phone number serves as the primary user identifier
     *
     * @param phone The phone number to save (should be validated before calling)
     * @return Result.Success with Unit if saved successfully,
     *         Result.Error with message if save failed
     */
    suspend fun savePhone(phone: String): Result<Unit>

    /**
     * Retrieve the saved phone number
     * Used to check authentication status and display user info
     *
     * @return Result.Success with phone number or null if not set,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getPhone(): Result<String?>

    /**
     * Check if user is logged in (has saved phone number)
     * Used to determine app entry point (login vs dashboard)
     *
     * @return Result.Success with true if logged in (phone exists), false otherwise,
     *         Result.Error with message if check failed
     */
    suspend fun isLoggedIn(): Result<Boolean>

    /**
     * Logout user by clearing saved phone number and preferences
     * This removes all user data from local storage
     *
     * @return Result.Success with Unit if logout successful,
     *         Result.Error with message if logout failed
     */
    suspend fun logout(): Result<Unit>

    /**
     * Observe user preferences as a reactive stream
     * Automatically emits new values when preferences change
     * Returns map of preference keys to values
     *
     * Example preferences: theme, notifications_enabled, last_sync_time
     *
     * @return Flow that emits preference map whenever preferences change
     */
    fun preferences(): Flow<Map<String, Any>>
}