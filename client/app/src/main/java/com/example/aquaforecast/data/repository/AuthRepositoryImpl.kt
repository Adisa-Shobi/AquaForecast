package com.example.aquaforecast.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of AuthRepository using DataStore
 * Handles user authentication and preferences storage
 */
class AuthRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : AuthRepository {

    companion object {
        private val PHONE_KEY = stringPreferencesKey("user_phone")
    }

    override suspend fun savePhone(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dataStore.edit { preferences ->
                preferences[PHONE_KEY] = phone
            }
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to save phone number").asError()
        }
    }

    override suspend fun getPhone(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val preferences = dataStore.data.first()
            preferences[PHONE_KEY].asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve phone number").asError()
        }
    }

    override suspend fun isLoggedIn(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val preferences = dataStore.data.first()
            val phone = preferences[PHONE_KEY]
            (!phone.isNullOrBlank()).asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to check login status").asError()
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to logout").asError()
        }
    }

    override fun preferences(): Flow<Map<String, Any>> {
        return dataStore.data.map { preferences ->
            preferences.asMap().mapKeys { it.key.name }
        }
    }
}