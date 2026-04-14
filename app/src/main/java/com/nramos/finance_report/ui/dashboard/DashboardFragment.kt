// ui/dashboard/DashboardFragment.kt
package com.nramos.finance_report.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nramos.finance_report.R
import com.nramos.finance_report.databinding.FragmentDashboardBinding
import com.nramos.finance_report.utils.extensions.showToast
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var recentAdapter: RecentMovementsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        recentAdapter = RecentMovementsAdapter { report ->
            showToast("Movimiento: ${report.concept}")
        }

        binding.rvRecentMovements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Verificar que el binding no sea nulo
                if (_binding == null) return@collectLatest

                // Actualizar saldo total
                binding.tvTotalBalance.text = formatAmount(state.totalBalance)

                // Actualizar saldos por modalidad
                updateModalityBalances(state.modalityBalances)

                // Actualizar últimos movimientos
                recentAdapter.submitList(state.recentReports)

                // Mostrar error
                state.error?.let { error ->
                    showToast(error)
                }
            }
        }
    }

    private fun updateModalityBalances(balances: Map<String, Double>) {
        if (_binding == null) return

        // Actualizar Efectivo
        val cashBalance = balances["Efectivo"] ?: 0.0
        binding.tvCashBalance.text = formatAmount(cashBalance)

        // Actualizar Yape / Tarjeta BCP
        val yapeBalance = balances["YAPE / Tarjeta BCP"] ?: 0.0
        binding.tvYapeBalance.text = formatAmount(yapeBalance)
    }

    private fun formatAmount(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        return formatter.format(amount)
    }

    // Método público para refrescar desde fuera
    fun refreshData() {
        viewModel.onEvent(DashboardEvent.OnRefresh)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(DashboardEvent.OnRefresh)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}