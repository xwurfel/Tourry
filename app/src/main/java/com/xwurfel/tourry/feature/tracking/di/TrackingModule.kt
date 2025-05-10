package com.xwurfel.tourry.feature.tracking.di

import com.google.firebase.database.FirebaseDatabase
import com.xwurfel.tourry.feature.tracking.api.GroupApi
import com.xwurfel.tourry.feature.tracking.data.repository.GroupRepositoryImpl
import com.xwurfel.tourry.feature.tracking.domain.repository.GroupRepository
import com.xwurfel.tourry.feature.tracking.firebase.LocationSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideLocationSyncManager(firebaseDatabase: FirebaseDatabase): LocationSyncManager {
        return LocationSyncManager(firebaseDatabase)
    }

    @Provides
    @Singleton
    fun provideGroupApi(retrofit: Retrofit): GroupApi {
        return retrofit.create(GroupApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGroupRepository(
        groupApi: GroupApi,
        locationSyncManager: LocationSyncManager,
        @com.xwurfel.tourry.core.di.IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): GroupRepository {
        return GroupRepositoryImpl(groupApi, locationSyncManager, ioDispatcher)
    }
}