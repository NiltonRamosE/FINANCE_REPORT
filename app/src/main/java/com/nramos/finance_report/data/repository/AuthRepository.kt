package com.nramos.finance_report.data.repository

import com.nramos.finance_report.data.api.ApiService
import com.nramos.finance_report.data.api.responses.LoginResponse
import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.data.model.request.LoginRequest
import com.nramos.finance_report.data.model.request.RegisterRequest
import com.nramos.finance_report.domain.model.User
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

    override suspend fun login(email: String, password: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body()?.success == true) {
                val loginData = response.body()?.data
                if (loginData != null) {
                    // Guardar datos de autenticación
                    tokenManager.saveAuthData(
                        token = loginData.token,
                        tokenType = loginData.tokenType,
                        userId = loginData.userId,
                        userName = loginData.name,
                        userEmail = loginData.email
                    )

                    val user = User(
                        userId = loginData.userId,
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

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        paternalSurname: String?,
        maternalSurname: String?,
        gender: Char?
    ): Flow<NetworkResult<User>> = flow {
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
                        userId = userData.userId,
                        userName = userData.name,
                        userEmail = userData.email
                    )

                    val user = User(
                        userId = userData.userId,
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

    override suspend fun getCurrentUser(): User? {
        return if (tokenManager.isLoggedIn()) {
            User(
                userId = tokenManager.getUserId(),
                name = tokenManager.getUserName(),
                email = tokenManager.getUserEmail()
            )
        } else {
            null
        }
    }
}