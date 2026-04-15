package com.nramos.finance_report.domain.usecase.auth

import com.nramos.finance_report.domain.repository.IAuthRepository
import javax.inject.Inject

class IsLoggedInUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}