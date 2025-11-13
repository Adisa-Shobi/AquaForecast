package com.example.aquaforecast

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aquaforecast.data.ml.MLPredictor
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Species
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.Result
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.random.Random

/**
 * Fake repository for testing - returns pre-built data instantly
 */
class FakeFarmDataRepository(private val testPond: Pond) : FarmDataRepository {
    override suspend fun getLatest(pondId: String): Result<FarmData?> {
        return Result.Success(createSampleFarmData())
    }

    override suspend fun getRecent(pondId: String, limit: Int): Result<List<FarmData>> {
        return Result.Success(List(7) { createSampleFarmData(it) })
    }

    override suspend fun save(farmData: FarmData): Result<Long> = Result.Success(1L)
    override suspend fun getAll(): Result<List<FarmData>> = Result.Success(emptyList())
    override suspend fun getByDateRange(startDate: Long, endDate: Long): Result<List<FarmData>> = Result.Success(emptyList())
    override suspend fun getUnsynced(): Result<List<FarmData>> = Result.Success(emptyList())
    override suspend fun markSynced(ids: List<Long>): Result<Unit> = Result.Success(Unit)

    private fun createSampleFarmData(index: Int = 0): FarmData {
        val random = Random(42 + index)
        return FarmData(
            id = index.toLong(),
            pondId = testPond.id.toString(),
            timestamp = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
            temperature = 25.0 + random.nextDouble() * 8.0,
            ph = 6.8 + random.nextDouble() * 1.4,
            dissolvedOxygen = 5.0 + random.nextDouble() * 4.0,
            ammonia = random.nextDouble() * 1.5,
            nitrate = random.nextDouble() * 40.0,
            turbidity = random.nextDouble() * 30.0,
            isSynced = true
        )
    }
}

/**
 * Performance tests for ML inference latency
 *
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aquaforecast.MLPredictorPerformanceTest
 */
@RunWith(AndroidJUnit4::class)
class MLPredictorPerformanceTest {

    private lateinit var mlPredictor: MLPredictor
    private lateinit var fakeRepository: FarmDataRepository
    private lateinit var context: Context
    private lateinit var testPond: Pond

    companion object {
        private const val TARGET_LATENCY_MS = 100L
        private const val WARMUP_ITERATIONS = 10
        private const val MEASUREMENT_ITERATIONS = 100
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        testPond = Pond(
            id = 1,
            name = "Test Pond",
            species = Species.CATFISH,
            stockCount = 1000,
            startDate = LocalDate.now().minusDays(30)
        )

        fakeRepository = FakeFarmDataRepository(testPond)
        mlPredictor = MLPredictor(context, fakeRepository)
        runBlocking { mlPredictor.initialize() }
    }

    @Test
    fun inferenceLatencyShouldBeUnder100ms() = runBlocking {
        // GC and settle
        System.gc()
        Thread.sleep(100)

        // Warm-up
        repeat(WARMUP_ITERATIONS) {
            mlPredictor.predict(testPond)
        }

        // Measure
        val latencies = mutableListOf<Long>()
        repeat(MEASUREMENT_ITERATIONS) {
            val startTime = System.nanoTime()
            mlPredictor.predict(testPond)
            val endTime = System.nanoTime()
            latencies.add((endTime - startTime) / 1_000_000)
        }

        val sorted = latencies.sorted()
        val avg = latencies.average()
        val p95 = sorted[95]
        val p99 = sorted[99]

        println("Avg: ${avg.toInt()}ms | P95: ${p95}ms | P99: ${p99}ms")

        assert(avg < TARGET_LATENCY_MS) { "Avg ${avg.toInt()}ms exceeds ${TARGET_LATENCY_MS}ms" }
        assert(p95 < TARGET_LATENCY_MS * 1.5) { "P95 ${p95}ms exceeds ${(TARGET_LATENCY_MS * 1.5).toLong()}ms" }
        assert(p99 < TARGET_LATENCY_MS * 2) { "P99 ${p99}ms exceeds ${TARGET_LATENCY_MS * 2}ms" }
    }

    @Test
    fun inferenceShouldBeConsistentAcrossVariedInputs() = runBlocking {
        System.gc()
        Thread.sleep(100)

        val seeds = listOf(42, 123, 456, 789, 999)
        val averages = mutableListOf<Double>()

        seeds.forEach { seed ->
            // Create new fake repo with different seed for variety
            val variedRepo = object : FarmDataRepository {
                override suspend fun getLatest(pondId: String) = Result.Success(createSampleFarmData(seed = seed))
                override suspend fun getRecent(pondId: String, limit: Int) = Result.Success(List(7) { createSampleFarmData(it, seed) })
                override suspend fun save(farmData: FarmData) = Result.Success(1L)
                override suspend fun getAll() = Result.Success(emptyList<FarmData>())
                override suspend fun getByDateRange(startDate: Long, endDate: Long) = Result.Success(emptyList<FarmData>())
                override suspend fun getUnsynced() = Result.Success(emptyList<FarmData>())
                override suspend fun markSynced(ids: List<Long>) = Result.Success(Unit)
            }

            val variedPredictor = MLPredictor(context, variedRepo)
            runBlocking { variedPredictor.initialize() }

            repeat(WARMUP_ITERATIONS / 2) {
                variedPredictor.predict(testPond)
            }

            val latencies = mutableListOf<Long>()
            repeat(MEASUREMENT_ITERATIONS / 2) {
                val start = System.nanoTime()
                variedPredictor.predict(testPond)
                latencies.add((System.nanoTime() - start) / 1_000_000)
            }
            averages.add(latencies.average())
        }

        val overallAvg = averages.average()
        val variance = averages.map { (it - overallAvg) * (it - overallAvg) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        println("Varied inputs - Avg: ${overallAvg.toInt()}ms | StdDev: ${stdDev.toInt()}ms")

        averages.forEach { avg ->
            assert(avg < TARGET_LATENCY_MS) { "Variation avg ${avg.toInt()}ms exceeds ${TARGET_LATENCY_MS}ms" }
        }
        assert(stdDev < TARGET_LATENCY_MS * 0.2) { "StdDev ${stdDev.toInt()}ms too high" }
    }

    private fun createSampleFarmData(index: Int = 0, seed: Int = 42): FarmData {
        val random = Random(seed + index)
        return FarmData(
            id = index.toLong(),
            pondId = testPond.id.toString(),
            timestamp = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
            temperature = 25.0 + random.nextDouble() * 8.0,
            ph = 6.8 + random.nextDouble() * 1.4,
            dissolvedOxygen = 5.0 + random.nextDouble() * 4.0,
            ammonia = random.nextDouble() * 1.5,
            nitrate = random.nextDouble() * 40.0,
            turbidity = random.nextDouble() * 30.0,
            isSynced = true
        )
    }
}
