package com.example.aquaforecast.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.aquaforecast.BuildConfig
import com.example.aquaforecast.data.local.dao.FarmDataDao
import com.example.aquaforecast.data.local.dao.FeedingScheduleDao
import com.example.aquaforecast.data.local.dao.PondDao
import com.example.aquaforecast.data.local.dao.PredictionDao
import com.example.aquaforecast.data.local.entity.FarmDataEntity
import com.example.aquaforecast.data.local.entity.FeedingScheduleEntity
import com.example.aquaforecast.data.local.entity.PondEntity
import com.example.aquaforecast.data.local.entity.PredictionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * DatabaseBootstrapper provides sample data for debug builds.
 * It populates the database with realistic aquaculture data for testing and development.
 */
class DatabaseBootstrapper(
    private val dataStore: DataStore<Preferences>,
    private val pondDao: PondDao,
    private val farmDataDao: FarmDataDao,
    private val predictionDao: PredictionDao,
    private val feedingScheduleDao: FeedingScheduleDao
) {
    private val bootstrapKey = booleanPreferencesKey("database_bootstrapped")

    suspend fun bootstrap() {
        // Only bootstrap if enabled in BuildConfig and not already bootstrapped
        if (!BuildConfig.ENABLE_DATABASE_BOOTSTRAP) {
            return
        }

        val isBootstrapped = dataStore.data.map { preferences ->
            preferences[bootstrapKey] ?: false
        }.first()

        if (isBootstrapped) {
            return
        }

        try {
//             Insert sample data
            insertSamplePonds()
            insertSampleFarmData()
            insertSamplePredictions()
            insertSampleFeedingSchedules()

            // Mark as bootstrapped
            dataStore.edit { preferences ->
                preferences[bootstrapKey] = true
            }
        } catch (e: Exception) {
            Log.e("DatabaseBootstrapper", "Bootstrap failed", e)
        }
    }

    private suspend fun insertSamplePonds() {
        val currentTime = System.currentTimeMillis()
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)
        val sixtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(60)

        val ponds = listOf(
            PondEntity(
                id = 1,
                name = "Pond A - Catfish",
                species = "CATFISH",
                stockCount = 1500,
                startDate = sixtyDaysAgo,
                createdAt = sixtyDaysAgo
            ),
            PondEntity(
                id = 2,
                name = "Pond B - Catfish",
                species = "CATFISH",
                stockCount = 1200,
                startDate = thirtyDaysAgo,
                createdAt = thirtyDaysAgo
            ),
            PondEntity(
                id = 3,
                name = "Pond C - Catfish",
                species = "CATFISH",
                stockCount = 2000,
                startDate = currentTime - TimeUnit.DAYS.toMillis(45),
                createdAt = currentTime - TimeUnit.DAYS.toMillis(45)
            )
        )

        ponds.forEach { pond ->
            pondDao.insert(pond)
        }
    }

    private suspend fun insertSampleFarmData() {
        val currentTime = System.currentTimeMillis()

        // Generate exactly 100 entries for ML training (minimum required)
        // Strategy: Generate 34, 33, 33 entries for ponds 1, 2, 3 respectively
        val pondEntries = mapOf(
            "1" to 34,
            "2" to 33,
            "3" to 33
        )

        pondEntries.forEach { (pondId, entryCount) ->
            for (day in 0 until entryCount) {
                val timestamp = currentTime - TimeUnit.DAYS.toMillis(day.toLong())

                // Generate realistic varying water parameters based on pond lifecycle
                // Water quality typically changes as fish grow and pond matures
                val pondAge = 90 - day // Simulate pond age in days
                val ageFactorTemp = if (pondAge > 60) 0.5 else 0.0 // Older ponds slightly warmer
                val ageFactorAmmonia = if (pondAge > 45) 0.05 else 0.0 // More waste accumulation

                val baseTemp = 28.0 + ageFactorTemp
                val basePh = 7.2
                val baseDO = 6.5
                val baseAmmonia = 0.1 + ageFactorAmmonia
                val baseNitrate = 10.0
                val baseTurbidity = 25.0

                // Add realistic daily variation (Gaussian-like distribution)
                val tempVariation = (Math.random() - 0.5) * 2
                val phVariation = (Math.random() - 0.5) * 0.6
                val doVariation = (Math.random() - 0.5) * 1.5
                val ammoniaVariation = (Math.random() - 0.5) * 0.1
                val nitrateVariation = (Math.random() - 0.5) * 5
                val turbidityVariation = (Math.random() - 0.5) * 10

                // Generate realistic location coordinates for East African aquaculture region
                // Example: Uganda/Kenya/Tanzania region
                // Latitude: -5 to 5 (equatorial region)
                // Longitude: 29 to 42 (East Africa)
                val baseLatitude = 0.0 // Equatorial region
                val baseLongitude = 32.5 // Uganda/Kenya region
                val randomLatitude = baseLatitude + ((Math.random() - 0.5) * 10) // ±5 degrees
                val randomLongitude = baseLongitude + ((Math.random() - 0.5) * 13) // ±6.5 degrees

                val farmData = FarmDataEntity(
                    temperature = (baseTemp + tempVariation).coerceIn(22.0, 35.0), // Realistic range
                    ph = (basePh + phVariation).coerceIn(6.0, 9.0), // Safe pH range
                    dissolvedOxygen = (baseDO + doVariation).coerceIn(3.0, 12.0), // Realistic DO
                    ammonia = (baseAmmonia + ammoniaVariation).coerceIn(0.0, 0.5), // Safe ammonia
                    nitrate = (baseNitrate + nitrateVariation).coerceIn(0.0, 40.0), // Safe nitrate
                    turbidity = (baseTurbidity + turbidityVariation).coerceIn(5.0, 80.0), // Realistic turbidity
                    timestamp = timestamp,
                    pondId = pondId,
                    latitude = randomLatitude,
                    longitude = randomLongitude,
                    isSynced = false // Marked as unsynced so it will be synced to backend
                )

                // Insert farm data and get the ID
                val farmDataId = farmDataDao.insert(farmData)

                // Create prediction for EVERY entry
                // This matches real-world usage where every water quality reading generates a prediction
                val baseDaysInFarm = 90 - day // Days since pond started (simulated)

                // Realistic fish growth curve (catfish growth rates)
                // Week 1-4: 0.3-0.5 kg, Week 5-8: 0.5-0.8 kg, Week 9-12: 0.8-1.2 kg
                val growthWeek = baseDaysInFarm / 7
                val baseWeight = when {
                    growthWeek < 4 -> 0.3 + (growthWeek * 0.05) // Early growth
                    growthWeek < 8 -> 0.5 + ((growthWeek - 4) * 0.075) // Mid growth
                    else -> 0.8 + ((growthWeek - 8) * 0.05) // Late growth
                }

                // Length correlates with weight (allometric growth)
                val baseLength = 15.0 + (Math.pow(baseWeight * 100, 0.4) * 2.5)

                // Add realistic biological variation (5-10% CV)
                val weightVariation = baseWeight * ((Math.random() - 0.5) * 0.15)
                val lengthVariation = baseLength * ((Math.random() - 0.5) * 0.1)

                // All bootstrap entries are verified for ML training
                // This provides exactly 100 verified entries for initial model training
                val prediction = PredictionEntity(
                    predictedWeight = (baseWeight + weightVariation).coerceIn(0.1, 3.0),
                    predictedLength = (baseLength + lengthVariation).coerceIn(10.0, 60.0),
                    harvestDate = timestamp + TimeUnit.DAYS.toMillis(30), // 30 days ahead
                    createdAt = timestamp,
                    pondId = pondId,
                    farmDataId = farmDataId,
                    verified = true // All bootstrap data is verified for ML training
                )

                predictionDao.insert(prediction)
            }
        }
    }

    private suspend fun insertSamplePredictions() {
        // Predictions are now created inline with farm data entries (see insertSampleFarmData)
        // This method can be used for creating standalone predictions for testing specific features
        // For now, it's kept empty but can be used to add additional test predictions if needed

        // Note: Most predictions are now linked to farmDataId and created in insertSampleFarmData()
        // This ensures proper sync logic testing where predictions are associated with specific readings
    }

    private suspend fun insertSampleFeedingSchedules() {
        val currentTime = System.currentTimeMillis()
        val schedules = listOf(
            FeedingScheduleEntity(
                name = "Morning Feed - Pond A",
                pondId = 1,
                pondName = "Pond A - Tilapia",
                startDate = currentTime - TimeUnit.DAYS.toMillis(30),
                endDate = currentTime + TimeUnit.DAYS.toMillis(30),
                feedingTime = "08:00",
                isActive = true,
                createdAt = currentTime - TimeUnit.DAYS.toMillis(30)
            ),
            FeedingScheduleEntity(
                name = "Evening Feed - Pond A",
                pondId = 1,
                pondName = "Pond A - Tilapia",
                startDate = currentTime - TimeUnit.DAYS.toMillis(30),
                endDate = currentTime + TimeUnit.DAYS.toMillis(30),
                feedingTime = "18:00",
                isActive = true,
                createdAt = currentTime - TimeUnit.DAYS.toMillis(30)
            ),
            FeedingScheduleEntity(
                name = "Morning Feed - Pond B",
                pondId = 2,
                pondName = "Pond B - Catfish",
                startDate = currentTime - TimeUnit.DAYS.toMillis(15),
                endDate = currentTime + TimeUnit.DAYS.toMillis(45),
                feedingTime = "07:30",
                isActive = true,
                createdAt = currentTime - TimeUnit.DAYS.toMillis(15)
            ),
            FeedingScheduleEntity(
                name = "Afternoon Feed - Pond B",
                pondId = 2,
                pondName = "Pond B - Catfish",
                startDate = currentTime - TimeUnit.DAYS.toMillis(15),
                endDate = currentTime + TimeUnit.DAYS.toMillis(45),
                feedingTime = "16:00",
                isActive = true,
                createdAt = currentTime - TimeUnit.DAYS.toMillis(15)
            ),
            FeedingScheduleEntity(
                name = "Morning Feed - Pond C",
                pondId = 3,
                pondName = "Pond C - Tilapia",
                startDate = currentTime - TimeUnit.DAYS.toMillis(20),
                endDate = currentTime + TimeUnit.DAYS.toMillis(40),
                feedingTime = "08:30",
                isActive = true,
                createdAt = currentTime - TimeUnit.DAYS.toMillis(20)
            ),
            FeedingScheduleEntity(
                name = "Evening Feed - Pond C",
                pondId = 3,
                pondName = "Pond C - Tilapia",
                startDate = currentTime - TimeUnit.DAYS.toMillis(20),
                endDate = currentTime + TimeUnit.DAYS.toMillis(40),
                feedingTime = "17:30",
                isActive = false, // Inactive schedule for testing
                createdAt = currentTime - TimeUnit.DAYS.toMillis(20)
            )
        )

        schedules.forEach { schedule ->
            feedingScheduleDao.insert(schedule)
        }
    }

    /**
     * Force re-bootstrap by clearing the bootstrap flag.
     * Useful for development when you want to reset sample data.
     */
    suspend fun clearBootstrapFlag() {
        dataStore.edit { preferences ->
            preferences.remove(bootstrapKey)
        }
    }
}
