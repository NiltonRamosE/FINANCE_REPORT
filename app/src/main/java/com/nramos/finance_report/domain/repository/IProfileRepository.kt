package com.nramos.finance_report.domain.repository

import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {
    suspend fun updateProfile(
        name: String,
        paternalSurname: String?,
        maternalSurname: String?,
        gender: Char?,
        avatarUrl: String? = null
    ): Flow<NetworkResult<UserProfile>>
}