package com.example.aquaforecast.domain.repository

import android.content.Context
import com.example.aquaforecast.domain.model.User
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Firebase authentication
 * Supports email/password and Google Sign-In
 */
interface AuthRepository {

    /**
     * Login with email and password
     */
    suspend fun loginWithEmail(email: String, password: String): Result<User>

    /**
     * Register new user with email and password
     */
    suspend fun registerWithEmail(email: String, password: String): Result<User>

    /**
     * Login with Google ID token
     */
    suspend fun signInWithGoogle(context: Context): Result<User>

    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): Result<User?>

    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<User?>

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Result<Boolean>

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}