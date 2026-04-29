package com.nramos.finance_report.ui.movements

sealed class MovementsEvent {
    data class OnFilterChanged(val filter: String) : MovementsEvent()
    data class OnSearchQueryChanged(val query: String) : MovementsEvent()
    data class OnEditReport(val report: EnrichedReport) : MovementsEvent()
    data class OnDeleteReport(val reportId: String) : MovementsEvent()
    data class OnUpdateReport(
        val reportId: String,
        val categoryId: String,
        val subcategoryId: String?,
        val modalityId: String,
        val concept: String?,
        val date: String,
        val amount: Double,
        val type: Char
    ) : MovementsEvent()
    object OnCloseEditDialog : MovementsEvent()
}