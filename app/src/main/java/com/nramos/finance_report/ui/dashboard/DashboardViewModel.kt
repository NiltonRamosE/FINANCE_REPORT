package com.nramos.finance_report.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.repository.ModalityRepository
import com.nramos.finance_report.data.repository.ReportRepository
import com.nramos.finance_report.domain.model.Modality
import com.nramos.finance_report.domain.model.Report
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val modalityRepository: ModalityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var isLoading = false

    init {
        loadData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnRefresh -> {
                loadData()
            }
        }
    }

    private fun loadData() {
        if (isLoading) return

        isLoading = true
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Cargar modalidades y reportes en paralelo
            val modalitiesDeferred = async { loadModalities() }
            val reportsDeferred = async { loadReports() }

            val modalities = modalitiesDeferred.await()
            val reports = reportsDeferred.await()

            if (modalities != null && reports != null) {
                val balances = calculateBalances(reports, modalities)
                val recentReports = getRecentReports(reports)

                _state.update {
                    it.copy(
                        isLoading = false,
                        totalBalance = balances.totalBalance,
                        modalityBalances = balances.modalityBalances,
                        recentReports = recentReports,
                        error = null
                    )
                }
            }
            isLoading = false
        }
    }

    private suspend fun loadModalities(): List<Modality>? {
        var resultList: List<Modality>? = null
        modalityRepository.getModalities().collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    resultList = result.data ?: emptyList()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
        return resultList
    }

    private suspend fun loadReports(): List<Report>? {
        var resultList: List<Report>? = null
        reportRepository.getReports().collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    resultList = result.data ?: emptyList()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
        return resultList
    }

    private fun calculateBalances(
        reports: List<Report>,
        modalities: List<Modality>
    ): BalanceResult {
        val modalityNames = modalities.associate { it.modalityId to normalizeModalityName(it.name) }

        val modalityBalances = mutableMapOf<String, Double>()

        val yapeGroupIds = modalities
            .filter { isYapeOrBcp(it.name) }
            .map { it.modalityId }

        var totalBalance = 0.0

        for (report in reports) {
            val amount = if (report.type == 'I') report.amount else -report.amount
            totalBalance += amount

            val groupKey = when {
                yapeGroupIds.contains(report.modalityId) -> "YAPE / Tarjeta BCP"
                else -> modalityNames[report.modalityId] ?: "Otras"
            }

            modalityBalances[groupKey] = (modalityBalances[groupKey] ?: 0.0) + amount
        }

        return BalanceResult(
            totalBalance = totalBalance,
            modalityBalances = modalityBalances
        )
    }

    private fun normalizeModalityName(name: String): String {
        return when {
            name.equals("EFECTIVO", ignoreCase = true) -> "Efectivo"
            name.equals("YAPE", ignoreCase = true) -> "Yape"
            name.equals("TARJETA BCP", ignoreCase = true) -> "Tarjeta BCP"
            else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun isYapeOrBcp(name: String): Boolean {
        return name.contains("YAPE", ignoreCase = true) ||
                name.contains("BCP", ignoreCase = true)
    }

    private fun getRecentReports(reports: List<Report>, limit: Int = 5): List<Report> {
        return reports
            .sortedByDescending { it.date }
            .take(limit)
    }
}

data class DashboardState(
    val totalBalance: Double = 0.0,
    val modalityBalances: Map<String, Double> = emptyMap(),
    val recentReports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class DashboardEvent {
    object OnRefresh : DashboardEvent()
}

data class BalanceResult(
    val totalBalance: Double,
    val modalityBalances: Map<String, Double>
)