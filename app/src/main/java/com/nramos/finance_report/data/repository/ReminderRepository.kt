package com.nramos.finance_report.data.repository

import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.Reminder
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
class ReminderRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    companion object {
        private const val TAG = "ReminderRepository"
    }

    suspend fun getReminders(): Flow<NetworkResult<List<Reminder>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val userId = tokenManager.getUserProfileId()
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/reminders?select=*&user_id=eq.$userId&order=date_time.asc"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "[]"

                Pair(response.isSuccessful, responseBody)
            }

            val (isSuccessful, responseBody) = result

            if (isSuccessful) {
                val jsonArray = JSONArray(responseBody)
                val reminders = mutableListOf<Reminder>()

                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val reminder = Reminder(
                        id = json.getString("id"),
                        userId = json.getString("user_id"),
                        title = json.getString("title"),
                        description = json.optString("description").takeIf { it.isNotEmpty() },
                        dateTime = json.getString("date_time"), // YYYY-MM-DD
                        frequency = json.getString("frequency"),
                        isActive = json.getBoolean("is_active")
                    )
                    reminders.add(reminder)
                }

                emit(NetworkResult.Success(reminders))
            } else {
                emit(NetworkResult.Error("Error al cargar recordatorios"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun createReminder(
        title: String,
        description: String?,
        dateTime: String,
        frequency: String
    ): Flow<NetworkResult<Reminder>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val userId = tokenManager.getUserProfileId()

                // Extraer solo la fecha (YYYY-MM-DD) si viene con hora
                val cleanDate = dateTime.split("T").first().take(10)

                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    put("title", title)
                    if (!description.isNullOrEmpty()) {
                        put("description", description)
                    }
                    put("date_time", cleanDate)
                    put("frequency", frequency)
                    put("is_active", true)
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/reminders"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                Triple(response.isSuccessful, response.code, responseBody)
            }

            val (isSuccessful, code, responseBody) = result

            if (isSuccessful) {
                val trimmedResponse = responseBody.trim()
                val jsonData = if (trimmedResponse.startsWith("[")) {
                    JSONArray(trimmedResponse).getJSONObject(0)
                } else {
                    JSONObject(trimmedResponse)
                }

                val reminder = Reminder(
                    id = jsonData.getString("id"),
                    userId = jsonData.getString("user_id"),
                    title = jsonData.getString("title"),
                    description = jsonData.optString("description").takeIf { it.isNotEmpty() },
                    dateTime = jsonData.getString("date_time"),
                    frequency = jsonData.getString("frequency"),
                    isActive = jsonData.getBoolean("is_active")
                )

                emit(NetworkResult.Success(reminder))
            } else {
                emit(NetworkResult.Error("Error al crear recordatorio"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun updateReminder(
        id: String,
        title: String,
        description: String?,
        dateTime: String,
        frequency: String,
        isActive: Boolean
    ): Flow<NetworkResult<Reminder>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val cleanDate = dateTime.split("T").first().take(10)

                val jsonBody = JSONObject().apply {
                    put("title", title)
                    if (!description.isNullOrEmpty()) {
                        put("description", description)
                    }
                    put("date_time", cleanDate)
                    put("frequency", frequency)
                    put("is_active", isActive)
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/reminders?id=eq.$id"

                val request = Request.Builder()
                    .url(url)
                    .patch(requestBody)
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                Pair(response.isSuccessful, responseBody)
            }

            val (isSuccessful, responseBody) = result

            if (isSuccessful) {
                val trimmedResponse = responseBody.trim()
                val jsonData = if (trimmedResponse.startsWith("[")) {
                    JSONArray(trimmedResponse).getJSONObject(0)
                } else {
                    JSONObject(trimmedResponse)
                }

                val reminder = Reminder(
                    id = jsonData.getString("id"),
                    userId = jsonData.getString("user_id"),
                    title = jsonData.getString("title"),
                    description = jsonData.optString("description").takeIf { it.isNotEmpty() },
                    dateTime = jsonData.getString("date_time"),
                    frequency = jsonData.getString("frequency"),
                    isActive = jsonData.getBoolean("is_active")
                )

                emit(NetworkResult.Success(reminder))
            } else {
                emit(NetworkResult.Error("Error al actualizar recordatorio"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun deleteReminder(id: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/reminders?id=eq.$id"

                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            }

            if (result) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Error al eliminar recordatorio"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }
}