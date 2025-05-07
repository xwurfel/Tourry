package com.xwurfel.tourry.di.repository

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.data.auth.repository.AuthRepositoryImpl
import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.repository.BookingRepositoryImpl
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.repository.TourCategoryRepositoryImpl
import com.xwurfel.tourry.data.checkin.dao.CheckInDao
import com.xwurfel.tourry.data.checkin.repository.CheckInRepositoryImpl
import com.xwurfel.tourry.data.network.api.AuthApi
import com.xwurfel.tourry.data.network.api.BookingApi
import com.xwurfel.tourry.data.network.api.CategoryApi
import com.xwurfel.tourry.data.network.api.CheckInApi
import com.xwurfel.tourry.data.network.api.RouteApi
import com.xwurfel.tourry.data.network.api.TourApi
import com.xwurfel.tourry.data.network.api.UserApi
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.repository.PoiRepositoryImpl
import com.xwurfel.tourry.data.poi.source.PoiDataSource
import com.xwurfel.tourry.data.route.dao.RoutePointDao
import com.xwurfel.tourry.data.route.repository.RouteRepositoryImpl
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.tour.repository.TourRepositoryImpl
import com.xwurfel.tourry.data.upload.FileUploadService
import com.xwurfel.tourry.data.user.dao.UserDao
import com.xwurfel.tourry.data.user.repository.UserRepositoryImpl
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.auth.repository.AuthRepository
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import com.xwurfel.tourry.domain.user.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
    fun provideUserRepository(
        userDao: UserDao,
        userApi: UserApi,
        syncDao: SyncDao,
        fileUploadService: FileUploadService,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepositoryImpl(
            userApi,
            userDao,
            syncDao,
            fileUploadService,
            context,
            gson,
            ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideTourRepository(
        tourDao: TourDao,
        tourApi: TourApi,
        syncDao: SyncDao,
        fileUploadService: FileUploadService,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TourRepository {
        return TourRepositoryImpl(
            tourApi,
            tourDao,
            syncDao,
            fileUploadService,
            gson,
            ioDispatcher,
            context
        )
    }

    @Singleton
    @Provides
    fun provideBookingRepository(
        bookingDao: BookingDao,
        bookingApi: BookingApi,
        syncDao: SyncDao,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): BookingRepository {
        return BookingRepositoryImpl(
            bookingApi,
            bookingDao,
            syncDao,
            context,
            gson,
            ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideTourCategoryRepository(
        tourCategoryDao: TourCategoryDao,
        tourCategoryApi: CategoryApi,
        syncDao: SyncDao,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TourCategoryRepository {
        return TourCategoryRepositoryImpl(
            tourCategoryApi,
            tourCategoryDao,
            syncDao,
            context,
            gson,
            ioDispatcher,
        )
    }

    @Singleton
    @Provides
    fun provideRouteRepository(
        routePointDao: RoutePointDao,
        routeApi: RouteApi,
        syncDao: SyncDao,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): RouteRepository {
        return RouteRepositoryImpl(
            routePointDao,
            routeApi,
            syncDao,
            context,
            gson,
            ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideCheckInRepository(
        checkInDao: CheckInDao,
        checkInApi: CheckInApi,
        routePointDao: RoutePointDao,
        syncDao: SyncDao,
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        fileUploadService: FileUploadService
    ): CheckInRepository {
        return CheckInRepositoryImpl(
            checkInApi,
            checkInDao,
            routePointDao,
            fileUploadService,
            syncDao,
            context,
            gson,
            ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        authApi: AuthApi,
        @ApplicationContext context: Context,
        tokenManager: TokenManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, tokenManager, context, ioDispatcher)
    }
}