package com.nramos.finance_report.ui.reports

import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Subcategory

sealed class ReportsEvent {
    data class OnTypeSelected(val type: Char) : ReportsEvent()
    data class OnCategorySelected(val category: Category) : ReportsEvent()
    object OnCreateCategory : ReportsEvent()
    data class OnCategoryCreated(val category: Category) : ReportsEvent()

    object OnCreateSubcategory : ReportsEvent()
    data class OnSubcategorySelected(val subcategory: Subcategory) : ReportsEvent()
    data class OnSubcategoryCreated(val subcategory: Subcategory) : ReportsEvent()
    data class OnDateSelected(val date: String) : ReportsEvent()
}