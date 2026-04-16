package com.nramos.finance_report.data.repository

import android.util.Log
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

                    // 3. Verificar si el perfil ya existe en Supabase (pasando el token)
                    val existingProfile = supabaseAuthManager.getProfileById(supabaseUser.id, supabaseToken)
                    // 4. Crear/actualizar perfil solo si es necesario
                    if (existingProfile == null) {
                        supabaseAuthManager.createProfile(supabaseToken, googleResult)
                    }

                    // 5. Obtener el perfil actualizado desde Supabase (con todos los datos)
                    val fullProfile = supabaseAuthManager.getProfileById(supabaseUser.id, supabaseToken)
                    // 6. Guardar en TokenManager SIN sobrescribir datos existentes
                    val isFirstLogin = !tokenManager.isLoggedIn()
                    if (isFirstLogin || existingProfile == null) {
                        tokenManager.saveAuthData(
                            token = supabaseToken,
                            tokenType = "Bearer",
                            profileId = supabaseUser.id,
                            userName = fullProfile?.name ?: googleResult.name,
                            userEmail = googleResult.email,
                            paternalSurname = fullProfile?.paternalSurname,
                            maternalSurname = fullProfile?.maternalSurname,
                            gender = fullProfile?.gender,
                            avatarUrl = fullProfile?.avatarUrl
                        )
                    } else {
                        tokenManager.updateAuthData(
                            token = supabaseToken,
                            userEmail = googleResult.email
                        )
                    }

                    val user = UserProfile(
                        profileId = supabaseUser.id,
                        name = fullProfile?.name ?: googleResult.name,
                        email = googleResult.email,
                        paternalSurname = fullProfile?.paternalSurname,
                        maternalSurname = fullProfile?.maternalSurname,
                        gender = fullProfile?.gender,
                        avatarUrl = fullProfile?.avatarUrl
                    )
                    Log.d("AuthRepository", "Emitiendo perfil de usuario: $user")
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
            UserProfile(
                profileId = tokenManager.getUserProfileId(),
                name = tokenManager.getUserName(),
                email = tokenManager.getUserEmail(),
                paternalSurname = tokenManager.getUserPaternalSurname(),
                maternalSurname = tokenManager.getUserMaternalSurname(),
                gender = tokenManager.getUserGender(),
                avatarUrl = tokenManager.getAvatarUrl()
            )
        } else {
            null
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
}