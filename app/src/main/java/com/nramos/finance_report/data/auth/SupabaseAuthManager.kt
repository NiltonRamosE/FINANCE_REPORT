package com.nramos.finance_report.data.auth

import android.util.Log
import com.nramos.finance_report.BuildConfig
import com.nramos.finance_report.domain.model.UserProfile
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
class SupabaseAuthManager @Inject constructor() {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun exchangeGoogleToken(googleIdToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("provider", "google")
                put("id_token", googleIdToken)
            }

            val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=id_token")
                .post(requestBody)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                Result.success(accessToken)
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optString("error_description", "Error desconocido")
                } catch (e: Exception) {
                    "Error al parsear respuesta: ${e.message}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(accessToken: String): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                .get()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val user = SupabaseUser(
                    id = json.getString("id"),
                    email = json.getString("email"),
                    name = json.optString("user_metadata").let {
                        JSONObject(it).optString("full_name", json.optString("email"))
                    }
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Error al obtener perfil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfileById(profileId: String, accessToken: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$profileId")
                .get()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "[]"
            if (response.isSuccessful && responseBody != "[]") {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val json = jsonArray.getJSONObject(0)
                    UserProfile(
                        profileId = json.getString("id"),
                        name = json.getString("name"),
                        paternalSurname = json.optString("paternal_surname").takeIf { it.isNotEmpty() },
                        maternalSurname = json.optString("maternal_surname").takeIf { it.isNotEmpty() },
                        gender = json.optString("gender").takeIf { it.isNotEmpty() }?.first(),
                        avatarUrl = json.optString("avatar_url").takeIf { it.isNotEmpty() }
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error getting profile: ${e.message}")
            null
        }
    }

    suspend fun createProfile(accessToken: String, googleUser: GoogleSignInResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userResult = getUserProfile(accessToken)
            if (userResult.isFailure) {
                return@withContext Result.failure(userResult.exceptionOrNull() ?: Exception("Error al obtener usuario"))
            }

            val supabaseUser = userResult.getOrNull()!!

            val profileJson = JSONObject().apply {
                put("id", supabaseUser.id)
                put("name", googleUser.name)
            }

            val insertRequest = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/rest/v1/profiles")
                .post(profileJson.toString().toRequestBody(jsonMediaType))
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            val insertResponse = client.newCall(insertRequest).execute()

            if (insertResponse.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al crear perfil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun signOutFromGoogle(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SupabaseUser(
    val id: String,
    val email: String,
    val name: String
)