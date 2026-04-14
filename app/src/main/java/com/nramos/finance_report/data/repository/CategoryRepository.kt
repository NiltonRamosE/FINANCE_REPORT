package com.nramos.finance_report.data.repository

import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.Category
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
class CategoryRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getCategoriesByType(inputType: Char): Flow<NetworkResult<List<Category>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()

                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/categories?select=*&input_type=eq.$inputType"

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
                val categories = mutableListOf<Category>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val category = Category(
                        categoryId = jsonObject.getString("id"),  // Nota: es "id", no "category_id"
                        userId = jsonObject.getString("user_id"),
                        name = jsonObject.getString("name"),
                        inputType = jsonObject.getString("input_type").first(),
                        subcategories = emptyList()
                    )
                    categories.add(category)
                }

                emit(NetworkResult.Success(categories))
            } else {
                val errorMsg = "Error al cargar categorías: $responseBody"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun createCategory(name: String, inputType: Char): Flow<NetworkResult<Category>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()

                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val userId = tokenManager.getUserProfileId()

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("input_type", inputType.toString())
                    put("user_id", userId)
                }


                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/categories"

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
                val categoryData = if (trimmedResponse.startsWith("[")) {
                    JSONArray(trimmedResponse).getJSONObject(0)
                } else {
                    JSONObject(trimmedResponse)
                }

                val category = Category(
                    categoryId = categoryData.getString("id"),
                    userId = categoryData.getString("user_id"),
                    name = categoryData.getString("name"),
                    inputType = categoryData.getString("input_type").first(),
                    subcategories = emptyList()
                )

                emit(NetworkResult.Success(category))
            } else {
                val errorMsg = "Error al crear categoría: código $code - $responseBody"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    suspend fun getCategoryNameById(categoryId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getToken()
                if (token == null) return@withContext "Categoría"

                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/categories?select=name&id=eq.$categoryId"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "[]"

                if (response.isSuccessful && responseBody != "[]") {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getJSONObject(0)
                        jsonObject.getString("name")
                    } else {
                        "Categoría"
                    }
                } else {
                    "Categoría"
                }
            } catch (e: Exception) {
                "Categoría"
            }
        }
    }

    suspend fun getCategoriesMap(ids: List<String>): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getToken()
                if (token == null) return@withContext emptyMap()

                val idsFilter = ids.joinToString(",") { "\"$it\"" }
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/categories?select=id,name&id=in.($idsFilter)"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "[]"

                if (response.isSuccessful) {
                    val jsonArray = JSONArray(responseBody)
                    val map = mutableMapOf<String, String>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        map[jsonObject.getString("id")] = jsonObject.getString("name")
                    }
                    map
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}