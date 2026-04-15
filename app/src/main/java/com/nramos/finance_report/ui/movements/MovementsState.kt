package com.nramos.finance_report.ui.movements

data class MovementsState(
    val reports: List<EnrichedReport> = emptyList(),
    val selectedFilter: String = "all",
    val isLoading: Boolean = false,
    val error: String? = null
)