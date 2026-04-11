package com.nramos.finance_report.utils

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun AppCompatActivity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Para mostrar toasts temporales que desaparecen solos
fun AppCompatActivity.showTemporaryToast(message: String, duration: Long = 2000) {
    lifecycleScope.launch {
        showToast(message)
        delay(duration)
        // El toast desaparece automáticamente, no necesitamos hacer nada
    }
}