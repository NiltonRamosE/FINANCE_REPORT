package com.nramos.finance_report.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.ReminderRepository
import com.nramos.finance_report.domain.model.Reminder
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RemindersState())
    val state: StateFlow<RemindersState> = _state.asStateFlow()

    init {
        loadReminders()
    }

    fun onEvent(event: RemindersEvent) {
        when (event) {
            is RemindersEvent.LoadReminders -> loadReminders()
            is RemindersEvent.CreateReminder -> createReminder(
                event.title,
                event.description,
                event.dateTime,
                event.frequency
            )
            is RemindersEvent.UpdateReminder -> updateReminder(
                event.id,
                event.title,
                event.description,
                event.dateTime,
                event.frequency,
                event.isActive
            )
            is RemindersEvent.DeleteReminder -> deleteReminder(event.id)
            is RemindersEvent.ToggleReminderStatus -> toggleReminderStatus(event.id, event.isActive)
            is RemindersEvent.ShowCreateDialog -> _state.update { it.copy(showCreateDialog = true) }
            is RemindersEvent.HideCreateDialog -> _state.update { it.copy(showCreateDialog = false) }
            is RemindersEvent.ShowEditDialog -> _state.update {
                it.copy(showEditDialog = true, reminderToEdit = event.reminder)
            }
            is RemindersEvent.HideEditDialog -> _state.update { it.copy(showEditDialog = false, reminderToEdit = null) }
        }
    }

    private fun loadReminders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            reminderRepository.getReminders().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                reminders = result.data ?: emptyList(),
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createReminder(
        title: String,
        description: String?,
        dateTime: String,
        frequency: String
    ) {
        viewModelScope.launch {
            reminderRepository.createReminder(title, description, dateTime, frequency).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                showCreateDialog = false,
                                reminderCreated = result.data
                            )
                        }
                        loadReminders()
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateReminder(
        id: String,
        title: String,
        description: String?,
        dateTime: String,
        frequency: String,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            reminderRepository.updateReminder(id, title, description, dateTime, frequency, isActive).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                showEditDialog = false,
                                reminderToEdit = null,
                                reminderUpdated = result.data
                            )
                        }
                        loadReminders()
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun deleteReminder(id: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadReminders()
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun toggleReminderStatus(id: String, isActive: Boolean) {
        val reminder = _state.value.reminders.find { it.id == id }
        reminder?.let {
            updateReminder(
                it.id,
                it.title,
                it.description,
                it.dateTime,
                it.frequency,
                isActive
            )
        }
    }

    fun clearSuccess() {
        _state.update {
            it.copy(
                reminderCreated = null,
                reminderUpdated = null
            )
        }
    }
}