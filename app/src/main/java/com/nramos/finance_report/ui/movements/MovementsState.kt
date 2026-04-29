package com.nramos.finance_report.ui.movements

import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Modality
import com.nramos.finance_report.domain.model.Subcategory

data class MovementsState(
    val reports: List<EnrichedReport> = emptyList(),
    val selectedFilter: String = "all",
    val isLoading: Boolean = false,
    val error: String? = null,
    val modalities: List<Modality> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val availableSubcategories: List<Subcategory> = emptyList(),
    val showEditDialog: Boolean = false,
    val reportToEdit: EnrichedReport? = null
)