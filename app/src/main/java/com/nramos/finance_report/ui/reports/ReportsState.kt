package com.nramos.finance_report.ui.reports

import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Modality
import com.nramos.finance_report.domain.model.Report
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
    val modalities: List<Modality> = emptyList(),
    val selectedModality: Modality? = null,
    val isLoadingModalities: Boolean = false,
    val selectedDate: String = "",
    val concept: String = "",
    val amount: Double = 0.0,
    val amountText: String = "",
    val isSavingReport: Boolean = false,
    val reportSaved: Report? = null,
    val error: String? = null
)