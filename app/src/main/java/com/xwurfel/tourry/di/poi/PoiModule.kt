package com.xwurfel.tourry.di.poi

import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.repository.PoiRepositoryImpl
import com.xwurfel.tourry.data.poi.source.PoiDataSource
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object PoiModule {

    @Provides
    fun providePoiDataSource(poiDao: PoiDao): PoiDataSource {
        return PoiDataSource(poiDao)
    }

    @Provides
    fun providePoiRepository(poiDataSource: PoiDataSource): PoiRepository {
        return PoiRepositoryImpl(poiDataSource)
    }
}