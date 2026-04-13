package com.nramos.finance_report.data.repository

import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.Subcategory
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
class SubcategoryRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getSubcategoriesByCategory(categoryId: String): Flow<NetworkResult<List<Subcategory>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/subcategories?select=*&category_id=eq.$categoryId"

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
                val subcategories = mutableListOf<Subcategory>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val subcategory = Subcategory(
                        subCategoryId = jsonObject.getString("id"),
                        categoryId = jsonObject.getString("category_id"),
                        name = jsonObject.getString("name")
                    )
                    subcategories.add(subcategory)
                }

                emit(NetworkResult.Success(subcategories))
            } else {
                emit(NetworkResult.Error("Error al cargar subcategorías"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun createSubcategory(name: String, categoryId: String): Flow<NetworkResult<Subcategory>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("category_id", categoryId)
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/subcategories"

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
                val subcategoryData = if (trimmedResponse.startsWith("[")) {
                    JSONArray(trimmedResponse).getJSONObject(0)
                } else {
                    JSONObject(trimmedResponse)
                }

                val subcategory = Subcategory(
                    subCategoryId = subcategoryData.getString("id"),
                    categoryId = subcategoryData.getString("category_id"),
                    name = subcategoryData.getString("name")
                )

                emit(NetworkResult.Success(subcategory))
            } else {
                emit(NetworkResult.Error("Error al crear subcategoría: código $code"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }
}