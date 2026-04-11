package com.nramos.finance_report.domain.usecase.auth

import com.nramos.finance_report.data.auth.GoogleSignInResult
import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.repository.IAuthRepository
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(googleResult: GoogleSignInResult): Flow<NetworkResult<UserProfile>> {
        return authRepository.loginWithGoogle(googleResult)
    }
}