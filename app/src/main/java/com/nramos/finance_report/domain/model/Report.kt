package com.nramos.finance_report.domain.model

data class Report(
    val reportId: String,
    val categoryId: String,
    val subcategoryId: String?,
    val modalityId: String,
    val concept: String?,
    val date: String,
    val amount: Double,
    val type: Char // 'I' = Ingreso, 'E' = Egreso
)