package com.xwurfel.tourry.feature.tours.data.di

import com.xwurfel.tourry.feature.tours.data.repository.FirebaseTourRepositoryImpl
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TourModule {

    @Binds
    @Singleton
    abstract fun bindTourRepository(repository: FirebaseTourRepositoryImpl): TourRepository
}