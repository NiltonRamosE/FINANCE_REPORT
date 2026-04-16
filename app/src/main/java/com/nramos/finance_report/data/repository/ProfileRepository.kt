package com.nramos.finance_report.data.repository

import android.util.Log
import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.repository.IProfileRepository
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val tokenManager: TokenManager
) : IProfileRepository {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun updateProfile(
        name: String,
        paternalSurname: String?,
        maternalSurname: String?,
        gender: Char?
    ): Flow<NetworkResult<UserProfile>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val profileId = tokenManager.getUserProfileId()

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    if (!paternalSurname.isNullOrEmpty()) {
                        put("paternal_surname", paternalSurname)
                    }
                    if (!maternalSurname.isNullOrEmpty()) {
                        put("maternal_surname", maternalSurname)
                    }
                    if (gender != null) {
                        put("gender", gender.toString())
                    }
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$profileId"

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

                Triple(response.isSuccessful, response.code, responseBody)
            }

            val (isSuccessful, code, responseBody) = result

            if (isSuccessful) {
                // Actualizar TokenManager con los nuevos datos
                tokenManager.saveAuthData(
                    token = tokenManager.getToken()!!,
                    tokenType = "Bearer",
                    profileId = tokenManager.getUserProfileId(),
                    userName = name,
                    userEmail = tokenManager.getUserEmail(),
                    paternalSurname = paternalSurname,
                    maternalSurname = maternalSurname,
                    gender = gender
                )

                val updatedUser = UserProfile(
                    profileId = tokenManager.getUserProfileId(),
                    name = name,
                    email = tokenManager.getUserEmail(),
                    paternalSurname = paternalSurname,
                    maternalSurname = maternalSurname,
                    gender = gender
                )

                emit(NetworkResult.Success(updatedUser))
            } else {
                val errorMsg = "Error al actualizar perfil: código $code - $responseBody"
                Log.e("ProfileRepository", errorMsg)
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error: ${e.message}", e)
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Flow<NetworkResult<UserProfile>> = flow {
        emit(NetworkResult.Loading())

        try {
            val result = withContext(Dispatchers.IO) {
                val token = tokenManager.getToken()
                if (token == null) {
                    throw Exception("No hay sesión activa")
                }

                val profileId = tokenManager.getUserProfileId()

                val jsonBody = JSONObject().apply {
                    put("avatar_url", avatarUrl)
                }

                val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$profileId"

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

                Triple(response.isSuccessful, response.code, responseBody)
            }

            val (isSuccessful, code, responseBody) = result

            if (isSuccessful) {
                // Actualizar TokenManager
                tokenManager.saveAvatarUrl(avatarUrl)

                val updatedUser = UserProfile(
                    profileId = tokenManager.getUserProfileId(),
                    name = tokenManager.getUserName(),
                    email = tokenManager.getUserEmail(),
                    paternalSurname = tokenManager.getUserPaternalSurname(),
                    maternalSurname = tokenManager.getUserMaternalSurname(),
                    gender = tokenManager.getUserGender(),
                    avatarUrl = avatarUrl
                )

                emit(NetworkResult.Success(updatedUser))
            } else {
                val errorMsg = "Error al actualizar avatar: código $code"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }
}