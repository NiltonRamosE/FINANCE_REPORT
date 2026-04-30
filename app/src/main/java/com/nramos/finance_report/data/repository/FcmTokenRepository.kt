// data/repository/FcmTokenRepository.kt
package com.nramos.finance_report.data.repository

import android.util.Log
import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "FcmTokenRepository"
    }

    /**
     * Guarda o actualiza el token FCM del usuario en la tabla profiles
     */
    suspend fun saveFcmToken(fcmToken: String) {
        try {
            // Verificar si hay sesión activa
            if (!tokenManager.isLoggedIn()) {
                Log.d(TAG, "No hay sesión activa, no se guarda el token")
                return
            }

            val authToken = tokenManager.getToken()
            if (authToken == null) {
                Log.d(TAG, "Token de autenticación nulo")
                return
            }

            val profileId = tokenManager.getUserProfileId()
            if (profileId.isEmpty()) {
                Log.d(TAG, "No hay profileId, no se guarda el token")
                return
            }

            Log.d(TAG, "Guardando token FCM para usuario: $profileId")
            Log.d(TAG, "Token: $fcmToken")

            val jsonBody = JSONObject().apply {
                put("token_firebase", fcmToken)
            }

            val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$profileId"

            Log.d(TAG, "URL: $url")
            Log.d(TAG, "AuthToken (primeros 20 chars): ${authToken.take(20)}...")

            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response body: $responseBody")

            if (response.isSuccessful) {
                Log.d(TAG, "Token FCM guardado exitosamente en Supabase")
                tokenManager.saveFcmToken(fcmToken)
            } else {
                Log.e(TAG, "Error al guardar token FCM: ${response.code} - $responseBody")

                // Si el error es 401, el token puede haber expirado
                if (response.code == 401) {
                    Log.e(TAG, "Token expirado. El usuario可能需要 volver a iniciar sesión")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al guardar token FCM: ${e.message}", e)
        }
    }

    /**
     * Obtiene el token FCM del usuario actual desde Supabase
     */
    suspend fun getFcmToken(): String? {
        return try {
            if (!tokenManager.isLoggedIn()) return null

            val authToken = tokenManager.getToken() ?: return null
            val profileId = tokenManager.getUserProfileId()
            if (profileId.isEmpty()) return null

            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?select=token_firebase&id=eq.$profileId"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $authToken")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "[]"

            if (response.isSuccessful && responseBody != "[]") {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val json = jsonArray.getJSONObject(0)
                    json.optString("token_firebase").takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener token FCM: ${e.message}")
            null
        }
    }
}