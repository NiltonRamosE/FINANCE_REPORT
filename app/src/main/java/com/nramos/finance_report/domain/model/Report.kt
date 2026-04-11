package com.nramos.finance_report.domain.model

data class Report(
    val reportId: Long,
    val categoryId: Long,
    val categoryName: String,
    val subcategoryId: Long?,
    val subcategoryName: String?,
    val modalityId: Long,
    val modalityName: String,
    val concept: String?,
    val date: String,
    val amount: Double,
    val type: Char // 'I' = Ingreso, 'E' = Egreso
)