package com.nramos.finance_report.domain.usecase.auth

import com.nramos.finance_report.domain.model.User
import com.nramos.finance_report.domain.repository.IAuthRepository
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Flow<NetworkResult<User>> {
        return authRepository.login(email, password)
    }
}