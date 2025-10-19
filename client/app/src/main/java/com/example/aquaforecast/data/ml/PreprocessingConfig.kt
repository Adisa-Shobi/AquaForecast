package com.example.aquaforecast.data.ml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for ML preprocessing pipeline
 * Loaded from assets/preprocessing_config.json
 *
 * This configuration ensures that preprocessing matches the training pipeline exactly.
 */
@Serializable
data class PreprocessingConfig(
    val scaler: ScalerConfig,
    @SerialName("imputation_medians")
    val imputationMedians: Map<String, Float>,
    @SerialName("biological_limits")
    val biologicalLimits: Map<String, List<Float>>,
    val constants: Constants,
    @SerialName("target_columns")
    val targetColumns: List<String>
)

/**
 * RobustScaler configuration (center and scale values)
 */
@Serializable
data class ScalerConfig(
    val center: List<Float>,
    val scale: List<Float>,
    @SerialName("feature_names")
    val featureNames: List<String>
) {
    init {
        require(center.size == scale.size) {
            "Center and scale arrays must have the same length"
        }
        require(center.size == featureNames.size) {
            "Center array must match feature names length"
        }
    }
}

/**
 * Constants used in preprocessing
 */
@Serializable
data class Constants(
    @SerialName("optimal_do")
    val optimalDo: Float,
    @SerialName("initial_day_offset")
    val initialDayOffset: Int
)
