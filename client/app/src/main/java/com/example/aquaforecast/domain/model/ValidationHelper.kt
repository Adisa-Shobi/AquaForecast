package com.example.aquaforecast.domain.model

object ValidationHelper {

    // Temperature ranges (Celsius)
    private const val TEMP_MIN = 20.0
    private const val TEMP_MAX = 35.0
    private const val TEMP_OPTIMAL_MIN = 26.0
    private const val TEMP_OPTIMAL_MAX = 30.0

    // pH ranges
    private const val PH_MIN = 5.0
    private const val PH_MAX = 9.0
    private const val PH_OPTIMAL_MIN = 6.5
    private const val PH_OPTIMAL_MAX = 8.5

    // Dissolved Oxygen ranges (mg/L)
    private const val DO_MIN = 0.0
    private const val DO_MAX = 15.0
    private const val DO_OPTIMAL_MIN = 5.0
    private const val DO_CRITICAL = 3.0

    // Ammonia ranges (mg/L)
    private const val AMMONIA_MIN = 0.0
    private const val AMMONIA_MAX = 5.0
    private const val AMMONIA_OPTIMAL = 0.5
    private const val AMMONIA_WARNING = 1.0

    // Nitrate ranges (mg/L)
    private const val NITRATE_MIN = 0.0
    private const val NITRATE_MAX = 500.0
    private const val NITRATE_OPTIMAL = 40.0
    private const val NITRATE_WARNING = 100.0

    // Turbidity ranges (NTU)
    private const val TURBIDITY_MIN = 0.0
    private const val TURBIDITY_MAX = 500.0
    private const val TURBIDITY_OPTIMAL = 30.0
    private const val TURBIDITY_WARNING = 60.0

    // ========== Validation Functions ==========

    fun isTemperatureValid(temp: Double): Boolean {
        return temp in TEMP_MIN..TEMP_MAX
    }

    fun isPhValid(ph: Double): Boolean {
        return ph in PH_MIN..PH_MAX
    }

    fun isDissolvedOxygenValid(oxygen: Double): Boolean {
        return oxygen in DO_MIN..DO_MAX
    }

    fun isAmmoniaValid(ammonia: Double): Boolean {
        return ammonia in AMMONIA_MIN..AMMONIA_MAX
    }

    fun isNitrateValid(nitrate: Double): Boolean {
        return nitrate in NITRATE_MIN..NITRATE_MAX
    }

    fun isTurbidityValid(turbidity: Double): Boolean {
        return turbidity in TURBIDITY_MIN..TURBIDITY_MAX
    }

    // ========== Status Calculation Functions ==========

    fun getTemperatureStatus(temp: Double): WaterQualityStatus {
        return when {
            !isTemperatureValid(temp) -> WaterQualityStatus.CRITICAL
            temp in TEMP_OPTIMAL_MIN..TEMP_OPTIMAL_MAX -> WaterQualityStatus.OPTIMAL
            else -> WaterQualityStatus.WARNING
        }
    }

    fun getPhStatus(ph: Double): WaterQualityStatus {
        return when {
            !isPhValid(ph) -> WaterQualityStatus.CRITICAL
            ph in PH_OPTIMAL_MIN..PH_OPTIMAL_MAX -> WaterQualityStatus.OPTIMAL
            else -> WaterQualityStatus.WARNING
        }
    }

    fun getDissolvedOxygenStatus(oxygen: Double): WaterQualityStatus {
        return when {
            oxygen < DO_CRITICAL -> WaterQualityStatus.CRITICAL
            oxygen < DO_OPTIMAL_MIN -> WaterQualityStatus.WARNING
            oxygen > DO_MAX -> WaterQualityStatus.WARNING
            else -> WaterQualityStatus.OPTIMAL
        }
    }

    fun getAmmoniaStatus(ammonia: Double): WaterQualityStatus {
        return when {
            ammonia > AMMONIA_WARNING || ammonia < AMMONIA_MIN -> WaterQualityStatus.CRITICAL
            ammonia > AMMONIA_OPTIMAL -> WaterQualityStatus.WARNING
            else -> WaterQualityStatus.OPTIMAL
        }
    }

    fun getNitrateStatus(nitrate: Double): WaterQualityStatus {
        return when {
            nitrate > NITRATE_MAX || nitrate < NITRATE_MIN -> WaterQualityStatus.CRITICAL
            nitrate > NITRATE_WARNING -> WaterQualityStatus.WARNING
            nitrate > NITRATE_OPTIMAL -> WaterQualityStatus.WARNING
            else -> WaterQualityStatus.OPTIMAL
        }
    }

    fun getTurbidityStatus(turbidity: Double): WaterQualityStatus {
        return when {
            !isTurbidityValid(turbidity) -> WaterQualityStatus.CRITICAL
            turbidity > TURBIDITY_WARNING -> WaterQualityStatus.WARNING
            turbidity > TURBIDITY_OPTIMAL -> WaterQualityStatus.WARNING
            else -> WaterQualityStatus.OPTIMAL
        }
    }

    // ========== Helper Functions ==========

    fun getRecommendation(parameter: String, status: WaterQualityStatus): String {
        if (status == WaterQualityStatus.OPTIMAL) {
            return "Maintain current levels"
        }

        return when (parameter.lowercase()) {
            "temperature" -> when (status) {
                WaterQualityStatus.WARNING -> "Adjust temperature to 26-30°C range"
                WaterQualityStatus.CRITICAL -> "Urgent: Temperature outside safe range. Add shade or aeration"
                else -> ""
            }
            "ph" -> when (status) {
                WaterQualityStatus.WARNING -> "Adjust pH to 6.5-8.5 range using lime or pH adjuster"
                WaterQualityStatus.CRITICAL -> "Urgent: pH is critical. Perform water exchange"
                else -> ""
            }
            "oxygen", "dissolved oxygen" -> when (status) {
                WaterQualityStatus.WARNING -> "Increase aeration or reduce stocking density"
                WaterQualityStatus.CRITICAL -> "Urgent: Add emergency aeration immediately"
                else -> ""
            }
            "ammonia" -> when (status) {
                WaterQualityStatus.WARNING -> "Reduce feeding and increase water exchange"
                WaterQualityStatus.CRITICAL -> "Urgent: Perform large water exchange and reduce feeding"
                else -> ""
            }
            "nitrate" -> when (status) {
                WaterQualityStatus.WARNING -> "Increase water exchange frequency"
                WaterQualityStatus.CRITICAL -> "Urgent: Perform water exchange and check biofilter"
                else -> ""
            }
            "turbidity" -> when (status) {
                WaterQualityStatus.WARNING -> "Improve filtration or reduce suspended solids"
                WaterQualityStatus.CRITICAL -> "Urgent: Clean filters and perform water exchange"
                else -> ""
            }
            else -> "Monitor and adjust as needed"
        }
    }

    fun getParameterRanges(): Map<String, String> {
        return mapOf(
            "Temperature" to "$TEMP_MIN - $TEMP_MAX °C",
            "pH" to "$PH_MIN - $PH_MAX",
            "Dissolved Oxygen" to "$DO_MIN - $DO_MAX mg/L",
            "Ammonia" to "$AMMONIA_MIN - $AMMONIA_MAX mg/L",
            "Nitrate" to "$NITRATE_MIN - $NITRATE_MAX mg/L",
            "Turbidity" to "$TURBIDITY_MIN - $TURBIDITY_MAX NTU"
        )
    }
}