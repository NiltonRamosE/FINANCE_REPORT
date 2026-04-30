package com.nramos.finance_report.ui.reminders

import com.nramos.finance_report.domain.model.Reminder

data class RemindersState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val reminderToEdit: Reminder? = null,
    val reminderCreated: Reminder? = null,
    val reminderUpdated: Reminder? = null,
    val error: String? = null
)