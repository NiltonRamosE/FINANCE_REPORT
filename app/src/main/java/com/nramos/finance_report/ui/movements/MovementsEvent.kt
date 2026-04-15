package com.nramos.finance_report.ui.movements

sealed class MovementsEvent {
    data class OnFilterChanged(val filter: String) : MovementsEvent()
}