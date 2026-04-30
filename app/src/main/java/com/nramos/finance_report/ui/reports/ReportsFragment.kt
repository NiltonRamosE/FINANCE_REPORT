package com.nramos.finance_report.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogCreateCategoryBinding
import com.nramos.finance_report.databinding.DialogCreateSubcategoryBinding
import com.nramos.finance_report.databinding.FragmentReportsBinding
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.domain.model.Modality
import com.nramos.finance_report.domain.model.Subcategory
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()
    private var categoryAdapter: ArrayAdapter<Category>? = null
    private var subcategoryAdapter: ArrayAdapter<Subcategory>? = null
    private var modalityAdapter: ArrayAdapter<Modality>? = null
    private lateinit var conceptAdapter: ArrayAdapter<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportsBinding.bind(view)

        setupObservers()
        setupListeners()
        setupCategoryAutoComplete()
        setupSubcategoryAutoComplete()
        setupModalityAutoComplete()
        setupTypeSelectionVisual()

        val currentDate = formatDate(
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.YEAR)
        )
        viewModel.onEvent(ReportsEvent.OnDateSelected(currentDate))
        binding.etDate.setText(currentDate)

        viewModel.onEvent(ReportsEvent.OnTypeSelected('I'))
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Actualizar categorías
                updateCategoryDropdown(state.categories)
                updateModalityDropdown(state.modalities)
                updateSubcategoryDropdown(state.subcategories)

                // Mostrar loading
                binding.progressBar.visibility = if (state.isLoadingCategories) View.VISIBLE else View.GONE

                // Mostrar diálogo para crear categoría
                if (state.showDialog) {
                    showCreateCategoryDialog()
                }

                // Mostrar diálogo para crear subcategoría
                if (state.showSubcategoryDialog) {
                    showCreateSubcategoryDialog()
                }

                // Categoría creada exitosamente
                state.categoryCreated?.let { category ->
                    showToast("Categoría '${category.name}' creada exitosamente")
                    viewModel.clearCategoryCreated()
                }

                // Subcategoría creada exitosamente
                state.subcategoryCreated?.let { subcategory ->
                    showToast("Subcategoría '${subcategory.name}' creada exitosamente")
                    viewModel.clearSubcategoryCreated()
                }

                if (state.isSavingReport) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }

                state.reportSaved?.let { report ->
                    showToast("Movimiento guardado exitosamente")
                    viewModel.clearReportSaved()
                    clearForm()
                }

                state.reportSaved?.let { report ->
                    val message = when (report.type) {
                        'I' -> "✓ Ingreso guardado: +${formatAmount(report.amount)}"
                        'E' -> "✓ Egreso guardado: -${formatAmount(report.amount)}"
                        else -> "✓ Movimiento guardado exitosamente"
                    }
                    when (report.type) {
                        'I' -> showSuccessNotification(message, isIncome = true)
                        'E' -> showSuccessNotification(message, isIncome = false)
                        else -> showSuccessNotification(message, isIncome = true)
                    }
                    viewModel.clearReportSaved()
                    clearForm()
                }

                state.error?.let { error ->
                    showErrorNotification("✗ Error: $error")
                }
            }
        }
    }
    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "S/ %.2f", amount)
    }

    private fun showSuccessNotification(message: String, isIncome: Boolean = true) {
        val backgroundColor = if (isIncome) {
            resources.getColor(R.color.green, null)
        } else {
            resources.getColor(R.color.red, null)
        }

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(backgroundColor)
            .setTextColor(resources.getColor(R.color.white, null))
            .setAction("OK") { }
            .show()
    }

    private fun showErrorNotification(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.red, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .setAction("OK") { }
            .show()
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
            btnAddSubcategory.setOnClickListener {
                val selectedCategory = viewModel.state.value.selectedCategory
                if (selectedCategory == null) {
                    showToast("Primero selecciona una categoría")
                } else {
                    viewModel.onEvent(ReportsEvent.OnCreateSubcategory)
                }
            }
            etDate.setOnClickListener {
                showDatePicker()
            }
            btnSave.setOnClickListener {
                viewModel.updateConcept(binding.etConcept.text.toString())
                viewModel.updateAmount(binding.etAmount.text.toString())
                viewModel.onEvent(ReportsEvent.OnSaveReport)
            }

            setupConceptAutoComplete()
        }
    }

    private fun setupConceptAutoComplete() {
        conceptAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )

        binding.etConcept.apply {
            setAdapter(conceptAdapter)
            threshold = 1
        }

        lifecycleScope.launch {
            viewModel.concepts.collect { concepts ->
                conceptAdapter.clear()
                conceptAdapter.addAll(concepts)
                conceptAdapter.notifyDataSetChanged()
            }
        }
    }
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = Date(selection)
            val formattedDate = sdf.format(date)
            viewModel.onEvent(ReportsEvent.OnDateSelected(formattedDate))
            binding.etDate.setText(formattedDate)
        }

        datePicker.show(parentFragmentManager, "DatePicker")
    }

    private fun formatDate(day: Int, month: Int, year: Int): String {
        return String.format("%02d/%02d/%04d", day, month, year)
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
            button.setBackgroundColor(resources.getColor(R.color.gold, null))
            button.setTextColor(resources.getColor(R.color.white, null))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.primary, null))
            button.setTextColor(resources.getColor(R.color.text_light_secondary, null))
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

                binding.etSubcategory.setText("", false)
            }
        }

        binding.etCategory.setOnClickListener {
            if (categoryAdapter?.count != 0) {
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

        dialogBinding.btnIncomeType.setBackgroundColor(resources.getColor(R.color.gold, null))
        dialogBinding.btnIncomeType.setTextColor(resources.getColor(R.color.white, null))
        dialogBinding.btnExpenseType.setBackgroundColor(resources.getColor(R.color.primary, null))
        dialogBinding.btnExpenseType.setTextColor(resources.getColor(R.color.text_light_secondary, null))

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
            button.setBackgroundColor(resources.getColor(R.color.gold, null))
            button.setTextColor(resources.getColor(R.color.white, null))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.primary, null))
            button.setTextColor(resources.getColor(R.color.text_light_secondary, null))
        }
    }

    private fun setupSubcategoryAutoComplete() {
        subcategoryAdapter = object : ArrayAdapter<Subcategory>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        ) {
            fun toString(obj: Subcategory?): String {
                return obj?.name ?: ""
            }

            override fun getItem(position: Int): Subcategory? {
                return super.getItem(position)
            }
        }
        binding.etSubcategory.setAdapter(subcategoryAdapter)

        binding.etSubcategory.setOnItemClickListener { _, _, position, _ ->
            val selectedSubcategory = subcategoryAdapter?.getItem(position)
            selectedSubcategory?.let {
                viewModel.onEvent(ReportsEvent.OnSubcategorySelected(it))
                binding.etSubcategory.setText(it.name, false)
            }
        }
    }

    private fun updateSubcategoryDropdown(subcategories: List<Subcategory>) {
        subcategoryAdapter?.clear()

        if (subcategories.isNotEmpty()) {
            subcategoryAdapter?.addAll(subcategories)
            subcategoryAdapter?.notifyDataSetChanged()

            binding.etSubcategory.hint = "Selecciona una subcategoría (opcional)"

            val currentSelected = viewModel.state.value.selectedSubcategory
            if (currentSelected != null && subcategories.any { it.subCategoryId == currentSelected.subCategoryId }) {
                binding.etSubcategory.setText(currentSelected.name, false)
            } else {
                binding.etSubcategory.setText("", false)
            }
        } else {
            subcategoryAdapter?.notifyDataSetChanged()
            binding.etSubcategory.setText("", false)
            binding.etSubcategory.hint = "Subcategoría (opcional)"
        }
    }

    private fun showCreateSubcategoryDialog() {

        val selectedCategory = viewModel.state.value.selectedCategory

        if (selectedCategory == null) {
            showToast("Primero debes seleccionar una categoría")
            viewModel.dismissSubcategoryDialog()
            return
        }

        val dialogBinding = DialogCreateSubcategoryBinding.inflate(layoutInflater)

        dialogBinding.etParentCategory.setText(selectedCategory.name, false)
        dialogBinding.etParentCategory.isEnabled = false

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Subcategoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val name = dialogBinding.etSubcategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createSubcategory(name, selectedCategory.categoryId)
                } else {
                    showToast("El nombre no puede estar vacío")
                }
                viewModel.dismissSubcategoryDialog()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.dismissSubcategoryDialog()
            }
            .show()
    }

    private fun setupModalityAutoComplete() {
        modalityAdapter = object : ArrayAdapter<Modality>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        ) {
            fun toString(obj: Modality?): String {
                return obj?.name ?: ""
            }
        }
        binding.etModality.setAdapter(modalityAdapter)

        binding.etModality.setOnItemClickListener { _, _, position, _ ->
            val selectedModality = modalityAdapter?.getItem(position)
            selectedModality?.let {
                viewModel.onEvent(ReportsEvent.OnModalitySelected(it))
                binding.etModality.setText(it.name, false)
            }
        }
    }

    private fun updateModalityDropdown(modalities: List<Modality>) {
        modalityAdapter?.clear()

        if (modalities.isNotEmpty()) {
            modalityAdapter?.addAll(modalities)
            modalityAdapter?.notifyDataSetChanged()

            val currentSelected = viewModel.state.value.selectedModality
            if (currentSelected != null && modalities.any { it.modalityId == currentSelected.modalityId }) {
                binding.etModality.setText(currentSelected.name, false)
            } else {
                binding.etModality.setText("", false)
            }
        } else {
            modalityAdapter?.notifyDataSetChanged()
            binding.etModality.setText("", false)
            binding.etModality.hint = "No hay modalidades disponibles"
        }
    }

    private fun clearForm() {
        binding.etCategory.setText("", false)
        binding.etSubcategory.setText("", false)
        binding.etConcept.setText("")
        binding.etAmount.setText("")
        val currentDate = formatDate(
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.YEAR)
        )
        viewModel.onEvent(ReportsEvent.OnDateSelected(currentDate))
        binding.etDate.setText(currentDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}