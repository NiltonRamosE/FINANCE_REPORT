package com.nramos.finance_report.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.ReportRepository
import com.nramos.finance_report.data.repository.CategoryRepository
import com.nramos.finance_report.data.repository.SubcategoryRepository
import com.nramos.finance_report.domain.model.CategoryStat
import com.nramos.finance_report.domain.model.MonthlyStat
import com.nramos.finance_report.domain.model.SubcategoryStat
import com.nramos.finance_report.domain.model.Report
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.core.graphics.toColorInt

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))

    init {
        loadStatistics(FilterType.MONTH)
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.OnFilterTypeChanged -> {
                loadStatistics(event.filterType)
            }
            is StatisticsEvent.OnCustomDateRange -> {
                loadStatisticsWithDateRange(event.startDate, event.endDate)
            }
        }
    }

    private fun loadStatistics(filterType: FilterType) {
        val (startDate, endDate) = getDateRange(filterType)
        loadReports(startDate, endDate, filterType)
    }

    private fun loadStatisticsWithDateRange(startDate: String, endDate: String) {
        loadReports(startDate, endDate, FilterType.CUSTOM)
    }

    private fun loadReports(startDate: String, endDate: String, filterType: FilterType) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, filterType = filterType) }

            reportRepository.getReports(startDate = startDate, endDate = endDate).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {}
                    is NetworkResult.Success -> {
                        val reports = result.data ?: emptyList()
                        processStatistics(reports)
                        _state.update { it.copy(isLoading = false) }
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun enrichTopCategories(categories: List<CategoryStat>): List<CategoryStat> {
        if (categories.isEmpty()) return emptyList()

        val categoryIds = categories.map { it.categoryId }.distinct()
        val categoryNames = categoryRepository.getCategoriesMap(categoryIds)

        return categories.map { category ->
            category.copy(categoryName = categoryNames[category.categoryId] ?: "Categoría ${category.categoryId.take(6)}")
        }
    }

    private suspend fun enrichTopSubcategories(subcategories: List<SubcategoryStat>): List<SubcategoryStat> {
        if (subcategories.isEmpty()) return emptyList()

        val subcategoryIds = subcategories.map { it.subcategoryId }.distinct()
        val categoryIds = subcategories.map { it.categoryName }.distinct()

        val subcategoryNames = subcategoryRepository.getSubcategoriesMap(subcategoryIds)
        val categoryNames = categoryRepository.getCategoriesMap(categoryIds)

        return subcategories.map { subcategory ->
            subcategory.copy(
                subcategoryName = subcategoryNames[subcategory.subcategoryId] ?: "Subcategoría ${subcategory.subcategoryId.take(6)}",
                categoryName = categoryNames[subcategory.categoryName] ?: "Categoría"
            )
        }
    }

    private fun processStatistics(reports: List<Report>) {
        viewModelScope.launch {
            // Separar ingresos y egresos
            val incomes = reports.filter { it.type == 'I' }
            val expenses = reports.filter { it.type == 'E' }

            // Totales
            val totalIncome = incomes.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }
            val balance = totalIncome - totalExpense

            // Estadísticas mensuales
            val monthlyStats = calculateMonthlyStats(reports)

            // Top 5 categorías de gastos (temporal con IDs)
            val rawTopCategories = calculateTopCategories(expenses)
            val topCategories = enrichTopCategories(rawTopCategories)

            // Top 5 subcategorías de gastos (temporal con IDs)
            val rawTopSubcategories = calculateTopSubcategories(expenses)
            val topSubcategories = enrichTopSubcategories(rawTopSubcategories)

            _state.update {
                it.copy(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = balance,
                    monthlyStats = monthlyStats,
                    topCategories = topCategories,
                    topSubcategories = topSubcategories
                )
            }
        }
    }

    private fun calculateMonthlyStats(reports: List<Report>): List<MonthlyStat> {
        val monthlyMap = mutableMapOf<String, Pair<Double, Double>>()

        reports.forEach { report ->
            val month = report.date.substring(0, 7)
            val monthName = formatMonth(month)
            val current = monthlyMap[monthName] ?: (0.0 to 0.0)

            if (report.type == 'I') {
                monthlyMap[monthName] = (current.first + report.amount) to current.second
            } else {
                monthlyMap[monthName] = current.first to (current.second + report.amount)
            }
        }

        return monthlyMap.map { (month, values) ->
            MonthlyStat(month, values.first, values.second)
        }.sortedBy { it.month }
    }

    private fun calculateTopCategories(expenses: List<Report>): List<CategoryStat> {
        val categoryMap = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            categoryMap[expense.categoryId] = (categoryMap[expense.categoryId] ?: 0.0) + expense.amount
        }

        val totalExpense = categoryMap.values.sum()
        val colors = listOf(
            "#FF6B6B".toColorInt(),
            "#4ECDC4".toColorInt(),
            "#45B7D1".toColorInt(),
            "#96CEB4".toColorInt(),
            "#FFEAA7".toColorInt()
        )

        return categoryMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapIndexed { index, entry ->
                CategoryStat(
                    categoryId = entry.key,
                    categoryName = entry.key,
                    totalAmount = entry.value,
                    percentage = ((entry.value / totalExpense) * 100).toFloat(),
                    color = colors[index % colors.size]
                )
            }
    }

    private fun calculateTopSubcategories(expenses: List<Report>): List<SubcategoryStat> {
        val subcategoryMap = mutableMapOf<String, Pair<String, Double>>()

        expenses.filter { it.subcategoryId != null && it.subcategoryId != "null" }
            .forEach { expense ->
                val current = subcategoryMap[expense.subcategoryId!!] ?: (expense.categoryId to 0.0)
                subcategoryMap[expense.subcategoryId!!] = current.first to (current.second + expense.amount)
            }

        val totalExpense = subcategoryMap.values.sumOf { it.second }

        return subcategoryMap.entries
            .sortedByDescending { it.value.second }
            .take(5)
            .map { (id, data) ->
                SubcategoryStat(
                    subcategoryId = id,
                    subcategoryName = id,
                    categoryName = data.first,
                    totalAmount = data.second,
                    percentage = ((data.second / totalExpense) * 100).toFloat()
                )
            }
    }

    private fun getDateRange(filterType: FilterType): Pair<String, String> {
        val endDate = dateFormat.format(calendar.time)

        return when (filterType) {
            FilterType.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                startDate to endDate
            }
            FilterType.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val startDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.MONTH, 1)
                startDate to endDate
            }
            FilterType.YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                val startDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.YEAR, 1)
                startDate to endDate
            }
            else -> {
                "1970-01-01" to endDate
            }
        }
    }

    private fun formatMonth(yearMonth: String): String {
        return try {
            val parts = yearMonth.split("-")
            val month = monthFormat.format(Date(parts[0].toInt() - 1900, parts[1].toInt() - 1, 1))
            "${month} ${parts[0]}"
        } catch (e: Exception) {
            yearMonth
        }
    }


}