package com.nramos.finance_report.ui.reminders

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.DialogReminderBinding
import com.nramos.finance_report.databinding.FragmentRemindersBinding
import com.nramos.finance_report.domain.model.Frequency
import com.nramos.finance_report.domain.model.Reminder
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RemindersFragment : Fragment(R.layout.fragment_reminders) {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RemindersViewModel by viewModels()
    private lateinit var adapter: RemindersAdapter
    private var isDialogShowing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRemindersBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = RemindersAdapter(
            onToggleClick = { id, isActive ->
                viewModel.onEvent(RemindersEvent.ToggleReminderStatus(id, isActive))
            },
            onEditClick = { reminder ->
                viewModel.onEvent(RemindersEvent.ShowEditDialog(reminder))
            },
            onDeleteClick = { reminder ->
                showDeleteConfirmationDialog(reminder)
            }
        )
        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RemindersFragment.adapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->

                adapter.submitList(state.reminders)

                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                val isEmpty = state.reminders.isEmpty() && !state.isLoading
                binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvReminders.visibility = if (isEmpty) View.GONE else View.VISIBLE

                state.error?.let { error ->
                    showToast(error)
                }

                if (state.showCreateDialog && !isDialogShowing) {
                    isDialogShowing = true
                    showCreateReminderDialog()
                }

                if (state.showEditDialog && state.reminderToEdit != null && !isDialogShowing) {
                    isDialogShowing = true
                    showEditReminderDialog(state.reminderToEdit!!)
                }

                state.reminderCreated?.let {
                    showToast("Recordatorio creado exitosamente")
                    viewModel.clearSuccess()
                }

                state.reminderUpdated?.let {
                    showToast("Recordatorio actualizado exitosamente")
                    viewModel.clearSuccess()
                    // Forzar cierre del flag después de actualizar
                    isDialogShowing = false
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            viewModel.onEvent(RemindersEvent.ShowCreateDialog)
        }
    }

    private fun showCreateReminderDialog() {
        try {
            val dialogBinding = DialogReminderBinding.inflate(layoutInflater)
            var selectedDate = Calendar.getInstance()

            setupFrequencySpinner(dialogBinding)

            dialogBinding.etDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        val selected = Calendar.getInstance().apply {
                            set(year, month, day)
                        }
                        selectedDate = selected
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dialogBinding.etDate.setText(dateFormat.format(selected.time))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dialogBinding.etDate.setText(dateFormat.format(today.time))

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton("Crear") { _, _ ->
                    val title = dialogBinding.etTitle.text.toString().trim()
                    val description = dialogBinding.etDescription.text.toString().trim()
                    val frequency = getSelectedFrequency(dialogBinding)

                    if (title.isEmpty()) {
                        showToast("El título es requerido")
                        return@setPositiveButton
                    }

                    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateTimeString = apiFormat.format(selectedDate.time)

                    viewModel.onEvent(
                        RemindersEvent.CreateReminder(
                            title = title,
                            description = description.takeIf { it.isNotEmpty() },
                            dateTime = dateTimeString,
                            frequency = frequency
                        )
                    )
                    isDialogShowing = false
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    viewModel.onEvent(RemindersEvent.HideCreateDialog)
                    isDialogShowing = false
                }
                .show()

            dialog.setOnDismissListener {
                isDialogShowing = false
                viewModel.onEvent(RemindersEvent.HideCreateDialog)
            }
        } catch (e: Exception) {
            isDialogShowing = false
        }
    }

    private fun showEditReminderDialog(reminder: Reminder) {
        try {
            val dialogBinding = DialogReminderBinding.inflate(layoutInflater)

            dialogBinding.etTitle.setText(reminder.title)
            dialogBinding.etDescription.setText(reminder.description ?: "")

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            try {
                calendar.time = sdf.parse(reminder.dateTime) ?: Date()
            } catch (e: Exception) {
                calendar.time = Date()
            }

            var selectedDate = calendar

            setupFrequencySpinner(dialogBinding, reminder.frequency)

            dialogBinding.etDate.setOnClickListener {
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        val selected = Calendar.getInstance().apply {
                            set(year, month, day)
                        }
                        selectedDate = selected
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dialogBinding.etDate.setText(dateFormat.format(selected.time))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dialogBinding.etDate.setText(displayFormat.format(calendar.time))

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton("Guardar") { _, _ ->
                    val title = dialogBinding.etTitle.text.toString().trim()
                    val description = dialogBinding.etDescription.text.toString().trim()
                    val frequency = getSelectedFrequency(dialogBinding)

                    if (title.isEmpty()) {
                        showToast("El título es requerido")
                        return@setPositiveButton
                    }

                    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateTimeString = apiFormat.format(selectedDate.time)

                    viewModel.onEvent(
                        RemindersEvent.UpdateReminder(
                            id = reminder.id,
                            title = title,
                            description = description.takeIf { it.isNotEmpty() },
                            dateTime = dateTimeString,
                            frequency = frequency,
                            isActive = reminder.isActive
                        )
                    )
                    isDialogShowing = false
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    viewModel.onEvent(RemindersEvent.HideEditDialog)
                    isDialogShowing = false
                }
                .show()

            dialog.setOnDismissListener {
                isDialogShowing = false
                viewModel.onEvent(RemindersEvent.HideEditDialog)
            }
        } catch (e: Exception) {
            showToast("Error al abrir el recordatorio")
            viewModel.onEvent(RemindersEvent.HideEditDialog)
            isDialogShowing = false
        }
    }

    private fun setupFrequencySpinner(binding: DialogReminderBinding, selectedFrequency: String = "once") {
        val frequencies = Frequency.getDisplayNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
        binding.etFrequency.setAdapter(adapter)

        val frequencyValues = Frequency.getValues()
        val index = frequencyValues.indexOf(selectedFrequency)
        if (index >= 0) {
            binding.etFrequency.setText(frequencies[index], false)
        }
    }

    private fun getSelectedFrequency(binding: DialogReminderBinding): String {
        val selectedDisplay = binding.etFrequency.text.toString()
        val frequencies = Frequency.getDisplayNames()
        val frequencyValues = Frequency.getValues()
        val index = frequencies.indexOf(selectedDisplay)
        return if (index >= 0) frequencyValues[index] else "once"
    }

    private fun showDeleteConfirmationDialog(reminder: Reminder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Recordatorio")
            .setMessage("¿Estás seguro de que deseas eliminar este recordatorio?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.onEvent(RemindersEvent.DeleteReminder(reminder.id))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}