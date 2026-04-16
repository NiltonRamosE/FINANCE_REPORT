package com.nramos.finance_report.domain.usecase.profile

import com.nramos.finance_report.domain.model.UserProfile
import com.nramos.finance_report.domain.repository.IProfileRepository
import com.nramos.finance_report.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateAvatarUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {
    suspend operator fun invoke(avatarUrl: String): Flow<NetworkResult<UserProfile>> {
        return profileRepository.updateAvatar(avatarUrl)
    }
}