package com.nramos.finance_report.ui.statistics

import com.nramos.finance_report.domain.model.CategoryStat
import com.nramos.finance_report.domain.model.MonthlyStat
import com.nramos.finance_report.domain.model.SubcategoryStat

data class StatisticsState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val monthlyStats: List<MonthlyStat> = emptyList(),
    val topCategories: List<CategoryStat> = emptyList(),
    val topSubcategories: List<SubcategoryStat> = emptyList(),
    val filterType: FilterType = FilterType.MONTH,
    val isLoading: Boolean = false,
    val error: String? = null
)