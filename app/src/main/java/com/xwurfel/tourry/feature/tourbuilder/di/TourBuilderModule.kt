package com.xwurfel.tourry.feature.tourbuilder.di

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.feature.tourbuilder.api.TourBuilderApi
import com.xwurfel.tourry.feature.tourbuilder.data.repository.TourBuilderRepositoryImpl
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.TourBuilderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TourBuilderModule {

    @Provides
    @Singleton
    fun provideTourBuilderApi(retrofit: Retrofit): TourBuilderApi {
        return retrofit.create(TourBuilderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTourBuilderRepository(
        api: TourBuilderApi,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ): TourBuilderRepository {
        return TourBuilderRepositoryImpl(api, context, ioDispatcher, gson)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}