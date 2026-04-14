package com.nramos.finance_report.data.repository

import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.Modality
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModalityRepository @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val client = OkHttpClient()

    suspend fun getModalities(): Flow<NetworkResult<List<Modality>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()

                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/modalities?select=*"

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
                val modalities = mutableListOf<Modality>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val modality = Modality(
                        modalityId = jsonObject.getString("id"),
                        name = jsonObject.getString("name")
                    )
                    modalities.add(modality)
                }

                emit(NetworkResult.Success(modalities))
            } else {
                val errorMsg = "Error al cargar modalidades: $responseBody"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }
}