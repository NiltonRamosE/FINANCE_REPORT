package com.nramos.finance_report.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.CategoryRepository
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
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private var isLoadingCategories = false
    private var isCreatingCategory = false

    fun onEvent(event: ReportsEvent) {
        when (event) {
            is ReportsEvent.OnTypeSelected -> {
                if (_state.value.selectedType != event.type) {
                    _state.update {
                        it.copy(
                            selectedType = event.type,
                            selectedCategory = null,
                            categories = emptyList()
                        )
                    }
                    loadCategories(event.type)
                }
            }
            is ReportsEvent.OnCategorySelected -> {
                _state.update { it.copy(selectedCategory = event.category) }
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
        }
    }

    private fun loadCategories(type: Char) {
        if (isLoadingCategories) return

        isLoadingCategories = true
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCategories = true) }


            categoryRepository.getCategoriesByType(type).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                    }
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

    fun createCategory(name: String, inputType: Char) {

        isCreatingCategory = true

        viewModelScope.launch {
            _state.update {
                it.copy(isCreatingCategory = true)
            }

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

    fun dismissDialog() {
        _state.update { it.copy(showDialog = false) }
    }

    fun clearCategoryCreated() {
        _state.update { it.copy(categoryCreated = null) }
    }
}

data class ReportsState(
    val selectedType: Char = 'I',
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isLoadingCategories: Boolean = false,
    val isCreatingCategory: Boolean = false,
    val showDialog: Boolean = false,
    val categoryCreated: Category? = null,
    val error: String? = null
)

sealed class ReportsEvent {
    data class OnTypeSelected(val type: Char) : ReportsEvent()
    data class OnCategorySelected(val category: Category) : ReportsEvent()
    object OnCreateCategory : ReportsEvent()
    data class OnCategoryCreated(val category: Category) : ReportsEvent()
}