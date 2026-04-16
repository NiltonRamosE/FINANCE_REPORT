package com.nramos.finance_report.ui.statistics

sealed class StatisticsEvent {
    data class OnFilterTypeChanged(val filterType: FilterType) : StatisticsEvent()
    data class OnCustomDateRange(val startDate: String, val endDate: String) : StatisticsEvent()
}