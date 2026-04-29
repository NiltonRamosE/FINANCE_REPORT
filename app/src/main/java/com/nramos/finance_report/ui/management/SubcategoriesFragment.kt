package com.nramos.finance_report.ui.management

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogCreateSubcategoryBinding
import com.nramos.finance_report.databinding.FragmentSubcategoriesManagementBinding
import com.nramos.finance_report.databinding.DialogEditSubcategoryBinding
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Subcategory
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubcategoriesFragment : Fragment(R.layout.fragment_subcategories_management) {

    private var _binding: FragmentSubcategoriesManagementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManagementViewModel by viewModels()
    private lateinit var adapter: SubcategoriesManagementAdapter
    private var categoryNames = mutableMapOf<String, String>()

    // Cache para el diálogo
    private var cachedCategories: List<Category> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSubcategoriesManagementBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = SubcategoriesManagementAdapter(
            onEditClick = { subcategory ->
                showEditSubcategoryDialog(subcategory)
            },
            onDeleteClick = { subcategory ->
                showDeleteConfirmationDialog(subcategory)
            }
        )
        binding.rvSubcategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubcategoriesFragment.adapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Construir mapa de nombres de categorías
                categoryNames.clear()
                state.categories.forEach { category ->
                    categoryNames[category.categoryId] = category.name
                }

                // Actualizar cache
                cachedCategories = state.categories

                adapter.submitList(state.subcategories, categoryNames)

                binding.progressBar.visibility = if (state.isLoadingSubcategories) View.VISIBLE else View.GONE

                state.error?.let { error ->
                    showToast(error)
                }

                state.subcategoryCreated?.let {
                    showToast("Subcategoría creada exitosamente")
                    viewModel.clearSuccess()
                }

                state.subcategoryUpdated?.let {
                    showToast("Subcategoría actualizada exitosamente")
                    viewModel.clearSuccess()
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showCreateSubcategoryDialog()
        }
    }

    private fun showCreateSubcategoryDialog() {
        val categories = viewModel.state.value.categories
        if (categories.isEmpty()) {
            showToast("Primero debes crear una categoría")
            return
        }

        val dialogBinding = DialogCreateSubcategoryBinding.inflate(layoutInflater)

        // Crear lista con nombres de categorías
        val categoryNamesList = categories.map { it.name }
        val categoryIdsList = categories.map { it.categoryId }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNamesList)
        dialogBinding.etParentCategory.setAdapter(adapter)

        // Seleccionar la primera por defecto
        var selectedCategoryId = categoryIdsList.first()
        dialogBinding.etParentCategory.setText(categoryNamesList.first(), false)

        dialogBinding.etParentCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categoryIdsList[position]
            dialogBinding.etParentCategory.setText(categoryNamesList[position], false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Subcategoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val name = dialogBinding.etSubcategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.onEvent(ManagementEvent.CreateSubcategory(name, selectedCategoryId))
                } else {
                    showToast("El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditSubcategoryDialog(subcategory: Subcategory) {
        val dialogBinding = DialogEditSubcategoryBinding.inflate(layoutInflater)
        dialogBinding.etSubcategoryName.setText(subcategory.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Subcategoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val name = dialogBinding.etSubcategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.onEvent(ManagementEvent.UpdateSubcategory(subcategory.subCategoryId, name))
                } else {
                    showToast("El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(subcategory: Subcategory) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Subcategoría")
            .setMessage("¿Estás seguro de que deseas eliminar la subcategoría '${subcategory.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.onEvent(ManagementEvent.DeleteSubcategory(subcategory.subCategoryId))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}