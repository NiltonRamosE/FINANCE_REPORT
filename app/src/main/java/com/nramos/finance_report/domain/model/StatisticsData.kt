package com.nramos.finance_report.domain.model

data class CategoryStat(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val color: Int
)

data class SubcategoryStat(
    val subcategoryId: String,
    val subcategoryName: String,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthlyStat(
    val month: String,
    val income: Double,
    val expense: Double
)