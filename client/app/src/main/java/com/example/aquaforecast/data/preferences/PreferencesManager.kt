package com.example.aquaforecast.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
}
