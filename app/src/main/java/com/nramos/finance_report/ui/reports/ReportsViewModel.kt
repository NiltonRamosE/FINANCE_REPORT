package com.nramos.finance_report.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.CategoryRepository
import com.nramos.finance_report.data.repository.ModalityRepository
import com.nramos.finance_report.data.repository.ReportRepository
import com.nramos.finance_report.data.repository.SubcategoryRepository
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val modalityRepository: ModalityRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private var isLoadingCategories = false
    private var isCreatingCategory = false
    private var isLoadingSubcategories = false
    private var isCreatingSubcategory = false
    private var isLoadingModalities = false
    private var isSavingReport = false
    init {
        // Cargar modalidades al iniciar
        loadModalities()
    }
    fun onEvent(event: ReportsEvent) {
        when (event) {
            is ReportsEvent.OnTypeSelected -> {
                if (_state.value.selectedType != event.type) {
                    _state.update {
                        it.copy(
                            selectedType = event.type,
                            selectedCategory = null,
                            categories = emptyList(),
                            selectedSubcategory = null,
                            subcategories = emptyList()
                        )
                    }
                    loadCategories(event.type)
                }
            }
            is ReportsEvent.OnCategorySelected -> {
                _state.update { it.copy(selectedCategory = event.category, selectedSubcategory = null) }
                // Cargar subcategorías cuando se selecciona una categoría
                loadSubcategories(event.category.categoryId)
            }
            is ReportsEvent.OnCreateCategory -> {
                if (!_state.value.showDialog) {
                    _state.update { it.copy(showDialog = true) }
                }
            }
            is ReportsEvent.OnCategoryCreated -> {
                _state.update { it.copy(selectedCategory = event.category) }
                loadCategories(_state.value.selectedType)
            }
            is ReportsEvent.OnCreateSubcategory -> {
                if (!_state.value.showSubcategoryDialog) {
                    _state.update { it.copy(showSubcategoryDialog = true) }
                }
            }
            is ReportsEvent.OnSubcategorySelected -> {
                _state.update { it.copy(selectedSubcategory = event.subcategory) }
            }
            is ReportsEvent.OnSubcategoryCreated -> {
                _state.update { it.copy(selectedSubcategory = event.subcategory) }
                if (_state.value.selectedCategory != null) {
                    loadSubcategories(_state.value.selectedCategory!!.categoryId)
                }
            }
            is ReportsEvent.OnModalitySelected -> {
                _state.update { it.copy(selectedModality = event.modality) }
            }
            is ReportsEvent.OnDateSelected -> {
                _state.update { it.copy(selectedDate = event.date) }
            }
            is ReportsEvent.OnSaveReport -> {
                saveReport()
            }
        }
    }

    private fun loadCategories(type: Char) {
        if (isLoadingCategories) return

        isLoadingCategories = true
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCategories = true) }

            categoryRepository.getCategoriesByType(type).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {}
                    is NetworkResult.Success -> {
                        val categories = result.data ?: emptyList()
                        _state.update {
                            it.copy(
                                isLoadingCategories = false,
                                categories = categories,
                                error = null
                            )
                        }
                        isLoadingCategories = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoadingCategories = false,
                                error = result.message
                            )
                        }
                        isLoadingCategories = false
                    }
                }
            }
        }
    }

    private fun loadSubcategories(categoryId: String) {
        if (isLoadingSubcategories) return

        isLoadingSubcategories = true
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSubcategories = true) }

            subcategoryRepository.getSubcategoriesByCategory(categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {}
                    is NetworkResult.Success -> {
                        val subcategories = result.data ?: emptyList()
                        _state.update {
                            it.copy(
                                isLoadingSubcategories = false,
                                subcategories = subcategories,
                                error = null
                            )
                        }
                        isLoadingSubcategories = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoadingSubcategories = false,
                                error = result.message
                            )
                        }
                        isLoadingSubcategories = false
                    }
                }
            }
        }
    }

    fun createCategory(name: String, inputType: Char) {
        isCreatingCategory = true

        viewModelScope.launch {
            _state.update { it.copy(isCreatingCategory = true) }

            categoryRepository.createCategory(name, inputType).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isCreatingCategory = true) }
                    }
                    is NetworkResult.Success -> {
                        val newCategory = result.data
                        if (newCategory != null) {
                            val currentCategories = _state.value.categories.toMutableList()
                            currentCategories.add(newCategory)

                            _state.update {
                                it.copy(
                                    isCreatingCategory = false,
                                    categories = currentCategories,
                                    selectedCategory = newCategory,
                                    categoryCreated = newCategory,
                                    showDialog = false
                                )
                            }
                            // Cargar subcategorías para la nueva categoría
                            loadSubcategories(newCategory.categoryId)
                        } else {
                            _state.update {
                                it.copy(
                                    isCreatingCategory = false,
                                    error = "Error al crear categoría: datos nulos"
                                )
                            }
                        }
                        isCreatingCategory = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isCreatingCategory = false,
                                error = result.message
                            )
                        }
                        isCreatingCategory = false
                    }
                }
            }
        }
    }

    fun createSubcategory(name: String, categoryId: String) {
        isCreatingSubcategory = true

        viewModelScope.launch {
            _state.update { it.copy(isCreatingSubcategory = true) }

            subcategoryRepository.createSubcategory(name, categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isCreatingSubcategory = true) }
                    }
                    is NetworkResult.Success -> {
                        val newSubcategory = result.data
                        if (newSubcategory != null) {
                            val currentSubcategories = _state.value.subcategories.toMutableList()
                            currentSubcategories.add(newSubcategory)

                            _state.update {
                                it.copy(
                                    isCreatingSubcategory = false,
                                    subcategories = currentSubcategories,
                                    selectedSubcategory = newSubcategory,
                                    subcategoryCreated = newSubcategory,
                                    showSubcategoryDialog = false
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    isCreatingSubcategory = false,
                                    error = "Error al crear subcategoría: datos nulos"
                                )
                            }
                        }
                        isCreatingSubcategory = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isCreatingSubcategory = false,
                                error = result.message
                            )
                        }
                        isCreatingSubcategory = false
                    }
                }
            }
        }
    }

    private fun loadModalities() {
        if (isLoadingModalities) return

        isLoadingModalities = true
        viewModelScope.launch {
            _state.update { it.copy(isLoadingModalities = true) }

            modalityRepository.getModalities().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {}
                    is NetworkResult.Success -> {
                        val modalities = result.data ?: emptyList()
                        _state.update {
                            it.copy(
                                isLoadingModalities = false,
                                modalities = modalities,
                                error = null
                            )
                        }
                        if (modalities.isNotEmpty() && _state.value.selectedModality == null) {
                            _state.update { it.copy(selectedModality = modalities.first()) }
                        }
                        isLoadingModalities = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoadingModalities = false,
                                error = result.message
                            )
                        }
                        isLoadingModalities = false
                    }
                }
            }
        }
    }

    private fun saveReport() {

        val selectedCategory = _state.value.selectedCategory
        val selectedModality = _state.value.selectedModality
        val concept = _state.value.concept ?: ""
        val amount = _state.value.amount
        val selectedDate = _state.value.selectedDate
        val selectedType = _state.value.selectedType
        val selectedSubcategory = _state.value.selectedSubcategory

        // Validaciones
        if (selectedCategory == null) {
            _state.update { it.copy(error = "Selecciona una categoría") }
            return
        }

        if (selectedModality == null) {
            _state.update { it.copy(error = "Selecciona una modalidad") }
            return
        }

        if (amount <= 0.0) {
            _state.update { it.copy(error = "Ingresa un monto válido") }
            return
        }

        if (selectedDate.isEmpty()) {
            _state.update { it.copy(error = "Selecciona una fecha") }
            return
        }
        isSavingReport = true
        viewModelScope.launch {
            _state.update { it.copy(isSavingReport = true, error = null) }

            reportRepository.createReport(
                categoryId = selectedCategory.categoryId,
                subcategoryId = selectedSubcategory?.subCategoryId,
                modalityId = selectedModality.modalityId,
                concept = concept.takeIf { it.isNotEmpty() },
                date = selectedDate,
                amount = amount,
                type = selectedType
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _state.update { it.copy(isSavingReport = true) }
                    }
                    is NetworkResult.Success -> {
                        val report = result.data
                        _state.update {
                            it.copy(
                                isSavingReport = false,
                                reportSaved = report,
                                error = null
                            )
                        }
                        isSavingReport = false
                    }
                    is NetworkResult.Error -> {
                        _state.update {
                            it.copy(
                                isSavingReport = false,
                                error = result.message
                            )
                        }
                        isSavingReport = false
                    }
                }
            }
        }
    }

    fun clearReportSaved() {
        _state.update { it.copy(reportSaved = null) }
    }

    fun updateConcept(concept: String) {
        _state.update { it.copy(concept = concept) }
    }

    fun updateAmount(amount: String) {
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        _state.update { it.copy(amount = amountValue, amountText = amount) }
    }
    fun dismissDialog() {
        _state.update { it.copy(showDialog = false) }
    }

    fun dismissSubcategoryDialog() {
        _state.update { it.copy(showSubcategoryDialog = false) }
    }

    fun clearCategoryCreated() {
        _state.update { it.copy(categoryCreated = null) }
    }

    fun clearSubcategoryCreated() {
        _state.update { it.copy(subcategoryCreated = null) }
    }
}