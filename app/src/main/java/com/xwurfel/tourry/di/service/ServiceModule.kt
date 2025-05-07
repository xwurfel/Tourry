package com.xwurfel.tourry.di.service

import android.content.Context
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.data.network.api.FileUploadApi
import com.xwurfel.tourry.data.upload.FileUploadService
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideFileUploadService(
        fileUploadApi: FileUploadApi,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FileUploadService {
        return FileUploadService(fileUploadApi, context, ioDispatcher)
    }
}