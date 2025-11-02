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

        // Model output indices
        private const val OUTPUT_WEIGHT_INDEX = 0
        private const val OUTPUT_LENGTH_INDEX = 1
        private const val OUTPUT_SIZE = 2 // weight and length predictions

        // Default weights for harvest readiness (in kg)
        private const val TILAPIA_HARVEST_WEIGHT = 0.5  // 500g
        private const val CATFISH_HARVEST_WEIGHT = 1.0  // 1kg
    }

    /**
     * Initialize the TFLite interpreter by loading the model from assets
     * Should be called before making predictions
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

            Log.d(TAG, "========== PREDICTION INPUT ==========")
            Log.d(TAG, "Pond: ${pond.name} (ID: ${pond.id}, Species: ${pond.species})")
            Log.d(TAG, "Stock Count: ${pond.stockCount}")
            Log.d(TAG, "Latest Data:")
            Log.d(TAG, "  - Temperature: ${latestData.temperature}°C")
            Log.d(TAG, "  - pH: ${latestData.ph}")
            Log.d(TAG, "  - Dissolved Oxygen: ${latestData.dissolvedOxygen} mg/L")
            Log.d(TAG, "  - Ammonia: ${latestData.ammonia} ppm")
            Log.d(TAG, "  - Nitrate: ${latestData.nitrate} ppm")
            Log.d(TAG, "  - Turbidity: ${latestData.turbidity} NTU")
            Log.d(TAG, "Historical Data Points: ${historicalData.size}")
            Log.d(TAG, "Preprocessed Features (scaled): ${features.contentToString()}")

            // Run inference
            val outputs = runInference(features)
                ?: return@withContext Result.Error("Inference failed")

            // Parse outputs (weight in grams, length in cm)
            val predictedWeightGrams = outputs[OUTPUT_WEIGHT_INDEX].toDouble()
            val predictedLength = outputs[OUTPUT_LENGTH_INDEX].toDouble()

            // Convert weight from grams to kg for internal use
            val predictedWeight = predictedWeightGrams / 1000.0

            Log.d(TAG, "========== PREDICTION OUTPUT ==========")
            Log.d(TAG, "Model Output:")
            Log.d(TAG, "  - Weight: ${predictedWeightGrams}g (${predictedWeight}kg)")
            Log.d(TAG, "  - Length: ${predictedLength}cm")

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
            e.printStackTrace()
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
        // This is a simplified calculation - adjust based on your model's predictions
        val weightDeficit = targetWeight - currentWeight
        val growthRatePerDay = 0.01 // TODO: Find a scientifically backed way to calculate this
        val daysUntilHarvest = (weightDeficit / growthRatePerDay).toLong()

        return latestData.timestamp + TimeUnit.DAYS.toMillis(daysUntilHarvest)
    }

    /**
     * Calculate prediction confidence score based on data quality and consistency
     *
     * @param predictedWeight Predicted weight from model
     * @param historicalData Historical farm data
     * @return Confidence score (0.0 - 1.0)
     */
    // TODO: Improve the algorithm
    private fun calculateConfidence(
        predictedWeight: Double,
        historicalData: List<FarmData>
    ): Double {
        if (historicalData.isEmpty()) {
            return 0.5 // Low confidence with no historical data
        }

        // Calculate data consistency (lower variance = higher confidence)
        val doValues = historicalData.map { it.dissolvedOxygen }
        val doMean = doValues.average()
        val doVariance = doValues.map { (it - doMean) * (it - doMean) }.average()
        val doStdDev = kotlin.math.sqrt(doVariance)

        // Normalize to 0-1 range (lower std dev = higher confidence)
        val consistencyScore = 1.0 - (doStdDev / doMean).coerceIn(0.0, 1.0)

        // Data completeness score
        val completenessScore = (historicalData.size / 7.0).coerceIn(0.0, 1.0)

        // Weighted average
        return (consistencyScore * 0.6 + completenessScore * 0.4).coerceIn(0.5, 0.95)
    }

    /**
     * Load TFLite model file from assets folder
     *
     * @return MappedByteBuffer containing the model
     */
    private fun loadModelFile(): MappedByteBuffer {
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
