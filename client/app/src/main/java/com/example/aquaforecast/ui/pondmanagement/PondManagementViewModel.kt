package com.example.aquaforecast.ui.pondmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PondManagementViewModel(
    private val pondRepository: PondRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PondManagementState())
    val state = _state.asStateFlow()

    init {
        loadPonds()
    }

    fun loadPonds() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            ponds = ponds
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
    }

    fun showDeleteDialog(pond: Pond) {
        _state.update {
            it.copy(
                showDeleteDialog = true,
                pondToDelete = pond
            )
        }
    }

    fun dismissDeleteDialog() {
        _state.update {
            it.copy(
                showDeleteDialog = false,
                pondToDelete = null
            )
        }
    }

    fun deletePond() {
        viewModelScope.launch {
            val pond = _state.value.pondToDelete ?: return@launch

            pondRepository.deletePondById(pond.id)
                .onSuccess {
                    dismissDeleteDialog()
                    loadPonds() // Reload list
                }
                .onError { message ->
                    _state.update { it.copy(error = message) }
                    dismissDeleteDialog()
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun togglePondHarvest(pond: Pond) {
        viewModelScope.launch {
            val result = if (pond.isHarvested) {
                pondRepository.markPondAsNotHarvested(pond.id)
            } else {
                pondRepository.markPondAsHarvested(pond.id)
            }

            result
                .onSuccess {
                    loadPonds() // Reload list to show updated status
                }
                .onError { message ->
                    _state.update { it.copy(error = message) }
                }
        }
    }
}
