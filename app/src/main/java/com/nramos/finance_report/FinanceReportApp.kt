package com.nramos.finance_report

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanceReportApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}