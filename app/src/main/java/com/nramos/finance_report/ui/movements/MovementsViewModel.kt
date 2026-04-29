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
import kotlinx.coroutines.async
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
    private val _allReports = mutableListOf<EnrichedReport>()
    private var currentFilterType: String? = null
    private var currentSearchQuery: String = ""
    private var isLoadingReports = false

    init {
        loadAllData()
    }

    fun onEvent(event: MovementsEvent) {
        when (event) {
            is MovementsEvent.OnFilterChanged -> {
                if (_state.value.selectedFilter != event.filter) {
                    _state.update { it.copy(selectedFilter = event.filter, isLoading = true) }
                    currentFilterType = if (event.filter == "all") null else event.filter
                    applyFiltersAndSearch()
                }
            }
            is MovementsEvent.OnSearchQueryChanged -> {
                currentSearchQuery = event.query.lowercase().trim()
                applyFiltersAndSearch()
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

    private fun loadAllData() {
        if (isLoadingReports) return
        isLoadingReports = true

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Cargar modalidades y categorías en paralelo
            val modalitiesDeferred = viewModelScope.async { loadModalitiesAsync() }
            val categoriesDeferred = viewModelScope.async { loadAllCategoriesAsync() }

            modalitiesDeferred.await()
            categoriesDeferred.await()

            // Cargar todos los reportes sin filtro de tipo
            loadAllReports()
        }
    }

    private suspend fun loadModalitiesAsync() {
        modalityRepository.getModalities().collect { result ->
            if (result is NetworkResult.Success) {
                _state.update { it.copy(modalities = result.data ?: emptyList()) }
            }
        }
    }

    private suspend fun loadAllCategoriesAsync() {
        val allCategories = mutableListOf<Category>()

        categoryRepository.getCategoriesByType('I').collect { result ->
            if (result is NetworkResult.Success) {
                allCategories.addAll(result.data ?: emptyList())
            }
        }

        categoryRepository.getCategoriesByType('E').collect { result ->
            if (result is NetworkResult.Success) {
                allCategories.addAll(result.data ?: emptyList())
                _state.update { it.copy(allCategories = allCategories) }
            }
        }
    }

    private fun loadAllReports() {
        viewModelScope.launch {
            reportRepository.getReports().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        val reports = result.data ?: emptyList()
                        val enrichedReports = enrichReportsWithNames(reports)

                        _allReports.clear()
                        _allReports.addAll(enrichedReports)

                        // Aplicar filtros después de cargar
                        applyFiltersAndSearch()

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
                    else -> {}
                }
            }
        }
    }

    private fun applyFiltersAndSearch() {
        val typeFiltered = when (currentFilterType) {
            "income" -> _allReports.filter { it.type == 'I' }
            "expense" -> _allReports.filter { it.type == 'E' }
            else -> _allReports.toList()
        }

        // Luego filtrar por búsqueda
        val finalFiltered = if (currentSearchQuery.isEmpty()) {
            typeFiltered
        } else {
            typeFiltered.filter { report ->
                report.concept?.lowercase()?.contains(currentSearchQuery) == true ||
                        report.categoryName.lowercase().contains(currentSearchQuery) ||
                        report.subcategoryName.lowercase().contains(currentSearchQuery)
            }
        }

        _state.update {
            it.copy(
                reports = finalFiltered,
                isLoading = false
            )
        }
    }

    private fun deleteReport(reportId: String) {
        viewModelScope.launch {
            reportRepository.deleteReport(reportId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadAllReports()
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
                        loadAllReports()
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

    fun updateSearchQuery(query: String) {
        currentSearchQuery = query.lowercase().trim()
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val filteredReports = if (currentSearchQuery.isEmpty()) {
            _allReports
        } else {
            _allReports.filter { report ->
                report.concept?.lowercase()?.contains(currentSearchQuery) == true ||
                        report.categoryName.lowercase().contains(currentSearchQuery) ||
                        report.subcategoryName.lowercase().contains(currentSearchQuery)
            }
        }

        _state.update { it.copy(reports = filteredReports) }
    }

    private suspend fun enrichReportsWithNames(reports: List<Report>): List<EnrichedReport> {
        if (reports.isEmpty()) return emptyList()

        val categoryIds = reports.map { it.categoryId }.distinct()
        val subcategoryIds = reports
            .mapNotNull { it.subcategoryId }
            .filter { id -> id.isNotEmpty() && id != "null" }
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