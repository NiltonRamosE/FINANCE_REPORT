package com.nramos.finance_report.data.repository

import com.nramos.finance_report.data.api.ApiService
import com.nramos.finance_report.data.auth.GoogleSignInResult
import com.nramos.finance_report.data.auth.SupabaseUser
import com.nramos.finance_report.data.auth.SupabaseAuthManager
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.data.model.request.LoginRequest
import com.nramos.finance_report.data.model.request.RegisterRequest
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.repository.IAuthRepository
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : IAuthRepository {

    private val supabaseAuthManager: SupabaseAuthManager = SupabaseAuthManager()

    override suspend fun login(email: String, password: String): Flow<NetworkResult<UserProfile>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body()?.success == true) {
                val loginData = response.body()?.data
                if (loginData != null) {
                    tokenManager.saveAuthData(
                        token = loginData.token,
                        tokenType = loginData.tokenType,
                        profileId = loginData.profileId,
                        userName = loginData.name,
                        userEmail = loginData.email
                    )

                    val user = UserProfile(
                        profileId = loginData.profileId,
                        name = loginData.name,
                        email = loginData.email
                    )

                    emit(NetworkResult.Success(user))
                } else {
                    emit(NetworkResult.Error("Datos de usuario no encontrados"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Error en el servidor"
                emit(NetworkResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun loginWithGoogle(googleResult: GoogleSignInResult): Flow<NetworkResult<UserProfile>> = flow {
        emit(NetworkResult.Loading())

        try {
            // 1. Intercambiar token de Google por JWT de Supabase
            val supabaseResult = supabaseAuthManager.exchangeGoogleToken(googleResult.idToken)

            if (supabaseResult.isSuccess) {
                val supabaseToken = supabaseResult.getOrNull()!!

                // 2. Obtener perfil del usuario desde Supabase
                val userProfile = supabaseAuthManager.getUserProfile(supabaseToken)

                if (userProfile.isSuccess) {
                    val supabaseUser = userProfile.getOrNull()!!

                    // 3. Crear/actualizar perfil en tu tabla profiles con los datos de Google
                    val createProfileResult = supabaseAuthManager.createOrUpdateProfile(
                        supabaseToken,
                        googleResult
                    )

                    if (createProfileResult.isFailure) {
                        emit(NetworkResult.Error("Error al crear perfil de usuario"))
                        return@flow
                    }

                    // 4. Guardar token y datos del usuario
                    tokenManager.saveAuthData(
                        token = supabaseToken,
                        tokenType = "Bearer",
                        profileId = supabaseUser.id,
                        userName = googleResult.name,
                        userEmail = googleResult.email
                    )

                    val user = UserProfile(
                        profileId = supabaseUser.id,
                        name = googleResult.name,
                        email = googleResult.email
                    )

                    emit(NetworkResult.Success(user))
                } else {
                    emit(NetworkResult.Error("Error al obtener perfil de usuario"))
                }
            } else {
                val error = supabaseResult.exceptionOrNull()
                emit(NetworkResult.Error(error?.message ?: "Error al autenticar con Google"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }

    private suspend fun ensureUserProfileExists(supabaseUser: SupabaseUser) {
        // Verificar si el usuario existe en tu tabla profiles
        // Si no existe, crearlo con los datos de Google
        // Implementa esto según tu lógica
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        paternalSurname: String?,
        maternalSurname: String?,
        gender: Char?
    ): Flow<NetworkResult<UserProfile>> = flow {
        emit(NetworkResult.Loading())

        try {
            val request = RegisterRequest(
                name = name,
                email = email,
                password = password,
                paternalSurname = paternalSurname,
                maternalSurname = maternalSurname,
                gender = gender
            )

            val response = apiService.register(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val userData = response.body()?.data
                if (userData != null) {
                    tokenManager.saveAuthData(
                        token = userData.token,
                        tokenType = userData.tokenType,
                        profileId = userData.profileId,
                        userName = userData.name,
                        userEmail = userData.email
                    )

                    val user = UserProfile(
                        profileId = userData.profileId,
                        name = userData.name,
                        email = userData.email
                    )

                    emit(NetworkResult.Success(user))
                } else {
                    emit(NetworkResult.Error("Error al registrar usuario"))
                }
            } else {
                emit(NetworkResult.Error(response.body()?.message ?: "Error en el registro"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun logout(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val token = tokenManager.getToken() ?: ""
            val response = apiService.logout("Bearer $token")

            if (response.isSuccessful) {
                tokenManager.clearAuthData()
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Error al cerrar sesión"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error de conexión"))
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    override suspend fun getCurrentUser(): UserProfile? {
        return if (tokenManager.isLoggedIn()) {
            UserProfile(
                profileId = tokenManager.getUserProfileId(),
                name = tokenManager.getUserName(),
                email = tokenManager.getUserEmail()
            )
        } else {
            null
        }
    }
}