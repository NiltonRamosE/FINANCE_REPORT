package com.nramos.finance_report.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nramos.finance_report.FinanceReportApp

object ThemeDialog {

    fun show(context: Context, onThemeChanged: (() -> Unit)? = null) {
        val app = FinanceReportApp.instance
        val themes = arrayOf("Claro", "Oscuro", "Sistema")
        val currentTheme = app.getCurrentThemeMode()

        val currentIndex = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Seleccionar Tema")
            .setSingleChoiceItems(themes, currentIndex) { _, which ->
                val newMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                app.saveThemeMode(newMode)
                onThemeChanged?.invoke()
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}