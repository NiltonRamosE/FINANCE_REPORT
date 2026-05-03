package com.nramos.finance_report

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanceReportApp : Application() {

    companion object {
        lateinit var instance: FinanceReportApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)

        applySavedTheme()
    }

    private fun applySavedTheme() {
        val savedTheme = getSavedThemeMode()
        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }

    private fun getSavedThemeMode(): Int {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        return sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun saveThemeMode(mode: Int) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPref.edit().putInt("theme_mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getCurrentThemeMode(): Int {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        return sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun getCurrentThemeName(): String {
        return when (getCurrentThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Claro"
            AppCompatDelegate.MODE_NIGHT_YES -> "Oscuro"
            else -> "Sistema"
        }
    }
}