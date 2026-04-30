// service/FcmTokenService.kt
package com.nramos.finance_report.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.nramos.finance_report.data.repository.FcmTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenService @Inject constructor(
    private val fcmTokenRepository: FcmTokenRepository
) {

    companion object {
        private const val TAG = "FcmTokenService"
        private const val RETRY_DELAY_MS = 5000L // 5 segundos
    }

    /**
     * Obtiene el token FCM actual y lo guarda en Supabase
     */
    fun refreshAndSaveToken(retryCount: Int = 0) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d(TAG, "Token FCM obtenido: ${token.take(50)}...")

            CoroutineScope(Dispatchers.IO).launch {
                // Esperar un poco para asegurar que la sesión está establecida
                delay(2000)
                fcmTokenRepository.saveFcmToken(token)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error al obtener token FCM: ${e.message}")

            // Reintentar después de un delay
            if (retryCount < 3) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(RETRY_DELAY_MS)
                    refreshAndSaveToken(retryCount + 1)
                }
            }
        }
    }
}