package com.example.aquaforecast.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val pondRepository: PondRepository,
    private val predictionRepository: PredictionRepository,
    private val farmDataRepository: FarmDataRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load pond configuration
            pondRepository.get()
                .onSuccess { pond ->
                    _state.update { it.copy(pond = pond) }

                    // If pond exists, load its data
                    pond?.let { p ->
                        loadPondData(p.id.toString())
                    } ?: run {
                        // No pond configured
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = message
                        )
                    }
                }
        }
    }

    private suspend fun loadPondData(pondId: String) {
        // Load latest prediction
        predictionRepository.getLatest(pondId)
            .onSuccess { prediction ->
                _state.update { it.copy(latestPrediction = prediction) }
            }
            .onError { message ->
                // Log error but don't block the UI
                println("Error loading prediction: $message")
            }

        // Load latest farm data
        farmDataRepository.getLatest(pondId)
            .onSuccess { farmData ->
                _state.update {
                    it.copy(
                        latestFarmData = farmData,
                        isLoading = false
                    )
                }
            }
            .onError { message ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = message
                    )
                }
            }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            val pondId = _state.value.pond?.id?.toString()
            if (pondId != null) {
                loadPondData(pondId)
            }

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
