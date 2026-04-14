package com.nramos.finance_report.utils.extensions
import java.text.SimpleDateFormat
import java.util.Locale

fun String.formatToDisplayDate(): String {
    return try {
        // Limpiar la fecha (quitar hora si existe)
        val cleanDate = if (contains("T")) substringBefore("T") else this

        // Parsear de YYYY-MM-DD a DD/MM/YYYY
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

fun String.formatToApiDate(): String {
    return try {
        val parts = split("/")
        if (parts.size == 3) {
            "${parts[2]}-${parts[1]}-${parts[0]}"
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}