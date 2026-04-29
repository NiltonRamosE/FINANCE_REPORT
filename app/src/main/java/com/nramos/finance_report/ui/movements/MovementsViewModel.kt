package com.nramos.finance_report.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.CategoryRepository
import com.nramos.finance_report.data.repository.ModalityRepository
import com.nramos.finance_report.data.repository.ReportRepository
import com.nramos.finance_report.data.repository.SubcategoryRepository
import com.nramos.finance_report.domain.model.Category
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
    private val subcategoryRepository: SubcategoryRepository,
    private val modalityRepository: ModalityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MovementsState())
    val state: StateFlow<MovementsState> = _state.asStateFlow()

    private var isLoadingReports = false

    init {
        loadReports()
        loadModalities()
        loadAllCategories()
    }

    fun onEvent(event: MovementsEvent) {
        when (event) {
            is MovementsEvent.OnFilterChanged -> {
                _state.update { it.copy(selectedFilter = event.filter) }
                loadReports(event.filter)
            }
            is MovementsEvent.OnEditReport -> {
                _state.update { it.copy(reportToEdit = event.report, showEditDialog = true) }
            }
            is MovementsEvent.OnDeleteReport -> {
                deleteReport(event.reportId)
            }
            is MovementsEvent.OnUpdateReport -> {
                updateReport(
                    event.reportId,
                    event.categoryId,
                    event.subcategoryId,
                    event.modalityId,
                    event.concept,
                    event.date,
                    event.amount,
                    event.type
                )
            }
            is MovementsEvent.OnCloseEditDialog -> {
                _state.update { it.copy(showEditDialog = false, reportToEdit = null) }
            }
        }
    }

    private fun loadModalities() {
        viewModelScope.launch {
            modalityRepository.getModalities().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update { it.copy(modalities = result.data ?: emptyList()) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            val allCategories = mutableListOf<Category>()

            categoryRepository.getCategoriesByType('I').collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        allCategories.addAll(result.data ?: emptyList())
                    }
                    else -> {}
                }
            }

            categoryRepository.getCategoriesByType('E').collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        allCategories.addAll(result.data ?: emptyList())
                        _state.update { it.copy(allCategories = allCategories) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadSubcategoriesForCategory(categoryId: String) {
        viewModelScope.launch {
            subcategoryRepository.getSubcategoriesByCategory(categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update { it.copy(availableSubcategories = result.data ?: emptyList()) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun deleteReport(reportId: String) {
        viewModelScope.launch {
            reportRepository.deleteReport(reportId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadReports(_state.value.selectedFilter)
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateReport(
        reportId: String,
        categoryId: String,
        subcategoryId: String?,
        modalityId: String,
        concept: String?,
        date: String,
        amount: Double,
        type: Char
    ) {
        viewModelScope.launch {
            reportRepository.updateReport(
                reportId, categoryId, subcategoryId, modalityId, concept, date, amount, type
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update { it.copy(showEditDialog = false, reportToEdit = null) }
                        loadReports(_state.value.selectedFilter)
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadSubcategories(categoryId: String) {
        loadSubcategoriesForCategory(categoryId)
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

        val categoryIds = reports.map { it.categoryId }.distinct()
        val subcategoryIds = reports
            .mapNotNull { it.subcategoryId }
            .filter { id ->
                id.isNotEmpty() && id != "null"
            }
            .distinct()

        val categoryNames = categoryRepository.getCategoriesMap(categoryIds)
        val subcategoryNames = subcategoryRepository.getSubcategoriesMap(subcategoryIds)
        return reports.map { report ->
            EnrichedReport(
                reportId = report.reportId,
                categoryId = report.categoryId,
                categoryName = categoryNames[report.categoryId] ?: "Categoría",
                subcategoryId = report.subcategoryId,
                subcategoryName = subcategoryNames[report.subcategoryId] ?: "Sin subcategoría",
                modalityId = report.modalityId,
                concept = report.concept,
                date = report.date,
                amount = report.amount,
                type = report.type
            )
        }
    }
}