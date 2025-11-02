package com.example.aquaforecast.ui.pondmanagement

import com.example.aquaforecast.domain.model.Pond

data class PondManagementState(
    val isLoading: Boolean = true,
    val ponds: List<Pond> = emptyList(),
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val pondToDelete: Pond? = null
)
