package com.nramos.finance_report.domain.model

data class Modality(
    val modalityId: String,
    val name: String,
) {
    override fun toString(): String {
        return name
    }
}