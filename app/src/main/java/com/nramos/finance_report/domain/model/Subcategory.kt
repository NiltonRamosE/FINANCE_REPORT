package com.nramos.finance_report.domain.model

data class Subcategory(
    val subCategoryId: String,
    val categoryId: String,
    val name: String,
){
    override fun toString(): String {
        return name
    }
}