package com.nramos.finance_report.di

import com.nramos.finance_report.domain.repository.IAuthRepository
import com.nramos.finance_report.domain.repository.IProfileRepository
import com.nramos.finance_report.domain.usecase.auth.LoginWithGoogleUseCase
import com.nramos.finance_report.domain.usecase.profile.UpdateAvatarUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(
        authRepository: IAuthRepository
    ): LoginWithGoogleUseCase {
        return LoginWithGoogleUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateAvatarUseCase(
        profileRepository: IProfileRepository
    ): UpdateAvatarUseCase = UpdateAvatarUseCase(profileRepository)
}