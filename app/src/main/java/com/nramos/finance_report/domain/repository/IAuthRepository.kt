package com.nramos.finance_report.domain.repository

import com.nramos.finance_report.data.auth.GoogleSignInResult
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {

    suspend fun login(email: String, password: String): Flow<NetworkResult<UserProfile>>

    suspend fun loginWithGoogle(googleResult: GoogleSignInResult): Flow<NetworkResult<UserProfile>>

    suspend fun register(
        name: String,
        email: String,
        password: String,
        paternalSurname: String? = null,
        maternalSurname: String? = null,
        gender: Char? = null
    ): Flow<NetworkResult<UserProfile>>

    suspend fun logout(): Flow<NetworkResult<Unit>>

    suspend fun isLoggedIn(): Boolean

    suspend fun getCurrentUser(): UserProfile?
}