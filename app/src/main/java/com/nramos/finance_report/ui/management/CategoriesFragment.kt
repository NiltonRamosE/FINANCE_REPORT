package com.nramos.finance_report.ui.management

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogCreateCategoryBinding
import com.nramos.finance_report.databinding.FragmentCategoriesManagementBinding
import com.nramos.finance_report.databinding.DialogEditCategoryBinding
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriesFragment : Fragment(R.layout.fragment_categories_management) {

    private var _binding: FragmentCategoriesManagementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManagementViewModel by viewModels()
    private lateinit var adapter: CategoriesManagementAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoriesManagementBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = CategoriesManagementAdapter(
            onEditClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            }
        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CategoriesFragment.adapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitList(state.categories)

                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.error?.let { error ->
                    showToast(error)
                }

                state.categoryCreated?.let {
                    showToast("Categoría creada exitosamente")
                    viewModel.clearSuccess()
                }

                state.categoryUpdated?.let {
                    showToast("Categoría actualizada exitosamente")
                    viewModel.clearSuccess()
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showCreateCategoryDialog()
        }
    }

    private fun showCreateCategoryDialog() {
        val dialogBinding = DialogCreateCategoryBinding.inflate(layoutInflater)
        var selectedType = 'I'

        // Función para actualizar estilos de los botones
        fun updateButtonStyles() {
            if (selectedType == 'I') {
                dialogBinding.btnIncomeType.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gold))
                dialogBinding.btnIncomeType.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                dialogBinding.btnExpenseType.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                dialogBinding.btnExpenseType.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light_secondary))
            } else {
                dialogBinding.btnIncomeType.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                dialogBinding.btnIncomeType.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light_secondary))
                dialogBinding.btnExpenseType.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                dialogBinding.btnExpenseType.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }

        // Configurar estado inicial
        selectedType = 'I'
        updateButtonStyles()

        // Listeners para los botones
        dialogBinding.btnIncomeType.setOnClickListener {
            selectedType = 'I'
            updateButtonStyles()
        }

        dialogBinding.btnExpenseType.setOnClickListener {
            selectedType = 'E'
            updateButtonStyles()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Categoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val name = dialogBinding.etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.onEvent(ManagementEvent.CreateCategory(name, selectedType))
                } else {
                    showToast("El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        if (!isAdded || isRemoving) return

        val dialogBinding = DialogEditCategoryBinding.inflate(layoutInflater)
        dialogBinding.etCategoryName.setText(category.name)

        // Configurar estado inicial según la categoría
        if (category.inputType == 'I') {
            dialogBinding.btnIncomeType.isChecked = true
            dialogBinding.btnIncomeType.isEnabled = true
            dialogBinding.btnExpenseType.isChecked = false
            dialogBinding.btnExpenseType.isEnabled = true
            updateTypeButtonStyleDialog(dialogBinding.btnIncomeType, true)
            updateTypeButtonStyleDialog(dialogBinding.btnExpenseType, false)
        } else {
            dialogBinding.btnIncomeType.isChecked = false
            dialogBinding.btnIncomeType.isEnabled = true
            dialogBinding.btnExpenseType.isChecked = true
            dialogBinding.btnExpenseType.isEnabled = true
            updateTypeButtonStyleDialog(dialogBinding.btnIncomeType, false)
            updateTypeButtonStyleDialog(dialogBinding.btnExpenseType, true)
        }

        var selectedType = category.inputType

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
            .setTitle("Editar Categoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val name = dialogBinding.etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.onEvent(ManagementEvent.UpdateCategory(category.categoryId, name, selectedType))
                } else {
                    showToast("El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateTypeButtonStyleDialog(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(resources.getColor(R.color.gold, null))
            button.setTextColor(resources.getColor(R.color.white, null))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.primary, null))
            button.setTextColor(resources.getColor(R.color.text_light_secondary, null))
        }
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de que deseas eliminar la categoría '${category.name}'? Se eliminarán también todas sus subcategorías.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.onEvent(ManagementEvent.DeleteCategory(category.categoryId))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}