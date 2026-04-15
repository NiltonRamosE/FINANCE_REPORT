package com.nramos.finance_report.ui.movements

data class EnrichedReport(
    val reportId: String,
    val categoryId: String,
    val categoryName: String,
    val subcategoryId: String?,
    val subcategoryName: String,
    val modalityId: String,
    val concept: String?,
    val date: String,
    val amount: Double,
    val type: Char
)