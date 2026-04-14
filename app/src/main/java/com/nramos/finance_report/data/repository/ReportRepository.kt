package com.nramos.finance_report.data.repository

import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.Report
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
class ReportRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun createReport(
        categoryId: String,
        subcategoryId: String?,
        modalityId: String,
        concept: String?,
        date: String,
        amount: Double,
        type: Char
    ): Flow<NetworkResult<Report>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()

                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val formattedDate = formatDateForApi(date)

                val jsonBody = JSONObject().apply {
                    put("category_id", categoryId)
                    if (subcategoryId != null && subcategoryId.isNotEmpty()) {
                        put("subcategory_id", subcategoryId)
                    }
                    put("modality_id", modalityId)
                    put("concept", concept ?: "")
                    put("date", formattedDate)
                    put("amount", amount)
                    put("type", type.toString())
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/reports"

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

                // La respuesta puede ser un array o un objeto
                val trimmedResponse = responseBody.trim()
                val reportData = if (trimmedResponse.startsWith("[")) {
                    JSONArray(trimmedResponse).getJSONObject(0)
                } else {
                    JSONObject(trimmedResponse)
                }

                val report = Report(
                    reportId = reportData.getString("id"),
                    categoryId = reportData.getString("category_id"),
                    subcategoryId = reportData.optString("subcategory_id").takeIf { it.isNotEmpty() },
                    modalityId = reportData.getString("modality_id"),
                    concept = reportData.optString("concept").takeIf { it.isNotEmpty() },
                    date = reportData.getString("date"),
                    amount = reportData.getDouble("amount"),
                    type = reportData.getString("type").first()
                )

                emit(NetworkResult.Success(report))
            } else {
                val errorMsg = "Error al guardar movimiento: código $code - $responseBody"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    private fun formatDateForApi(date: String): String {
        return try {
            val parts = date.split("/")
            if (parts.size == 3) {
                "${parts[2]}-${parts[1]}-${parts[0]}"
            } else {
                date
            }
        } catch (e: Exception) {
            date
        }
    }
}