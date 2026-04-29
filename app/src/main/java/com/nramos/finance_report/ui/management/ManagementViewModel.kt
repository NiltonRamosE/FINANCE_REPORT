package com.nramos.finance_report.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.CategoryRepository
import com.nramos.finance_report.data.repository.SubcategoryRepository
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Subcategory
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManagementState())
    val state: StateFlow<ManagementState> = _state.asStateFlow()

    init {
        loadAllData()
    }

    fun onEvent(event: ManagementEvent) {
        when (event) {
            is ManagementEvent.LoadAllData -> loadAllData()
            is ManagementEvent.CreateCategory -> createCategory(event.name, event.inputType)
            is ManagementEvent.UpdateCategory -> updateCategory(event.categoryId, event.name, event.inputType)
            is ManagementEvent.DeleteCategory -> deleteCategory(event.categoryId)
            is ManagementEvent.CreateSubcategory -> createSubcategory(event.name, event.categoryId)
            is ManagementEvent.UpdateSubcategory -> updateSubcategory(event.subcategoryId, event.name)
            is ManagementEvent.DeleteSubcategory -> deleteSubcategory(event.subcategoryId)
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isLoadingSubcategories = true) }

            // Cargar categorías de ambos tipos
            val incomeResult = categoryRepository.getCategoriesByType('I')
            val expenseResult = categoryRepository.getCategoriesByType('E')

            val incomeCategories = mutableListOf<Category>()
            val expenseCategories = mutableListOf<Category>()

            incomeResult.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        incomeCategories.addAll(result.data ?: emptyList())
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }

            expenseResult.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        expenseCategories.addAll(result.data ?: emptyList())
                        val allCategories = (incomeCategories + expenseCategories)
                            .sortedBy { it.name }

                        _state.update {
                            it.copy(
                                isLoading = false,
                                categories = allCategories,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }

            // Después de tener categorías, cargar subcategorías
            loadAllSubcategories()
        }
    }

    private fun loadAllSubcategories() {
        viewModelScope.launch {
            val currentCategories = _state.value.categories
            if (currentCategories.isEmpty()) {
                _state.update { it.copy(isLoadingSubcategories = false, subcategories = emptyList()) }
                return@launch
            }

            val allSubcategories = mutableListOf<Subcategory>()

            for (category in currentCategories) {
                subcategoryRepository.getSubcategoriesByCategory(category.categoryId).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            allSubcategories.addAll(result.data ?: emptyList())
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(error = result.message) }
                        }
                        else -> {}
                    }
                }
            }

            _state.update {
                it.copy(
                    isLoadingSubcategories = false,
                    subcategories = allSubcategories,
                    error = null
                )
            }
        }
    }

    private fun createCategory(name: String, inputType: Char) {
        viewModelScope.launch {
            categoryRepository.createCategory(name, inputType).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                categoryCreated = result.data,
                                showDialog = false
                            )
                        }
                        loadAllData()
                        // Forzar actualización en SubcategoriesFragment
                        _state.update { state ->
                            state.copy(categories = _state.value.categories)
                        }
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateCategory(categoryId: String, name: String, inputType: Char) {
        viewModelScope.launch {
            categoryRepository.updateCategory(categoryId, name, inputType).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                categoryUpdated = result.data,
                                showEditDialog = false
                            )
                        }
                        loadAllData() // Recargar todo después de actualizar
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadAllData() // Recargar todo después de eliminar
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun createSubcategory(name: String, categoryId: String) {
        viewModelScope.launch {
            subcategoryRepository.createSubcategory(name, categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                subcategoryCreated = result.data,
                                showDialog = false
                            )
                        }
                        loadAllData() // Recargar todo después de crear
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateSubcategory(subcategoryId: String, name: String) {
        viewModelScope.launch {
            subcategoryRepository.updateSubcategory(subcategoryId, name).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _state.update {
                            it.copy(
                                subcategoryUpdated = result.data,
                                showEditDialog = false
                            )
                        }
                        loadAllData() // Recargar todo después de actualizar
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun deleteSubcategory(subcategoryId: String) {
        viewModelScope.launch {
            subcategoryRepository.deleteSubcategory(subcategoryId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadAllData() // Recargar todo después de eliminar
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearDialogs() {
        _state.update {
            it.copy(
                showDialog = false,
                showEditDialog = false,
                categoryToEdit = null,
                subcategoryToEdit = null
            )
        }
    }

    fun setCategoryToEdit(category: Category) {
        _state.update {
            it.copy(
                showEditDialog = true,
                categoryToEdit = category
            )
        }
    }

    fun setSubcategoryToEdit(subcategory: Subcategory) {
        _state.update {
            it.copy(
                showEditDialog = true,
                subcategoryToEdit = subcategory
            )
        }
    }

    fun clearSuccess() {
        _state.update {
            it.copy(
                categoryCreated = null,
                categoryUpdated = null,
                subcategoryCreated = null,
                subcategoryUpdated = null
            )
        }
    }
}

data class ManagementState(
    val categories: List<Category> = emptyList(),
    val subcategories: List<Subcategory> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSubcategories: Boolean = false,
    val showDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val categoryToEdit: Category? = null,
    val subcategoryToEdit: Subcategory? = null,
    val categoryCreated: Category? = null,
    val categoryUpdated: Category? = null,
    val subcategoryCreated: Subcategory? = null,
    val subcategoryUpdated: Subcategory? = null,
    val error: String? = null
)

sealed class ManagementEvent {
    object LoadAllData : ManagementEvent()
    data class CreateCategory(val name: String, val inputType: Char) : ManagementEvent()
    data class UpdateCategory(val categoryId: String, val name: String, val inputType: Char) : ManagementEvent()
    data class DeleteCategory(val categoryId: String) : ManagementEvent()
    data class CreateSubcategory(val name: String, val categoryId: String) : ManagementEvent()
    data class UpdateSubcategory(val subcategoryId: String, val name: String) : ManagementEvent()
    data class DeleteSubcategory(val subcategoryId: String) : ManagementEvent()
}