package com.example.aquaforecast.ui.pondmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Species
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class PondFormViewModel(
    private val pondRepository: PondRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PondFormState())
    val state = _state.asStateFlow()

    fun loadPond(pondId: Long) {
        viewModelScope.launch {
            pondRepository.getPondById(pondId)
                .onSuccess { pond ->
                    pond?.let {
                        _state.update { state ->
                            state.copy(
                                isEditMode = true,
                                editingPondId = it.id,
                                pondName = it.name,
                                species = it.species.name,
                                stockCount = it.stockCount.toString(),
                                startDate = it.startDate
                            )
                        }
                    }
                }
                .onError { message ->
                    _state.update { it.copy(error = message) }
                }
        }
    }

    fun onPondNameChanged(name: String) {
        _state.update { it.copy(pondName = name) }
    }

    fun onSpeciesChanged(species: String) {
        _state.update { it.copy(species = species) }
    }

    fun onStockCountChanged(count: String) {
        // Only allow numbers
        if (count.isEmpty() || count.all { it.isDigit() }) {
            _state.update { it.copy(stockCount = count) }
        }
    }

    fun onStartDateChanged(millis: Long) {
        val localDate = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        _state.update { it.copy(startDate = localDate, showDatePicker = false) }
    }

    fun toggleSpeciesDropdown() {
        _state.update { it.copy(isSpeciesDropdownExpanded = !it.isSpeciesDropdownExpanded) }
    }

    fun dismissSpeciesDropdown() {
        _state.update { it.copy(isSpeciesDropdownExpanded = false) }
    }

    fun toggleDatePicker() {
        _state.update { it.copy(showDatePicker = !it.showDatePicker) }
    }

    fun dismissDatePicker() {
        _state.update { it.copy(showDatePicker = false) }
    }

    fun savePond() {
        viewModelScope.launch {
            val currentState = _state.value

            // Validation
            if (currentState.pondName.isBlank()) {
                _state.update { it.copy(error = "Pond name is required") }
                return@launch
            }

            if (currentState.species.isBlank()) {
                _state.update { it.copy(error = "Species is required") }
                return@launch
            }

            val stockCount = currentState.stockCount.toIntOrNull()
            if (stockCount == null || stockCount <= 0) {
                _state.update { it.copy(error = "Valid stock count is required") }
                return@launch
            }

            if (currentState.startDate == null) {
                _state.update { it.copy(error = "Start date is required") }
                return@launch
            }

            _state.update { it.copy(isSaving = true, error = null) }

            val species = when (currentState.species) {
                "CATFISH", "Catfish" -> Species.CATFISH
                "TILAPIA", "Tilapia" -> Species.TILAPIA
                else -> Species.CATFISH // Default
            }

            val pond = Pond(
                id = currentState.editingPondId ?: 0,
                name = currentState.pondName,
                species = species,
                stockCount = stockCount,
                startDate = currentState.startDate,
                createdAt = System.currentTimeMillis()
            )

            if (currentState.isEditMode && currentState.editingPondId != null) {
                // Update existing pond
                pondRepository.update(pond)
                    .onSuccess {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Pond updated successfully"
                            )
                        }
                    }
                    .onError { message ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = message
                            )
                        }
                    }
            } else {
                // Create new pond
                pondRepository.save(pond)
                    .onSuccess {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Pond created successfully"
                            )
                        }
                    }
                    .onError { message ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = message
                            )
                        }
                    }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun clearForm() {
        _state.value = PondFormState()
    }
}
