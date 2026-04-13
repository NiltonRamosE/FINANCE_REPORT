package com.nramos.finance_report.domain.model

data class Category(
    val categoryId: String,
    val userId: String,
    val name: String,
    val inputType: Char,
    val subcategories: List<Subcategory> = emptyList()
) {
    override fun toString(): String {
        return name
    }
}