package com.nramos.finance_report.di

import com.nramos.finance_report.data.datasource.local.TokenManager
import com.nramos.finance_report.data.repository.AuthRepository
import com.nramos.finance_report.data.repository.ProfileRepository
import com.nramos.finance_report.domain.repository.IAuthRepository
import com.nramos.finance_report.domain.repository.IProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository = authRepository

    @Provides
    @Singleton
    fun provideProfileRepository(
        tokenManager: TokenManager
    ): IProfileRepository = ProfileRepository(tokenManager)
}