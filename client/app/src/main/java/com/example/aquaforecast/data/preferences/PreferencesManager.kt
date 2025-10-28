package com.example.aquaforecast.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages app preferences using DataStore
 */
class PreferencesManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val FORECAST_HORIZON_KEY = intPreferencesKey("forecast_horizon_days")
        private val OFFLINE_MODE_KEY = booleanPreferencesKey("offline_mode")
        const val DEFAULT_FORECAST_HORIZON = 20 // Default: predict 20 days into the future
    }

    /**
     * Get the forecast horizon (number of days to predict into the future)
     */
    val forecastHorizon: Flow<Int> = dataStore.data.map { preferences ->
        preferences[FORECAST_HORIZON_KEY] ?: DEFAULT_FORECAST_HORIZON
    }

    /**
     * Update the forecast horizon setting
     *
     * @param days Number of days to forecast (must be between 1 and 180)
     */
    suspend fun setForecastHorizon(days: Int) {
        require(days in 1..180) { "Forecast horizon must be between 1 and 180 days" }

        dataStore.edit { preferences ->
            preferences[FORECAST_HORIZON_KEY] = days
        }
    }

    /**
     * Get the current forecast horizon value (suspend function for one-time reads)
     */
    suspend fun getForecastHorizonValue(): Int {
        return dataStore.data.map { preferences ->
            preferences[FORECAST_HORIZON_KEY] ?: DEFAULT_FORECAST_HORIZON
        }.first()
    }

    /**
     * Get the offline mode setting as a Flow
     */
    val offlineMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE_KEY] ?: false
    }

    /**
     * Update the offline mode setting
     */
    suspend fun setOfflineMode(isOffline: Boolean) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_KEY] = isOffline
        }
    }

    /**
     * Get the current offline mode value (suspend function for one-time reads)
     */
    suspend fun getOfflineModeValue(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[OFFLINE_MODE_KEY] ?: false
        }.first()
    }
}
