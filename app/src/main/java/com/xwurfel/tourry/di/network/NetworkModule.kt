package com.xwurfel.tourry.di.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.data.network.api.AuthApi
import com.xwurfel.tourry.data.network.api.BookingApi
import com.xwurfel.tourry.data.network.api.CategoryApi
import com.xwurfel.tourry.data.network.api.CheckInApi
import com.xwurfel.tourry.data.network.api.FileUploadApi
import com.xwurfel.tourry.data.network.api.RouteApi
import com.xwurfel.tourry.data.network.api.TourApi
import com.xwurfel.tourry.data.network.api.UserApi
import com.xwurfel.tourry.data.network.interceptor.AuthInterceptor
import com.xwurfel.tourry.data.network.util.LocalDateTimeAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTourApi(retrofit: Retrofit): TourApi {
        return retrofit.create(TourApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBookingApi(retrofit: Retrofit): BookingApi {
        return retrofit.create(BookingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRouteApi(retrofit: Retrofit): RouteApi {
        return retrofit.create(RouteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCheckInApi(retrofit: Retrofit): CheckInApi {
        return retrofit.create(CheckInApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCategoryApi(retrofit: Retrofit): CategoryApi {
        return retrofit.create(CategoryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFileUploadApi(retrofit: Retrofit): FileUploadApi {
        return retrofit.create(FileUploadApi::class.java)
    }

    private const val BASE_URL = "http://10.0.2.2:8080/api/"
}