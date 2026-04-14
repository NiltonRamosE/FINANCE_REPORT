package com.nramos.finance_report.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.CategoryRepository
import com.nramos.finance_report.data.repository.ReportRepository
import com.nramos.finance_report.data.repository.SubcategoryRepository
import com.nramos.finance_report.domain.model.Report
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MovementsState())
    val state: StateFlow<MovementsState> = _state.asStateFlow()

    private var isLoadingReports = false

    init {
        loadReports()
    }

    fun onEvent(event: MovementsEvent) {
        when (event) {
            is MovementsEvent.OnFilterChanged -> {
                _state.update { it.copy(selectedFilter = event.filter) }
                loadReports(event.filter)
            }
        }
    }

    private fun loadReports(filter: String? = null) {
        if (isLoadingReports) return

        isLoadingReports = true
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val type = when (filter) {
                "income" -> 'I'
                "expense" -> 'E'
                else -> null
            }

            reportRepository.getReports(type = type).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        val reports = result.data ?: emptyList()

                        // Cargar nombres de categorías y subcategorías
                        val enrichedReports = enrichReportsWithNames(reports)

                        _state.update {
                            it.copy(
                                isLoading = false,
                                reports = enrichedReports,
                                error = null
                            )
                        }
                        isLoadingReports = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        isLoadingReports = false
                    }
                }
            }
        }
    }

    private suspend fun enrichReportsWithNames(reports: List<Report>): List<EnrichedReport> {
        if (reports.isEmpty()) return emptyList()

        // Obtener IDs únicos de categorías y subcategorías
        val categoryIds = reports.map { it.categoryId }.distinct()
        val subcategoryIds = reports.mapNotNull { it.subcategoryId }.distinct()

        // Cargar mapas de nombres
        val categoryNames = categoryRepository.getCategoriesMap(categoryIds)
        val subcategoryNames = subcategoryRepository.getSubcategoriesMap(subcategoryIds)

        // Enriquecer reportes
        return reports.map { report ->
            EnrichedReport(
                reportId = report.reportId,
                categoryId = report.categoryId,
                categoryName = categoryNames[report.categoryId] ?: "Categoría",
                subcategoryId = report.subcategoryId,
                subcategoryName = report.subcategoryId?.let { subcategoryNames[it] } ?: "Sin subcategoría",
                modalityId = report.modalityId,
                concept = report.concept,
                date = report.date,
                amount = report.amount,
                type = report.type
            )
        }
    }
}

data class EnrichedReport(
    val reportId: String,
    val categoryId: String,
    val categoryName: String,
    val subcategoryId: String?,
    val subcategoryName: String,
    val modalityId: String,
    val concept: String?,
    val date: String,
    val amount: Double,
    val type: Char
)

data class MovementsState(
    val reports: List<EnrichedReport> = emptyList(),
    val selectedFilter: String = "all",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MovementsEvent {
    data class OnFilterChanged(val filter: String) : MovementsEvent()
}