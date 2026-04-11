package com.nramos.finance_report.domain.model

data class Category(
    val categoryId: Long,
    val userId: Long,
    val name: String,
    val inputType: Char, // 'I' = Ingreso, 'E' = Egreso
    val subcategories: List<Subcategory> = emptyList()
)