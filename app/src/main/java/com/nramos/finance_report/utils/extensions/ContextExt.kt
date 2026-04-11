package com.nramos.finance_report.utils.extensions

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun showToast(message: String) {
    // Esta función asume que se llama desde una Activity o Context
    // Mejor usar la versión con Context
}