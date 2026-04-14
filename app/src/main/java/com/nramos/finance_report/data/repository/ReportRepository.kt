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

    suspend fun getReports(
        type: Char? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<NetworkResult<List<Report>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()

                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                // Construir URL con filtros
                var url = "${BuildConfig.SUPABASE_URL}/rest/v1/reports?select=*&order=created_at.desc"

                type?.let {
                    url += "&type=eq.$it"
                }

                startDate?.let {
                    url += "&date=gte.$it"
                }

                endDate?.let {
                    url += "&date=lte.$it"
                }

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                Pair(response.isSuccessful, responseBody)
            }

            val (isSuccessful, responseBody) = result

            if (isSuccessful) {
                val jsonArray = JSONArray(responseBody)
                val reports = mutableListOf<Report>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val report = Report(
                        reportId = jsonObject.getString("id"),
                        categoryId = jsonObject.getString("category_id"),
                        subcategoryId = jsonObject.optString("subcategory_id").takeIf { it.isNotEmpty() },
                        modalityId = jsonObject.getString("modality_id"),
                        concept = jsonObject.optString("concept").takeIf { it.isNotEmpty() },
                        date = jsonObject.getString("date"),
                        amount = jsonObject.getDouble("amount"),
                        type = jsonObject.getString("type").first()
                    )
                    reports.add(report)
                }

                emit(NetworkResult.Success(reports))
            } else {
                val errorMsg = "Error al cargar movimientos: $responseBody"
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