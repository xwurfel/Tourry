package com.xwurfel.tourry.feature.analytics.di

import android.content.Context
import com.google.android.datatransport.runtime.dagger.Provides
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideTourAnalytics(
        @ApplicationContext context: Context
    ): TourAnalytics {
        return TourAnalytics(context)
    }
}