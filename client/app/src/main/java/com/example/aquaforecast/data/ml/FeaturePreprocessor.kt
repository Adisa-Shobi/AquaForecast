package com.example.aquaforecast.data.ml

import android.content.Context
import android.util.Log
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Handles feature engineering and preprocessing for ML model input
 *
 * 1. Get Current Farm Data (7 base features)
 * 2. Apply Biological Caps (lines 429-443)
 * 3. Engineer 8 Additional Features (lines 328-371)
 * 4. Create Ordered Feature Array (15 total features, lines 523-533)
 * 5. Impute Missing Values (lines 461-471)
 * 6. Ensure Non-Negatives (lines 497-506)
 * 7. Scale Features using RobustScaler (lines 559-562)
 *
 * Output: FloatArray[15] ready for TFLite model input
 */
class FeaturePreprocessor(context: Context) {

    private val config: PreprocessingConfig
    private val json = Json { ignoreUnknownKeys = true }

    init {
        // Load preprocessing configuration from assets
        val configJson = context.assets.open("preprocessing_config.json")
            .bufferedReader()
            .use { it.readText() }
        config = json.decodeFromString(configJson)
    }

    companion object {
        // Features that should never be negative
        private val NON_NEGATIVE_FEATURES = setOf(
            "ammonia(g/ml)",
            "nitrate(g/ml)",
            "turbidity(ntu)",
            "population",
            "days_in_farm"
        )
    }

    /**
     * Prepare input features for ML model prediction
     *
     * Implements the complete 7-step preprocessing pipeline from the guide.
     *
     * @param latestData The most recent farm data entry (7 base features)
     * @param historicalData Last 7 days of farm data (for rolling averages)
     * @param pond Pond information (for days_in_farm calculation)
     * @return FloatArray of 16 scaled features ready for TFLite model input
     */
    fun prepareFeatures(
        latestData: FarmData,
        historicalData: List<FarmData>,
        pond: Pond
    ): FloatArray {
        // Get Current Farm Data (7 base features)
        val temperature = latestData.temperature.toFloat()
        val ph = latestData.ph.toFloat()
        val dissolvedOxygen = latestData.dissolvedOxygen.toFloat()
        val ammonia = latestData.ammonia.toFloat()
        val nitrate = latestData.nitrate.toFloat()
        val turbidity = latestData.turbidity.toFloat()

        // Apply Biological Caps
        val cappedFeatures = applyBiologicalCaps(
            temperature, ph, dissolvedOxygen,
            ammonia, nitrate, turbidity
        )

        // Engineer 8 Additional Features
        val engineeredFeatures = engineerFeatures(
            cappedFeatures = cappedFeatures,
            pond = pond,
            latestData = latestData,
            historicalData = historicalData,
        )

        // Create Ordered Feature Array (15 features)
        val orderedFeatures = createOrderedFeatureArray(
            cappedFeatures = cappedFeatures,
            engineeredFeatures = engineeredFeatures
        )

        // Impute Missing Values
        val imputedFeatures = imputeMissingValues(orderedFeatures)

        // Ensure Non-Negatives
        val validatedFeatures = ensureNonNegatives(imputedFeatures)

        // Scale Features (RobustScaler)
        return scaleFeatures(validatedFeatures)
    }

    /**
     * Clip base features to realistic ranges using biological_limits
     */
    private fun applyBiologicalCaps(
        temperature: Float,
        ph: Float,
        dissolvedOxygen: Float,
        ammonia: Float,
        nitrate: Float,
        turbidity: Float
    ): Map<String, Float> {
        return mapOf(
            "temperature(c)" to clampToLimits(temperature, "temperature(c)"),
            "ph" to clampToLimits(ph, "ph"),
            "dissolved_oxygen(g/ml)" to clampToLimits(dissolvedOxygen, "dissolved_oxygen(g/ml)"),
            "ammonia(g/ml)" to clampToLimits(ammonia, "ammonia(g/ml)"),
            "nitrate(g/ml)" to clampToLimits(nitrate, "nitrate(g/ml)"),
            "turbidity(ntu)" to clampToLimits(turbidity, "turbidity(ntu)")
        )
    }

    /**
     * Clamp a value to biological limits from config
     */
    private fun clampToLimits(value: Float, featureName: String): Float {
        val limits = config.biologicalLimits[featureName] ?: return value
        return value.coerceIn(limits[0], limits[1])
    }

    /**
     * STEP 3: Engineer 8 Additional Features
     *
     * Creates the same engineered features as the training pipeline to capture
     * temporal patterns, cyclic behavior, and water quality trends.
     */
    private fun engineerFeatures(
        cappedFeatures: Map<String, Float>,
        pond: Pond,
        latestData: FarmData,
        historicalData: List<FarmData>,
    ): Map<String, Float> {
        // Extract timestamp information
        val timestamp = java.time.Instant.ofEpochMilli(latestData.timestamp)
            .atZone(java.time.ZoneId.systemDefault())

        // days_in_farm: Days since pond start date
        // IMPORTANT: In training, this was "days since first record" for each pond.
        // For consistency, pond.startDate should be set to the date of the FIRST data entry,
        // not the physical pond creation date. This ensures the feature matches training data.
        val daysInFarm = ChronoUnit.DAYS.between(pond.startDate, LocalDate.now()).toFloat()
            Log.d("FeaturePreprocessor", "daysInFarm $daysInFarm")

        // day_of_year: Extract from timestamp (1-366)
        val dayOfYear = timestamp.dayOfYear.toFloat()
        Log.d("FeaturePreprocessor", "dayOfYear $dayOfYear")

        // hour: Extract from timestamp (0-23)
        val hour = timestamp.hour.toFloat()

        // sin_hour, cos_hour: Cyclic encoding
        val sinHour = sin(2 * PI * hour / 24).toFloat()
        val cosHour = cos(2 * PI * hour / 24).toFloat()

        // temp_do_interaction: Product of two features
        val tempDoInteraction = cappedFeatures["temperature(c)"]!! *
                cappedFeatures["dissolved_oxygen(g/ml)"]!!
        Log.d("FeaturePreprocessor", "tempDoInteraction $tempDoInteraction")

        // avg_do_7d: 7-day rolling average of DO
        val avgDo7d = calculateAvgDo7d(historicalData, cappedFeatures["dissolved_oxygen(g/ml)"]!!)
        Log.d("FeaturePreprocessor", "avgDo7d $avgDo7d")

        // avg_wqi_7d: Water quality deviation
        val avgWqi7d = abs(avgDo7d - config.constants.optimalDo) / config.constants.optimalDo
        Log.d("FeaturePreprocessor", "avgWqi7d $avgWqi7d")

        return mapOf(
            "days_in_farm" to daysInFarm,
            "day_of_year" to dayOfYear,
            "hour" to hour,
            "sin_hour" to sinHour,
            "cos_hour" to cosHour,
            "temp_do_interaction" to tempDoInteraction,
            "avg_do_7d" to avgDo7d,
            "avg_wqi_7d" to avgWqi7d
        )
    }

    /**
     * Calculate 7-day rolling average of dissolved oxygen
     *
     * Matches training pipeline behavior: includes current reading in the 7-day window
     * Equivalent to pandas: df['dissolved_oxygen'].rolling(window=7, min_periods=1).mean()
     */
    private fun calculateAvgDo7d(historicalData: List<FarmData>, currentDo: Float): Float {
        if (historicalData.isEmpty()) {
            return currentDo
        }

        // Include current DO value in the 7-day average (last 6 historical + current)
        // This matches the pandas rolling window behavior from training
        val historicalDoValues = historicalData.take(6).map { it.dissolvedOxygen.toFloat() }
        val allDoValues = historicalDoValues + currentDo

        return allDoValues.average().toFloat()
    }

    /**
     * Create Ordered Feature Array
     * Combine all 15 features in the exact order from scaler.feature_names
     */
    private fun createOrderedFeatureArray(
        cappedFeatures: Map<String, Float>,
        engineeredFeatures: Map<String, Float>
    ): FloatArray {
        // Combine all features into a single map
        val allFeatures = cappedFeatures + engineeredFeatures

        // Create array in exact order from config
        return config.scaler.featureNames.map { featureName ->
            allFeatures[featureName] ?: Float.NaN
        }.toFloatArray()
    }

    /**
     * Impute Missing Values
     * Replace any null/NaN with values from imputation_medians
     */
    private fun imputeMissingValues(features: FloatArray): FloatArray {
        return features.mapIndexed { index, value ->
            if (value.isNaN() || value.isInfinite()) {
                val featureName = config.scaler.featureNames[index]
                config.imputationMedians[featureName] ?: value
            } else {
                value
            }
        }.toFloatArray()
    }

    /**
     * Ensure Non-Negatives
     * For specific features only: ammonia, nitrate, turbidity, population, days_in_farm
     */
    private fun ensureNonNegatives(features: FloatArray): FloatArray {
        return features.mapIndexed { index, value ->
            val featureName = config.scaler.featureNames[index]
            if (featureName in NON_NEGATIVE_FEATURES) {
                max(0f, value)
            } else {
                value
            }
        }.toFloatArray()
    }

    /**
     * Scale Features (RobustScaler)
     * Apply: (value - center[/i]) / scale[/i] for each feature
     */
    private fun scaleFeatures(features: FloatArray): FloatArray {
        return features.mapIndexed { i, value ->
            (value - config.scaler.center[i]) / config.scaler.scale[i]
        }.toFloatArray()
    }
}
