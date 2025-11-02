package com.example.aquaforecast.data.local

import android.content.Context
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
            // Log error but don't crash the app
            e.printStackTrace()
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
        val farmDataList = mutableListOf<FarmDataEntity>()

        // Generate data for the last 30 days for each pond
        val ponds = listOf("1", "2", "3")

        ponds.forEach { pondId ->
            for (day in 0..29) {
                val timestamp = currentTime - TimeUnit.DAYS.toMillis(day.toLong())

                // Generate realistic varying water parameters
                val baseTemp = 28.0
                val basePh = 7.2
                val baseDO = 6.5
                val baseAmmonia = 0.1
                val baseNitrate = 10.0
                val baseTurbidity = 25.0

                // Add some variation
                val tempVariation = (Math.random() - 0.5) * 2
                val phVariation = (Math.random() - 0.5) * 0.6
                val doVariation = (Math.random() - 0.5) * 1.5
                val ammoniaVariation = (Math.random() - 0.5) * 0.1
                val nitrateVariation = (Math.random() - 0.5) * 5
                val turbidityVariation = (Math.random() - 0.5) * 10

                farmDataList.add(
                    FarmDataEntity(
                        temperature = baseTemp + tempVariation,
                        ph = basePh + phVariation,
                        dissolvedOxygen = baseDO + doVariation,
                        ammonia = (baseAmmonia + ammoniaVariation).coerceAtLeast(0.0),
                        nitrate = (baseNitrate + nitrateVariation).coerceAtLeast(0.0),
                        turbidity = (baseTurbidity + turbidityVariation).coerceAtLeast(0.0),
                        timestamp = timestamp,
                        pondId = pondId,
                        isSynced = day < 15 // Older data is synced
                    )
                )
            }
        }

        farmDataList.forEach { farmData ->
            farmDataDao.insert(farmData)
        }
    }

    private suspend fun insertSamplePredictions() {
        val currentTime = System.currentTimeMillis()
        val predictions = listOf(
            PredictionEntity(
                predictedWeight = 0.4505,
                predictedLength = 28.3,
                harvestDate = currentTime + TimeUnit.DAYS.toMillis(30),
                createdAt = currentTime,
                pondId = "1"
            ),
            PredictionEntity(
                predictedWeight = 0.5202,
                predictedLength = 31.5,
                harvestDate = currentTime + TimeUnit.DAYS.toMillis(60),
                createdAt = currentTime,
                pondId = "2"
            ),
            PredictionEntity(
                predictedWeight = 0.3800,
                predictedLength = 25.8,
                harvestDate = currentTime + TimeUnit.DAYS.toMillis(45),
                createdAt = currentTime,
                pondId = "3"
            ),
            // Add historical prediction for Pond 1
            PredictionEntity(
                predictedWeight = 0.4200,
                predictedLength = 26.5,
                harvestDate = currentTime + TimeUnit.DAYS.toMillis(20),
                createdAt = currentTime - TimeUnit.DAYS.toMillis(10),
                pondId = "1"
            )
        )

        predictions.forEach { prediction ->
            predictionDao.insert(prediction)
        }
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
