package com.nramos.finance_report.data.repository

import android.util.Log
import com.nramos.finance_report.data.api.ApiService
import com.nramos.finance_report.data.auth.GoogleSignInResult
import com.nramos.finance_report.data.auth.SupabaseUser
import com.nramos.finance_report.data.auth.SupabaseAuthManager
import com.nramos.finance_report.data.datasource.local.TokenManager
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

    override suspend fun logout(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            tokenManager.clearAuthData()

            // Cerrar sesión en Google
            val googleSignOutResult = supabaseAuthManager.signOutFromGoogle()
            if (googleSignOutResult.isFailure) {
                Log.e("AuthRepository", "Error al cerrar sesión en Google: ${googleSignOutResult.exceptionOrNull()?.message}")
            }

            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al cerrar sesión"))
        }
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        return if (tokenManager.isLoggedIn()) {
            val profileId = tokenManager.getUserProfileId()
            val userName = tokenManager.getUserName()
            val userEmail = tokenManager.getUserEmail()

            UserProfile(
                profileId = profileId,
                name = userName,
                email = userEmail,
                paternalSurname = tokenManager.getUserPaternalSurname(),
                maternalSurname = tokenManager.getUserMaternalSurname(),
                gender = tokenManager.getUserGender()
            )
        } else {
            null
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