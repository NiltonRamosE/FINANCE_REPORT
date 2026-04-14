package com.nramos.finance_report.ui.movements

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentMovementsBinding
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovementsFragment : Fragment(R.layout.fragment_movements) {

    private var _binding: FragmentMovementsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MovementsViewModel by viewModels()

    private lateinit var adapter: MovementsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMovementsBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = MovementsAdapter { report ->
            // TODO: Implementar click para ver detalle o editar
            showToast("Click en movimiento: ${report.amount}")
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}