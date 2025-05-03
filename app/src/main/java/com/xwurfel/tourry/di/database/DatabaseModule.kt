package com.xwurfel.tourry.di.database

import android.content.Context
import androidx.room.Room
import com.xwurfel.tourry.core.AppDatabase
import com.xwurfel.tourry.data.poi.dao.PoiDao
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
        ).build()
    }

    @Provides
    fun providePoiDao(database: AppDatabase): PoiDao {
        return database.poiDao()
    }
}