package com.xwurfel.tourry.feature.discovery.di

import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.feature.discovery.api.TourDiscoveryApi
import com.xwurfel.tourry.feature.discovery.data.repository.TourDiscoveryRepositoryImpl
import com.xwurfel.tourry.feature.discovery.domain.repository.TourDiscoveryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryModule {

    @Provides
    @Singleton
    fun provideTourDiscoveryApi(retrofit: Retrofit): TourDiscoveryApi {
        return retrofit.create(TourDiscoveryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTourDiscoveryRepository(
        api: TourDiscoveryApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): TourDiscoveryRepository {
        return TourDiscoveryRepositoryImpl(api, ioDispatcher)
    }
}