package com.example.aquaforecast.data.ml

import android.content.Context
import android.util.Log
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Prediction
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit

private const val TAG = "MLPredictor"


 // ML Predictor for fish growth forecasting using TensorFlow Lite
class MLPredictor(
    private val context: Context,
    private val farmDataRepository: FarmDataRepository
) {
    private var interpreter: Interpreter? = null
    private val featurePreprocessor = FeaturePreprocessor(context)

    companion object {
        private const val MODEL_FILE_NAME = "aqua_forecast_model.tflite"
        private const val MODEL_DIR = "models"

        // Model output indices
        private const val OUTPUT_WEIGHT_INDEX = 0
        private const val OUTPUT_LENGTH_INDEX = 1
        private const val OUTPUT_SIZE = 2 // weight and length predictions

        // Default weights for harvest readiness (in kg)
        private const val TILAPIA_HARVEST_WEIGHT = 0.5  // 500g
        private const val CATFISH_HARVEST_WEIGHT = 1.0  // 1kg

        // Conversion constants
        private const val GRAMS_TO_KG = 1000.0

        // Growth estimation (simplified - based on typical aquaculture data)
        private const val GROWTH_RATE_PER_DAY_KG = 0.01

        // Confidence calculation constants
        private const val MIN_CONFIDENCE = 0.5
        private const val MAX_CONFIDENCE = 0.95
        private const val HISTORICAL_WINDOW_DAYS = 7.0
        private const val CONSISTENCY_WEIGHT = 0.6
        private const val COMPLETENESS_WEIGHT = 0.4
    }

    /**
     * Initialize the TFLite interpreter by loading the model
     * Tries to load downloaded model from internal storage first,
     * falls back to bundled model in assets if not available.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to initialize ML model: ${e.message}")
        }
    }

    /**
     * Generate predictions for a specific pond
     *
     * @param pond Pond to generate predictions for
     * @return Result with Prediction object or error message
     */
    suspend fun predict(pond: Pond): Result<Prediction> = withContext(Dispatchers.IO) {
        try {
            // Ensure interpreter is initialized
            if (interpreter == null) {
                initialize().let { result ->
                    if (result is Result.Error) {
                        return@withContext result
                    }
                }
            }

            // Get latest farm data for the pond
            val latestDataResult = farmDataRepository.getLatest(pond.id.toString())
            val latestData = latestDataResult.getOrNull()
                ?: return@withContext Result.Error("No farm data available for prediction")

            // Get historical data (last 7 days)
            val historicalDataResult = farmDataRepository.getRecent(pond.id.toString(), limit = 7)
            val historicalData = historicalDataResult.getOrNull() ?: emptyList()

            // Prepare features (fishWeight is optional, defaults to null)
            val features = featurePreprocessor.prepareFeatures(
                latestData = latestData,
                historicalData = historicalData,
                pond = pond
            )

            // Run inference
            val outputs = runInference(features)
                ?: return@withContext Result.Error("Inference failed")

            // Parse outputs (weight in grams, length in cm)
            val predictedWeightGrams = outputs[OUTPUT_WEIGHT_INDEX].toDouble()
            val predictedLength = outputs[OUTPUT_LENGTH_INDEX].toDouble()

            // Convert weight from grams to kg for internal use
            val predictedWeight = predictedWeightGrams / GRAMS_TO_KG

            // Calculate harvest date based on predicted weight
            val harvestDate = calculateHarvestDate(
                pond = pond,
                currentWeight = predictedWeight,
                latestData = latestData
            )

            // Calculate confidence score (simplified)
            val confidence = calculateConfidence(
                predictedWeight = predictedWeight,
                historicalData = historicalData
            )

            val prediction = Prediction(
                predictedWeight = predictedWeight,
                predictedLength = predictedLength,
                harvestDate = harvestDate,
                confidence = confidence,
                createdAt = System.currentTimeMillis(),
                pondId = pond.id.toString()
            )

            Result.Success(prediction)
        } catch (e: Exception) {
            Result.Error("Prediction failed: ${e.message}")
        }
    }

    /**
     * Run TFLite inference
     *
     * @param features Normalized input features
     * @return FloatArray of model outputs [weight, length] or null if failed
     */
    private fun runInference(features: FloatArray): FloatArray? {
        try {
            val interpreter = this.interpreter ?: return null

            // Prepare input tensor (batch size 1)
            val inputArray = Array(1) { features }

            // Prepare output tensor
            val outputArray = Array(1) { FloatArray(OUTPUT_SIZE) }

            // Run inference
            interpreter.run(inputArray, outputArray)

            return outputArray[0]
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            return null
        }
    }

    /**
     * Calculate estimated harvest date based on predicted weight and species
     *
     * @param pond Pond information
     * @param currentWeight Current predicted weight (kg)
     * @param latestData Latest farm data
     * @return Harvest date in milliseconds
     */
    private fun calculateHarvestDate(
        pond: Pond,
        currentWeight: Double,
        latestData: FarmData
    ): Long {
        val targetWeight = when (pond.species.name) {
            "TILAPIA" -> TILAPIA_HARVEST_WEIGHT
            "CATFISH" -> CATFISH_HARVEST_WEIGHT
            else -> CATFISH_HARVEST_WEIGHT
        }

        // If already at harvest weight, return current date
        if (currentWeight >= targetWeight) {
            return latestData.timestamp
        }

        // Estimate days until harvest based on growth rate
        // Note: Growth rate is simplified estimate based on typical aquaculture data
        val weightDeficit = targetWeight - currentWeight
        val daysUntilHarvest = (weightDeficit / GROWTH_RATE_PER_DAY_KG).toLong()

        return latestData.timestamp + TimeUnit.DAYS.toMillis(daysUntilHarvest)
    }

    /**
     * Calculate prediction confidence score based on data quality and consistency
     * Confidence is calculated from data consistency (dissolved oxygen variance)
     * and completeness (number of historical data points)
     *
     * @param predictedWeight Predicted weight from model
     * @param historicalData Historical farm data
     * @return Confidence score (MIN_CONFIDENCE - MAX_CONFIDENCE)
     */
    private fun calculateConfidence(
        predictedWeight: Double,
        historicalData: List<FarmData>
    ): Double {
        if (historicalData.isEmpty()) {
            return MIN_CONFIDENCE
        }

        // Calculate data consistency (lower variance = higher confidence)
        val doValues = historicalData.map { it.dissolvedOxygen }
        val doMean = doValues.average()
        val doVariance = doValues.map { (it - doMean) * (it - doMean) }.average()
        val doStdDev = kotlin.math.sqrt(doVariance)

        // Normalize to 0-1 range (lower std dev = higher confidence)
        val consistencyScore = 1.0 - (doStdDev / doMean).coerceIn(0.0, 1.0)

        // Data completeness score
        val completenessScore = (historicalData.size / HISTORICAL_WINDOW_DAYS).coerceIn(0.0, 1.0)

        // Weighted average
        return (consistencyScore * CONSISTENCY_WEIGHT + completenessScore * COMPLETENESS_WEIGHT)
            .coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE)
    }

    /**
     * Load TFLite model file
     * Tries downloaded model first, falls back to bundled model in assets
     *
     * @return MappedByteBuffer containing the model
     */
    private fun loadModelFile(): MappedByteBuffer {
        // Check for downloaded model in internal storage
        val downloadedModel = File(context.filesDir, "$MODEL_DIR/$MODEL_FILE_NAME")

        if (downloadedModel.exists()) {
            Log.i(TAG, "Loading downloaded model from: ${downloadedModel.absolutePath}")
            val inputStream = FileInputStream(downloadedModel)
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, downloadedModel.length())
        }

        // Fall back to bundled model in assets
        Log.i(TAG, "Loading bundled model from assets")
        val fileDescriptor = context.assets.openFd(MODEL_FILE_NAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Release resources when done
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
