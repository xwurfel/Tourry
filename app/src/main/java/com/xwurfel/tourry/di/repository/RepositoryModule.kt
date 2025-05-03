package com.xwurfel.tourry.di.repository

import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.repository.BookingRepositoryImpl
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.repository.TourCategoryRepositoryImpl
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.repository.PoiRepositoryImpl
import com.xwurfel.tourry.data.poi.source.PoiDataSource
import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.tour.repository.TourRepositoryImpl
import com.xwurfel.tourry.data.user.dao.UserDao
import com.xwurfel.tourry.data.user.repository.UserRepositoryImpl
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import com.xwurfel.tourry.domain.user.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun providePoiDataSource(poiDao: PoiDao): PoiDataSource {
        return PoiDataSource(poiDao)
    }

    @Singleton
    @Provides
    fun providePoiRepository(poiDataSource: PoiDataSource): PoiRepository {
        return PoiRepositoryImpl(poiDataSource)
    }

    @Singleton
    @Provides
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepositoryImpl(userDao)
    }

    @Singleton
    @Provides
    fun provideTourRepository(tourDao: TourDao): TourRepository {
        return TourRepositoryImpl(tourDao)
    }

    @Singleton
    @Provides
    fun provideBookingRepository(bookingDao: BookingDao): BookingRepository {
        return BookingRepositoryImpl(bookingDao)
    }

    @Singleton
    @Provides
    fun provideTourCategoryRepository(tourCategoryDao: TourCategoryDao): TourCategoryRepository {
        return TourCategoryRepositoryImpl(tourCategoryDao)
    }
}