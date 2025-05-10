package com.xwurfel.tourry.core.network.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwurfel.tourry.core.network.interceptor.AuthInterceptor
import com.xwurfel.tourry.core.network.interceptor.NetworkConnectivityInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(dataStore: DataStore<Preferences>): AuthInterceptor {
        return AuthInterceptor(dataStore)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkConnectivityInterceptor: NetworkConnectivityInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(networkConnectivityInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // TODO: Add base url
            .baseUrl("https://api.tourry.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}