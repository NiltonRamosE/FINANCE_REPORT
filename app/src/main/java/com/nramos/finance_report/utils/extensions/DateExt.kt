package com.nramos.finance_report.utils.extensions

fun String.formatToDisplayDate(): String {
    return try {
        val cleanDate = if (contains("T")) substringBefore("T") else this
        val parts = cleanDate.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}