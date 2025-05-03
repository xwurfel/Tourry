package com.xwurfel.tourry.di.database

import android.content.Context
import androidx.room.Room
import com.xwurfel.tourry.core.AppDatabase
import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.user.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration(false) // For development only, consider proper migrations in production
            .build()
    }

    @Provides
    fun providePoiDao(database: AppDatabase): PoiDao {
        return database.poiDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideTourDao(database: AppDatabase): TourDao {
        return database.tourDao()
    }

    @Provides
    fun provideBookingDao(database: AppDatabase): BookingDao {
        return database.bookingDao()
    }

    @Provides
    fun provideTourCategoryDao(database: AppDatabase): TourCategoryDao {
        return database.tourCategoryDao()
    }
}