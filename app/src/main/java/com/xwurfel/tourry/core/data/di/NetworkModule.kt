package com.xwurfel.tourry.core.data.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.xwurfel.tourry.core.data.interceptor.ErrorMappingInterceptor
import com.xwurfel.tourry.core.data.interceptor.NetworkConnectionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(context: Context): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(NetworkConnectionInterceptor(context))
                .addInterceptor(ErrorMappingInterceptor())
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            val json = Json {
                ignoreUnknownKeys = true
            }
            val contentType = "application/json".toMediaType()

            return Retrofit
                .Builder()
                .addConverterFactory(json.asConverterFactory(contentType))
                // TODO: Replace with BuildConfig
                .baseUrl("BuildConfig.API_URL")
                .client(okHttpClient)
                .build()
        }
    }
}