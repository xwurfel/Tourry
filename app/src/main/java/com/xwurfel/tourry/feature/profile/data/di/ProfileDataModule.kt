package com.xwurfel.tourry.feature.profile.data.di

import com.xwurfel.tourry.feature.profile.data.repository.UserRepositoryImpl
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileDataModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}