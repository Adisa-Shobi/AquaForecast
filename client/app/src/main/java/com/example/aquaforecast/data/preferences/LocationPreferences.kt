package com.example.aquaforecast.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages storage of the last location used for data entry
 */
class LocationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save the last location used for data entry
     */
    fun saveLastUsedLocation(latitude: Double, longitude: Double) {
        prefs.edit()
            .putFloat(KEY_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LONGITUDE, longitude.toFloat())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get the last location used for data entry
     * @return Pair of (latitude, longitude) or null if no location was saved
     */
    fun getLastUsedLocation(): Pair<Double, Double>? {
        val latitude = prefs.getFloat(KEY_LATITUDE, Float.NaN)
        val longitude = prefs.getFloat(KEY_LONGITUDE, Float.NaN)

        return if (!latitude.isNaN() && !longitude.isNaN()) {
            Pair(latitude.toDouble(), longitude.toDouble())
        } else {
            null
        }
    }

    /**
     * Get the timestamp when the last location was saved
     */
    fun getLastUsedLocationTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_TIMESTAMP, -1L)
        return if (timestamp != -1L) timestamp else null
    }

    /**
     * Clear the saved location
     */
    fun clearLastUsedLocation() {
        prefs.edit()
            .remove(KEY_LATITUDE)
            .remove(KEY_LONGITUDE)
            .remove(KEY_TIMESTAMP)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "location_prefs"
        private const val KEY_LATITUDE = "last_latitude"
        private const val KEY_LONGITUDE = "last_longitude"
        private const val KEY_TIMESTAMP = "last_timestamp"
    }
}
