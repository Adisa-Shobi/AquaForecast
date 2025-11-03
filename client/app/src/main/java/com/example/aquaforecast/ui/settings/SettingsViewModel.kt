package com.example.aquaforecast.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.data.preferences.PreferencesManager
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.ModelRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.SyncRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

private const val TAG = "SettingsViewModel"
class SettingsViewModel(
    private val pondRepository: PondRepository,
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val syncRepository: SyncRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        loadPondConfig()
        loadForecastHorizon()
        loadOfflineMode()
        observeAuthState()
        loadModelVersion()
    }

    private fun loadPondConfig() {
        viewModelScope.launch {
            pondRepository.getPondConfig()
                .onSuccess { config ->
                    config?.let {
                        _state.update { state ->
                            state.copy(
                                pondName = it.name,
                                species = it.species.displayName,
                                stockCount = it.stockCount.toString(),
                                startDate = it.startDate,
                                isConfigured = true
                            )
                        }
                    }
                }
        }
    }

    private fun loadForecastHorizon() {
        viewModelScope.launch {
            preferencesManager.forecastHorizon.collect { horizon ->
                _state.update { it.copy(forecastHorizon = horizon.toString()) }
            }
        }
    }

    private fun loadOfflineMode() {
        viewModelScope.launch {
            preferencesManager.offlineMode.collect { isOffline ->
                _state.update { it.copy(isOfflineMode = isOffline) }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                _state.update {
                    it.copy(
                        isAuthenticated = firebaseUser != null,
                        userEmail = firebaseUser?.email
                    )
                }
            }
        }
    }

    private fun loadModelVersion() {
        viewModelScope.launch {
            preferencesManager.modelVersion.collect { version ->
                _state.update { it.copy(modelVersion = version) }
            }
        }
    }

    fun onPondNameChanged(name: String) {
        _state.update {
            it.copy(
                pondName = name,
                pondNameError = null
            )
        }
    }

    fun onSpeciesChanged(species: String) {
        _state.update {
            it.copy(
                species = species,
                speciesError = null
            )
        }
    }

    fun onStockCountChanged(count: String) {
        val filteredCount = count.filter { it.isDigit() }
        _state.update {
            it.copy(
                stockCount = filteredCount,
                stockCountError = null
            )
        }
    }

    fun onForecastHorizonChanged(days: String) {
        val filteredDays = days.filter { it.isDigit() }
        _state.update {
            it.copy(
                forecastHorizon = filteredDays,
                forecastHorizonError = null
            )
        }
    }

    fun onStartDateChanged(millis: Long) {
        val date = Instant
            .ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        Log.d(TAG, date.toString())

        // Validate date
        if (date.isAfter(java.time.LocalDate.now())) {
            _state.update { it.copy(
                startDateError = "Cannot set date in the future"
            ) }
            return
        }

        if (date.isBefore(java.time.LocalDate.now().minusYears(2))) {
            _state.update { it.copy(
                startDateError = "Start Date cannot be older than 2 years"
            ) }
            return
        }

        _state.update {
            it.copy(
                startDate = date,
                startDateError = null
            )
        }
    }

    fun toggleSpeciesDropdown() {
        _state.update {
            it.copy(isSpeciesDropdownExpanded = !it.isSpeciesDropdownExpanded)
        }
    }

    fun dismissSpeciesDropdown() {
        _state.update {
            it.copy(isSpeciesDropdownExpanded = false)
        }
    }

    fun toggleDatePicker() {
        _state.update {
            it.copy(showDatePicker = !it.showDatePicker)
        }
    }

    fun dismissDatePicker() {
        _state.update {
            it.copy(showDatePicker = false)
        }
    }

    fun toggleOfflineMode() {
        viewModelScope.launch {
            val newValue = !_state.value.isOfflineMode
            preferencesManager.setOfflineMode(newValue)
            _state.update {
                it.copy(isOfflineMode = newValue)
            }
        }
    }

    fun savePondConfig() {
        val currentState = _state.value

        val pondNameError = validatePondName(currentState.pondName)
        val speciesError = validateSpecies(currentState.species)
        val stockCountError = validateStockCount(currentState.stockCount)
        val startDateError = validateStartDate(currentState.startDate)
        val forecastHorizonError = validateForecastHorizon(currentState.forecastHorizon)

        if (pondNameError != null || speciesError != null ||
            stockCountError != null || startDateError != null || forecastHorizonError != null) {
            _state.update {
                it.copy(
                    pondNameError = pondNameError,
                    speciesError = speciesError,
                    stockCountError = stockCountError,
                    startDateError = startDateError,
                    forecastHorizonError = forecastHorizonError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Save pond config
            pondRepository.savePondConfig(
                pondName = currentState.pondName.trim(),
                species = currentState.species,
                initialStockCount = currentState.stockCount.toInt(),
                startDate = currentState.startDate ?: LocalDate.now()
            ).onSuccess {
                // Save forecast horizon to preferences
                preferencesManager.setForecastHorizon(currentState.forecastHorizon.toInt())

                _state.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        isConfigured = true
                    )
                }
            }.onError { message ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = message
                    )
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncing = true,
                    syncSuccess = false,
                    syncMessage = "Checking for unsynced data...",
                    syncedCount = null,
                    syncingCount = null
                )
            }

            // First, get the count of unsynced data
            syncRepository.getUnsyncedCount()
                .onSuccess { unsyncedCount ->
                    if (unsyncedCount == 0) {
                        // No data to sync
                        _state.update {
                            it.copy(
                                isSyncing = false,
                                syncSuccess = true,
                                syncMessage = "No new data to sync",
                                syncedCount = 0,
                                syncingCount = null
                            )
                        }
                        return@launch
                    }

                    // Update message with count
                    _state.update {
                        it.copy(
                            syncMessage = "Syncing $unsyncedCount ${if (unsyncedCount == 1) "entry" else "entries"}...",
                            syncingCount = unsyncedCount
                        )
                    }

                    // Perform the sync
                    syncRepository.syncData()
                        .onSuccess { count ->
                            Log.d(TAG, "Successfully synced $count entries")
                            _state.update {
                                it.copy(
                                    isSyncing = false,
                                    syncSuccess = true,
                                    syncMessage = "Successfully synced $count ${if (count == 1) "entry" else "entries"}",
                                    syncedCount = count,
                                    syncingCount = null
                                )
                            }
                        }
                        .onError { message ->
                            Log.e(TAG, "Sync failed: $message")
                            _state.update {
                                it.copy(
                                    isSyncing = false,
                                    syncSuccess = false,
                                    syncMessage = message,
                                    syncedCount = null,
                                    syncingCount = null
                                )
                            }
                        }
                }
                .onError { message ->
                    Log.e(TAG, "Failed to get unsynced count: $message")
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            syncSuccess = false,
                            syncMessage = "Failed to prepare sync: $message",
                            syncedCount = null,
                            syncingCount = null
                        )
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun saveForecastSettings() {
        val currentState = _state.value
        val forecastHorizonError = validateForecastHorizon(currentState.forecastHorizon)

        if (forecastHorizonError != null) {
            _state.update {
                it.copy(forecastHorizonError = forecastHorizonError)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                // Save forecast horizon to preferences
                preferencesManager.setForecastHorizon(currentState.forecastHorizon.toInt())

                _state.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save forecast settings: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    fun clearSyncMessage() {
        _state.update {
            it.copy(
                syncSuccess = false,
                syncMessage = null,
                syncedCount = null
            )
        }
    }

    fun checkForModelUpdate() {
        viewModelScope.launch {
            _state.update { it.copy(isCheckingModel = true, modelUpdateMessage = null) }

            modelRepository.checkForModelUpdate()
                .onSuccess { newVersion ->
                    if (newVersion != null) {
                        _state.update {
                            it.copy(
                                isCheckingModel = false,
                                modelUpdateAvailable = true,
                                availableModelVersion = newVersion.version,
                                modelUpdateMessage = "Version ${newVersion.version} available"
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isCheckingModel = false,
                                modelUpdateAvailable = false,
                                modelUpdateMessage = "You have the latest model"
                            )
                        }
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isCheckingModel = false,
                            modelUpdateMessage = "Check failed: $message"
                        )
                    }
                }
        }
    }

    fun updateModel() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isUpdatingModel = true,
                    modelUpdateMessage = "Downloading model..."
                )
            }

            modelRepository.updateModel()
                .onSuccess { newVersion ->
                    _state.update {
                        it.copy(
                            isUpdatingModel = false,
                            modelUpdateAvailable = false,
                            modelVersion = newVersion.version,
                            modelUpdateMessage = "Model updated to ${newVersion.version}"
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isUpdatingModel = false,
                            modelUpdateMessage = "Update failed: $message"
                        )
                    }
                }
        }
    }

    fun clearModelUpdateMessage() {
        _state.update {
            it.copy(
                modelUpdateMessage = null,
                modelUpdateAvailable = false
            )
        }
    }

    private fun validatePondName(name: String): String? {
        return when {
            name.isBlank() -> "Pond name is required"
            name.length < 3 -> "Pond name must be at least 3 characters"
            name.length > 50 -> "Pond name is too long"
            else -> null
        }
    }

    private fun validateSpecies(species: String): String? {
        return when {
            species.isBlank() -> "Please select a species"
            species !in listOf("Tilapia", "Catfish") -> "Invalid species"
            else -> null
        }
    }

    private fun validateStockCount(count: String): String? {
        return when {
            count.isBlank() -> "Stock count is required"
            count.toIntOrNull() == null -> "Invalid stock count"
            count.toInt() < 1 -> "Stock count must be at least 1"
            count.toInt() > 100000 -> "Stock count is too large"
            else -> null
        }
    }

    private fun validateStartDate(date: LocalDate?): String? {
        return when {
            date == null -> "Start date is required"
            date.isAfter(LocalDate.now()) -> "Start date cannot be in the future"
            date.isBefore(LocalDate.now().minusYears(2)) -> "Start date is too old"
            else -> null
        }
    }

    private fun validateForecastHorizon(days: String): String? {
        return when {
            days.isBlank() -> "Forecast horizon is required"
            days.toIntOrNull() == null -> "Invalid forecast horizon"
            days.toInt() < 1 -> "Forecast horizon must be at least 1 day"
            days.toInt() > 180 -> "Forecast horizon cannot exceed 180 days"
            else -> null
        }
    }
}

data class SettingsState(
    val pondName: String = "",
    val species: String = "",
    val stockCount: String = "",
    val startDate: LocalDate? = LocalDate.now(),
    val forecastHorizon: String = "20", // Default: 20 days
    val pondNameError: String? = null,
    val speciesError: String? = null,
    val stockCountError: String? = null,
    val startDateError: String? = null,
    val forecastHorizonError: String? = null,
    val isSpeciesDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val isSaving: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val isConfigured: Boolean = false,
    val syncSuccess: Boolean = false,
    val syncMessage: String? = null,
    val syncedCount: Int? = null,
    val syncingCount: Int? = null, // Number of entries currently being synced
    // Model versioning
    val modelVersion: String = "built-in",
    val isCheckingModel: Boolean = false,
    val isUpdatingModel: Boolean = false,
    val modelUpdateAvailable: Boolean = false,
    val availableModelVersion: String? = null,
    val modelUpdateMessage: String? = null
)