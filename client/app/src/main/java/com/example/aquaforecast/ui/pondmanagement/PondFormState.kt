package com.example.aquaforecast.ui.pondmanagement

import java.time.LocalDate

data class PondFormState(
    val isEditMode: Boolean = false,
    val editingPondId: Long? = null,

    // Form fields
    val pondName: String = "",
    val species: String = "",
    val stockCount: String = "",
    val startDate: LocalDate? = null,

    // UI state
    val isSpeciesDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
