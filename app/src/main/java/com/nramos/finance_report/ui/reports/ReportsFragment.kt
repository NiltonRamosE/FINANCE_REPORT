package com.nramos.finance_report.ui.reports

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogCreateCategoryBinding
import com.nramos.finance_report.databinding.FragmentReportsBinding
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    private var categoryAdapter: ArrayAdapter<Category>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportsBinding.bind(view)

        setupObservers()
        setupListeners()
        setupCategoryAutoComplete()
        setupTypeSelectionVisual()

        viewModel.onEvent(ReportsEvent.OnTypeSelected('I'))
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Actualizar categorías
                updateCategoryDropdown(state.categories)

                // Mostrar loading
                binding.progressBar.visibility = if (state.isLoadingCategories) View.VISIBLE else View.GONE

                // Mostrar error
                state.error?.let { error ->
                    showToast(error)
                }

                // Mostrar diálogo para crear categoría
                if (state.showDialog) {
                    showCreateCategoryDialog()
                }

                // Categoría creada exitosamente
                state.categoryCreated?.let { category ->
                    showToast("Categoría '${category.name}' creada exitosamente")
                    viewModel.clearCategoryCreated()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val type = when (checkedId) {
                        R.id.btnIncome -> 'I'
                        R.id.btnExpense -> 'E'
                        else -> 'I'
                    }
                    viewModel.onEvent(ReportsEvent.OnTypeSelected(type))
                }
            }
            btnAddCategory.setOnClickListener {
                viewModel.onEvent(ReportsEvent.OnCreateCategory)
            }
        }
    }

    private fun setupTypeSelectionVisual() {
        binding.apply {
            updateTypeButtonStyle(btnIncome, true)
            updateTypeButtonStyle(btnExpense, false)

            toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnIncome -> {
                            updateTypeButtonStyle(btnIncome, true)
                            updateTypeButtonStyle(btnExpense, false)
                        }
                        R.id.btnExpense -> {
                            updateTypeButtonStyle(btnIncome, false)
                            updateTypeButtonStyle(btnExpense, true)
                        }
                    }
                }
            }
        }
    }

    private fun updateTypeButtonStyle(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(resources.getColor(R.color.navy_500, null))
            button.setTextColor(resources.getColor(R.color.white, null))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.surface_light_200, null))
            button.setTextColor(resources.getColor(R.color.text_on_light_secondary, null))
        }
    }

    private fun setupCategoryAutoComplete() {
        categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.etCategory.setAdapter(categoryAdapter)

        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoryAdapter?.getItem(position)
            selectedCategory?.let {
                viewModel.onEvent(ReportsEvent.OnCategorySelected(it))
                binding.etCategory.setText(it.name, false)
            }
        }

        binding.etCategory.setOnClickListener {
            if (categoryAdapter?.count == 0) {
                viewModel.onEvent(ReportsEvent.OnCreateCategory)
            } else {
                binding.etCategory.showDropDown()
            }
        }
    }

    private fun updateCategoryDropdown(categories: List<Category>) {
        categoryAdapter?.clear()

        if (categories.isNotEmpty()) {
            categoryAdapter?.addAll(categories)
            categoryAdapter?.notifyDataSetChanged()

            val currentSelected = viewModel.state.value.selectedCategory
            if (currentSelected == null) {
                val firstCategory = categories.first()
                viewModel.onEvent(ReportsEvent.OnCategorySelected(firstCategory))
                binding.etCategory.setText(firstCategory.name, false)
            } else {
                val stillExists = categories.any { it.categoryId == currentSelected.categoryId }
                if (stillExists) {
                    binding.etCategory.setText(currentSelected.name, false)
                } else {
                    val firstCategory = categories.first()
                    viewModel.onEvent(ReportsEvent.OnCategorySelected(firstCategory))
                    binding.etCategory.setText(firstCategory.name, false)
                }
            }
        } else {
            categoryAdapter?.notifyDataSetChanged()
            binding.etCategory.setText("", false)
            binding.etCategory.hint = "No hay categorías - Haz clic para crear"
        }
    }

    private fun showCreateCategoryDialog() {
        if (!isAdded || isRemoving) return

        val dialogBinding = DialogCreateCategoryBinding.inflate(layoutInflater)

        dialogBinding.btnIncomeType.setBackgroundColor(resources.getColor(R.color.navy_500, null))
        dialogBinding.btnIncomeType.setTextColor(resources.getColor(R.color.white, null))
        dialogBinding.btnExpenseType.setBackgroundColor(resources.getColor(R.color.surface_light_200, null))
        dialogBinding.btnExpenseType.setTextColor(resources.getColor(R.color.text_on_light_secondary, null))

        var selectedType = 'I'

        dialogBinding.btnIncomeType.isChecked = true
        dialogBinding.btnExpenseType.isChecked = false
        updateTypeButtonStyleDialog(dialogBinding.btnIncomeType, true)
        updateTypeButtonStyleDialog(dialogBinding.btnExpenseType, false)

        // Listener para cambios en el toggle
        dialogBinding.toggleType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncomeType -> {
                        selectedType = 'I'
                        updateTypeButtonStyleDialog(dialogBinding.btnIncomeType, true)
                        updateTypeButtonStyleDialog(dialogBinding.btnExpenseType, false)
                    }
                    R.id.btnExpenseType -> {
                        selectedType = 'E'
                        updateTypeButtonStyleDialog(dialogBinding.btnIncomeType, false)
                        updateTypeButtonStyleDialog(dialogBinding.btnExpenseType, true)
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Categoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val name = dialogBinding.etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createCategory(name, selectedType)
                }
                viewModel.dismissDialog()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.dismissDialog()
            }
            .show()
    }

    private fun updateTypeButtonStyleDialog(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(resources.getColor(R.color.navy_500, null))
            button.setTextColor(resources.getColor(R.color.white, null))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.surface_light_200, null))
            button.setTextColor(resources.getColor(R.color.text_on_light_secondary, null))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}