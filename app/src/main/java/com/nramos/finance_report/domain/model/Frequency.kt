package com.nramos.finance_report.domain.model

enum class Frequency(val displayName: String, val value: String) {
    ONCE("Una sola vez", "once"),
    DAILY("Diario", "daily"),
    WEEKLY("Semanal", "weekly"),
    MONTHLY("Mensual", "monthly"),
    YEARLY("Anual", "yearly");

    companion object {
        fun fromValue(value: String): Frequency {
            return entries.find { it.value == value } ?: ONCE
        }

        fun getDisplayNames(): List<String> {
            return entries.map { it.displayName }
        }

        fun getValues(): List<String> {
            return entries.map { it.value }
        }
    }
}