package com.nramos.finance_report.ui.reminders

import com.nramos.finance_report.domain.model.Reminder

sealed class RemindersEvent {
    object LoadReminders : RemindersEvent()
    data class CreateReminder(
        val title: String,
        val description: String?,
        val dateTime: String,
        val frequency: String
    ) : RemindersEvent()
    data class UpdateReminder(
        val id: String,
        val title: String,
        val description: String?,
        val dateTime: String,
        val frequency: String,
        val isActive: Boolean
    ) : RemindersEvent()
    data class DeleteReminder(val id: String) : RemindersEvent()
    data class ToggleReminderStatus(val id: String, val isActive: Boolean) : RemindersEvent()
    object ShowCreateDialog : RemindersEvent()
    object HideCreateDialog : RemindersEvent()
    data class ShowEditDialog(val reminder: Reminder) : RemindersEvent()
    object HideEditDialog : RemindersEvent()
}