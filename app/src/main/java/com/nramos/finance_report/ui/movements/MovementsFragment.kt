package com.nramos.finance_report.ui.movements

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogEditMovementBinding
import com.nramos.finance_report.databinding.FragmentMovementsBinding
import com.nramos.finance_report.utils.extensions.formatToDisplayDate
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MovementsFragment : Fragment(R.layout.fragment_movements) {

    private var _binding: FragmentMovementsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MovementsViewModel by viewModels()

    private lateinit var adapter: MovementsAdapter
    private var isEditDialogShowing = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMovementsBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupSearchListener()
        setupEditDialogObserver()
    }

    private fun setupRecyclerView() {
        adapter = MovementsAdapter(
            onItemClick = { report ->
                // Ver detalle
            },
            onEditClick = { report ->
                viewModel.onEvent(MovementsEvent.OnEditReport(report))
            },
            onDeleteClick = { report ->
                showDeleteConfirmationDialog(report)
            }
        )

        binding.rvMovements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MovementsFragment.adapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Actualizar lista
                adapter.submitList(state.reports)

                // Mostrar loading
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                // Mostrar empty state
                val isEmpty = state.reports.isEmpty() && !state.isLoading
                binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvMovements.visibility = if (isEmpty) View.GONE else View.VISIBLE

                // Mostrar error
                state.error?.let { error ->
                    showToast(error)
                }
            }
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.apply {
            chipAll.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.onEvent(MovementsEvent.OnFilterChanged("all"))
                }
            }

            chipIncome.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.onEvent(MovementsEvent.OnFilterChanged("income"))
                }
            }

            chipExpense.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.onEvent(MovementsEvent.OnFilterChanged("expense"))
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(report: EnrichedReport) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Movimiento")
            .setMessage("¿Estás seguro de que deseas eliminar este movimiento?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.onEvent(MovementsEvent.OnDeleteReport(report.reportId))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupEditDialogObserver() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                if (state.showEditDialog && state.reportToEdit != null && !isEditDialogShowing) {
                    isEditDialogShowing = true
                    showEditMovementDialog(state.reportToEdit!!)
                }
            }
        }
    }

    private fun showEditMovementDialog(report: EnrichedReport) {
        val dialogBinding = DialogEditMovementBinding.inflate(layoutInflater)

        // Variables para almacenar selecciones
        var selectedType = report.type
        var selectedCategoryId = report.categoryId
        var selectedSubcategoryId = report.subcategoryId
        var selectedModalityId = report.modalityId

        // Configurar valores iniciales
        setupEditDialogInitialValues(dialogBinding, report)

        // Configurar listeners de UI
        setupEditDialogTypeToggle(dialogBinding) { type ->
            selectedType = type
            updateCategoriesForType(dialogBinding, type)
        }

        setupEditDialogCategoryAutoComplete(dialogBinding) { categoryId ->
            selectedCategoryId = categoryId
            updateSubcategoriesForCategory(dialogBinding, categoryId)
        }

        setupEditDialogSubcategoryAutoComplete(dialogBinding) { subcategoryId ->
            selectedSubcategoryId = subcategoryId
        }

        setupEditDialogModalityAutoComplete(dialogBinding) { modalityId ->
            selectedModalityId = modalityId
        }

        setupEditDialogDatePicker(dialogBinding)

        // Crear y mostrar el diálogo
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Movimiento")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val concept = dialogBinding.etConcept.text.toString().trim()
                val date = dialogBinding.etDate.text.toString().trim()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0

                if (date.isEmpty()) {
                    showToast("Selecciona una fecha")
                    return@setPositiveButton
                }
                if (amount <= 0.0) {
                    showToast("Ingresa un monto válido")
                    return@setPositiveButton
                }

                viewModel.onEvent(
                    MovementsEvent.OnUpdateReport(
                        reportId = report.reportId,
                        categoryId = selectedCategoryId,
                        subcategoryId = selectedSubcategoryId?.takeIf { it.isNotEmpty() },
                        modalityId = selectedModalityId,
                        concept = concept.takeIf { it.isNotEmpty() },
                        date = date,
                        amount = amount,
                        type = selectedType
                    )
                )
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.onEvent(MovementsEvent.OnCloseEditDialog)
                isEditDialogShowing = false
            }
            .show()
        dialog.setOnDismissListener {
            isEditDialogShowing = false
            viewModel.onEvent(MovementsEvent.OnCloseEditDialog)
        }
    }

    private fun setupEditDialogInitialValues(
        binding: DialogEditMovementBinding,
        report: EnrichedReport
    ) {
        // Configurar tipo
        if (report.type == 'I') {
            binding.btnIncome.isChecked = true
            updateTypeButtonStyle(binding.btnIncome, true)
            updateTypeButtonStyle(binding.btnExpense, false)
        } else {
            binding.btnExpense.isChecked = true
            updateTypeButtonStyle(binding.btnIncome, false)
            updateTypeButtonStyle(binding.btnExpense, true)
        }

        // Configurar concepto, fecha y monto
        binding.etConcept.setText(report.concept ?: "")
        binding.etDate.setText(report.date.formatToDisplayDate())
        binding.etAmount.setText(String.format("%.2f", report.amount))

        // Cargar categorías según el tipo
        loadCategoriesForEditDialog(binding, report.type, report.categoryId)

        // Cargar modalidades
        loadModalitiesForEditDialog(binding, report.modalityId)
    }

    private fun loadCategoriesForEditDialog(
        binding: DialogEditMovementBinding,
        type: Char,
        selectedCategoryId: String
    ) {
        lifecycleScope.launch {
            val allCategories = viewModel.state.value.allCategories
            val filteredCategories = allCategories.filter { it.inputType == type }

            val categoryNames = filteredCategories.map { it.name }
            val categoryIds = filteredCategories.map { it.categoryId }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )
            binding.etCategory.setAdapter(adapter)

            // Seleccionar la categoría actual
            val position = categoryIds.indexOf(selectedCategoryId)
            if (position >= 0) {
                binding.etCategory.setText(categoryNames[position], false)
            } else if (categoryNames.isNotEmpty()) {
                binding.etCategory.setText(categoryNames[0], false)
            }

            // Cargar subcategorías de la categoría seleccionada
            loadSubcategoriesForEditDialog(binding, selectedCategoryId)
        }
    }

    private fun loadSubcategoriesForEditDialog(
        binding: DialogEditMovementBinding,
        categoryId: String
    ) {
        lifecycleScope.launch {
            viewModel.loadSubcategories(categoryId)

            // Esperar a que se carguen las subcategorías
            var retries = 0
            while (viewModel.state.value.availableSubcategories.isEmpty() && retries < 10) {
                kotlinx.coroutines.delay(100)
                retries++
            }

            val subcategories = viewModel.state.value.availableSubcategories
            val subcategoryNames = listOf("Sin subcategoría") + subcategories.map { it.name }
            val subcategoryIds = listOf("") + subcategories.map { it.subCategoryId }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subcategoryNames)
            binding.etSubcategory.setAdapter(adapter)

            // Seleccionar la subcategoría actual si existe
            val currentSubcategory = viewModel.state.value.reportToEdit?.subcategoryId
            val position = subcategoryIds.indexOf(currentSubcategory)
            if (position >= 0) {
                binding.etSubcategory.setText(subcategoryNames[position], false)
            } else {
                binding.etSubcategory.setText("", false)
            }
        }
    }

    private fun loadModalitiesForEditDialog(
        binding: DialogEditMovementBinding,
        selectedModalityId: String
    ) {
        val modalities = viewModel.state.value.modalities
        val modalityNames = modalities.map { it.name }
        val modalityIds = modalities.map { it.modalityId }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modalityNames)
        binding.etModality.setAdapter(adapter)

        val position = modalityIds.indexOf(selectedModalityId)
        if (position >= 0) {
            binding.etModality.setText(modalityNames[position], false)
        } else if (modalityNames.isNotEmpty()) {
            binding.etModality.setText(modalityNames[0], false)
        }
    }

    private fun setupEditDialogTypeToggle(
        binding: DialogEditMovementBinding,
        onTypeChanged: (Char) -> Unit
    ) {
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val type = when (checkedId) {
                    R.id.btnIncome -> 'I'
                    else -> 'E'
                }
                updateTypeButtonStyle(binding.btnIncome, type == 'I')
                updateTypeButtonStyle(binding.btnExpense, type == 'E')
                onTypeChanged(type)
            }
        }
    }

    private fun updateTypeButtonStyle(button: com.google.android.material.button.MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gold))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light_secondary))
        }
    }

    private fun setupEditDialogCategoryAutoComplete(
        binding: DialogEditMovementBinding,
        onCategorySelected: (String) -> Unit
    ) {
        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            val categories = viewModel.state.value.allCategories
            val filteredCategories = categories.filter { it.inputType == getCurrentEditDialogType(binding) }
            if (position < filteredCategories.size) {
                val selectedCategory = filteredCategories[position]
                onCategorySelected(selectedCategory.categoryId)
                binding.etCategory.setText(selectedCategory.name, false)
                // Limpiar subcategoría al cambiar de categoría
                binding.etSubcategory.setText("", false)
            }
        }
    }

    private fun setupEditDialogSubcategoryAutoComplete(
        binding: DialogEditMovementBinding,
        onSubcategorySelected: (String) -> Unit
    ) {
        binding.etSubcategory.setOnItemClickListener { _, _, position, _ ->
            val subcategories = viewModel.state.value.availableSubcategories
            if (position == 0) {
                onSubcategorySelected("")
            } else if (position - 1 < subcategories.size) {
                onSubcategorySelected(subcategories[position - 1].subCategoryId)
            }
        }
    }

    private fun setupEditDialogModalityAutoComplete(
        binding: DialogEditMovementBinding,
        onModalitySelected: (String) -> Unit
    ) {
        binding.etModality.setOnItemClickListener { _, _, position, _ ->
            val modalities = viewModel.state.value.modalities
            if (position < modalities.size) {
                onModalitySelected(modalities[position].modalityId)
            }
        }
    }

    private fun setupEditDialogDatePicker(binding: DialogEditMovementBinding) {
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona la fecha")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = Date(selection)
                val formattedDate = sdf.format(date)
                binding.etDate.setText(formattedDate)
            }

            datePicker.show(parentFragmentManager, "DatePicker")
        }
    }

    private fun getCurrentEditDialogType(binding: DialogEditMovementBinding): Char {
        return if (binding.btnIncome.isChecked) 'I' else 'E'
    }

    private fun updateCategoriesForType(
        binding: DialogEditMovementBinding,
        type: Char
    ) {
        lifecycleScope.launch {
            val allCategories = viewModel.state.value.allCategories
            val filteredCategories = allCategories.filter { it.inputType == type }

            val categoryNames = filteredCategories.map { it.name }
            val categoryIds = filteredCategories.map { it.categoryId }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.etCategory.setAdapter(adapter)

            if (categoryNames.isNotEmpty()) {
                binding.etCategory.setText(categoryNames[0], false)
                // Cargar subcategorías de la primera categoría
                updateSubcategoriesForCategory(binding, categoryIds[0])
            } else {
                binding.etCategory.setText("", false)
                binding.etSubcategory.setText("", false)
                val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, emptyList<String>())
                binding.etSubcategory.setAdapter(emptyAdapter)
            }
        }
    }

    private fun updateSubcategoriesForCategory(
        binding: DialogEditMovementBinding,
        categoryId: String
    ) {
        lifecycleScope.launch {
            viewModel.loadSubcategories(categoryId)

            var retries = 0
            while (viewModel.state.value.availableSubcategories.isEmpty() && retries < 10) {
                kotlinx.coroutines.delay(100)
                retries++
            }

            val subcategories = viewModel.state.value.availableSubcategories
            val subcategoryNames = listOf("Sin subcategoría") + subcategories.map { it.name }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subcategoryNames)
            binding.etSubcategory.setAdapter(adapter)
            binding.etSubcategory.setText(subcategoryNames[0], false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}