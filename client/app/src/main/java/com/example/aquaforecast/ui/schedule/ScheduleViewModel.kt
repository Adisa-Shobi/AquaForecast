package com.example.aquaforecast.ui.schedule

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.data.workers.NotificationScheduler
import com.example.aquaforecast.domain.model.FeedingSchedule
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.repository.FeedingScheduleRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private const val TAG = "ScheduleViewModel"

class ScheduleViewModel(
    application: Application,
    private val feedingScheduleRepository: FeedingScheduleRepository,
    private val pondRepository: PondRepository
) : AndroidViewModel(application) {

    private val notificationScheduler = NotificationScheduler(application)

    private val _state = MutableStateFlow(ScheduleState())
    val state = _state.asStateFlow()

    init {
        loadPonds()
        observeSchedules()
    }

    /**
     * Refresh ponds list - call when navigating to schedule creation screen
     */
    fun refreshPonds() {
        loadPonds()
    }

    /**
     * Load available ponds for schedule creation
     */
    private fun loadPonds() {
        viewModelScope.launch {
            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    _state.update {
                        it.copy(
                            availablePonds = ponds
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            error = "Failed to load ponds: $message"
                        )
                    }
                }
        }
    }

    /**
     * Observe feeding schedules in real-time
     */
    private fun observeSchedules() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            feedingScheduleRepository.observeAllSchedules().collect { schedules ->
                _state.update {
                    it.copy(
                        schedules = schedules,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Prepare state for creating new schedule
     */
    fun prepareCreateSchedule() {
        val pond = _state.value.availablePonds.firstOrNull()
        if (pond == null) {
            _state.update { it.copy(error = "No pond configured. Please configure a pond first.") }
            return
        }

        _state.update {
            it.copy(
                isEditMode = false,
                selectedName = "",
                selectedPondId = pond.id,
                selectedPondName = pond.name,
                selectedStartDate = LocalDate.now(),
                selectedEndDate = LocalDate.now().plusMonths(1),
                selectedTime = LocalTime.of(7, 0),
                dialogError = null
            )
        }
    }

    /**
     * Prepare state for editing existing schedule
     */
    fun prepareEditSchedule(schedule: FeedingSchedule) {
        _state.update {
            it.copy(
                isEditMode = true,
                editingScheduleId = schedule.id,
                selectedName = schedule.name,
                selectedPondId = schedule.pondId,
                selectedPondName = schedule.pondName,
                selectedStartDate = schedule.startDate,
                selectedEndDate = schedule.endDate,
                selectedTime = schedule.feedingTime,
                dialogError = null
            )
        }
        Log.d(TAG, _state.value.selectedName.toString())
    }

    /**
     * Reset schedule form
     */
    fun resetScheduleForm() {
        _state.update {
            it.copy(
                isEditMode = false,
                editingScheduleId = null,
                selectedName = null,
                selectedPondId = null,
                selectedPondName = null,
                selectedStartDate = null,
                selectedEndDate = null,
                selectedTime = null,
                dialogError = null
            )
        }
    }

    /**
     * Update schedule name
     */
    fun onNameChanged(name: String) {
        _state.update { it.copy(selectedName = name) }
    }

    /**
     * Update selected pond
     */
    fun onPondSelected(pond: Pond) {
        _state.update {
            it.copy(
                selectedPondId = pond.id,
                selectedPondName = pond.name
            )
        }
    }

    /**
     * Update start date
     */
    fun onStartDateSelected(date: LocalDate) {
        _state.update { state ->
            val currentEndDate = state.selectedEndDate
            val newEndDate = if (currentEndDate != null && date.isAfter(currentEndDate)) {
                date.plusMonths(1)
            } else {
                currentEndDate
            }

            state.copy(
                selectedStartDate = date,
                selectedEndDate = newEndDate
            )
        }
    }

    /**
     * Update end date
     */
    fun onEndDateSelected(date: LocalDate) {
        val startDate = _state.value.selectedStartDate
        if (startDate != null && date.isBefore(startDate)) {
            _state.update { it.copy(dialogError = "End date must be after start date") }
            return
        }

        _state.update {
            it.copy(
                selectedEndDate = date,
                dialogError = null
            )
        }
    }

    /**
     * Update feeding time
     */
    fun onTimeSelected(time: LocalTime) {
        _state.update { it.copy(selectedTime = time) }
    }

    /**
     * Save schedule (create or update)
     */
    fun saveSchedule() {
        val currentState = _state.value

        // Validate inputs
        if (currentState.selectedName.isNullOrBlank()) {
            _state.update { it.copy(dialogError = "Please enter a schedule name") }
            return
        }
        if (currentState.selectedPondId == null || currentState.selectedPondName == null) {
            _state.update { it.copy(dialogError = "Please select a pond") }
            return
        }
        if (currentState.selectedStartDate == null) {
            _state.update { it.copy(dialogError = "Please select a start date") }
            return
        }
        if (currentState.selectedEndDate == null) {
            _state.update { it.copy(dialogError = "Please select an end date") }
            return
        }
        if (currentState.selectedTime == null) {
            _state.update { it.copy(dialogError = "Please select a feeding time") }
            return
        }
        if (currentState.selectedEndDate.isBefore(currentState.selectedStartDate)) {
            _state.update { it.copy(dialogError = "End date must be after start date") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, dialogError = null) }

            if (currentState.isEditMode && currentState.editingScheduleId != null) {
                // Update existing schedule
                val updatedSchedule = FeedingSchedule(
                    id = currentState.editingScheduleId,
                    name = currentState.selectedName,
                    pondId = currentState.selectedPondId,
                    pondName = currentState.selectedPondName,
                    startDate = currentState.selectedStartDate,
                    endDate = currentState.selectedEndDate,
                    feedingTime = currentState.selectedTime,
                    isActive = true
                )

                feedingScheduleRepository.updateSchedule(updatedSchedule)
                    .onSuccess {
                        // Schedule notifications for the updated schedule
                        notificationScheduler.scheduleNotifications(updatedSchedule)

                        _state.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Schedule updated successfully"
                            )
                        }
                        resetScheduleForm()
                        clearSuccessMessage()
                    }
                    .onError { message ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                dialogError = "Failed to update schedule: $message"
                            )
                        }
                    }
            } else {
                // Create new schedule
                feedingScheduleRepository.createSchedule(
                    name = currentState.selectedName,
                    pondId = currentState.selectedPondId,
                    pondName = currentState.selectedPondName,
                    startDate = currentState.selectedStartDate,
                    endDate = currentState.selectedEndDate,
                    feedingTime = currentState.selectedTime
                )
                    .onSuccess { scheduleId ->
                        // Get the created schedule and schedule notifications
                        viewModelScope.launch {
                            feedingScheduleRepository.getScheduleById(scheduleId)
                                .onSuccess { schedule ->
                                    schedule?.let {
                                        notificationScheduler.scheduleNotifications(it)
                                    }
                                }
                        }

                        _state.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Schedule created successfully"
                            )
                        }
                        resetScheduleForm()
                        clearSuccessMessage()
                    }
                    .onError { message ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                dialogError = "Failed to create schedule: $message"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Delete schedule
     */
    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            // Cancel notifications for this schedule
            notificationScheduler.cancelNotifications(scheduleId)

            feedingScheduleRepository.deleteSchedule(scheduleId)
                .onSuccess {
                    _state.update {
                        it.copy(successMessage = "Schedule deleted successfully")
                    }
                    clearSuccessMessage()
                }
                .onError { message ->
                    _state.update {
                        it.copy(error = "Failed to delete schedule: $message")
                    }
                }
        }
    }

    /**
     * Toggle schedule active status
     */
    fun toggleScheduleActive(scheduleId: Long, isActive: Boolean) {
        viewModelScope.launch {
            feedingScheduleRepository.toggleScheduleActive(scheduleId, !isActive)
                .onSuccess {
                    // Update notifications based on new active status
                    feedingScheduleRepository.getScheduleById(scheduleId)
                        .onSuccess { schedule ->
                            schedule?.let {
                                if (it.isActive) {
                                    // Schedule notifications if now active
                                    notificationScheduler.scheduleNotifications(it)
                                } else {
                                    // Cancel notifications if now inactive
                                    notificationScheduler.cancelNotifications(scheduleId)
                                }
                            }
                        }
                }
                .onError { message ->
                    _state.update {
                        it.copy(error = "Failed to toggle schedule: $message")
                    }
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clear success message after delay
     */
    private fun clearSuccessMessage() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _state.update { it.copy(successMessage = null) }
        }
    }
}

data class ScheduleState(
    val schedules: List<FeedingSchedule> = emptyList(),
    val availablePonds: List<Pond> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Form state
    val isEditMode: Boolean = false,
    val editingScheduleId: Long? = null,
    val selectedName: String? = null,
    val selectedPondId: Long? = null,
    val selectedPondName: String? = null,
    val selectedStartDate: LocalDate? = null,
    val selectedEndDate: LocalDate? = null,
    val selectedTime: LocalTime? = null,
    val dialogError: String? = null
)
