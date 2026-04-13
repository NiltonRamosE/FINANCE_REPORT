package com.nramos.finance_report.domain.usecase.auth

import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.repository.IAuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): UserProfile? {
        return authRepository.getCurrentUserProfile()
    }
}