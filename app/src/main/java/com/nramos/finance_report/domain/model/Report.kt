package com.nramos.finance_report.domain.model

data class Report(
    val reportId: String,
    val categoryId: String,
    val categoryName: String,
    val subcategoryId: String?,
    val subcategoryName: String?,
    val modalityId: String,
    val modalityName: String,
    val concept: String?,
    val date: String,
    val amount: Double,
    val type: Char // 'I' = Ingreso, 'E' = Egreso
)