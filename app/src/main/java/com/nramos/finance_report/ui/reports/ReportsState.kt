package com.nramos.finance_report.ui.reports

import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Subcategory

data class ReportsState(
    val selectedType: Char = 'I',
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isLoadingCategories: Boolean = false,
    val isCreatingCategory: Boolean = false,
    val showDialog: Boolean = false,
    val categoryCreated: Category? = null,
    val subcategories: List<Subcategory> = emptyList(),
    val selectedSubcategory: Subcategory? = null,
    val isLoadingSubcategories: Boolean = false,
    val isCreatingSubcategory: Boolean = false,
    val showSubcategoryDialog: Boolean = false,
    val subcategoryCreated: Subcategory? = null,
    val error: String? = null
)