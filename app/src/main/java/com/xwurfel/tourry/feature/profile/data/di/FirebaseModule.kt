package com.xwurfel.tourry.feature.profile.data.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.xwurfel.tourry.feature.profile.data.repository.FirebaseUserRepositoryImpl
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInOptions(@ApplicationContext context: Context): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId(context))
            .requestEmail()
            .requestProfile()
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        gso: GoogleSignInOptions
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseUserRepository: FirebaseUserRepositoryImpl
    ): UserRepository = firebaseUserRepository

    private fun getWebClientId(context: Context): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

        return if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            throw IllegalStateException(
                "Google Web Client ID not found. Ensure google-services.json is properly configured."
            )
        }
    }
}

/**
 * Separate module for Firebase configuration management
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseConfigurationModule {

    @Provides
    @Singleton
    fun provideFirebaseConfiguration(
        @ApplicationContext context: Context
    ): FirebaseConfiguration {
        return FirebaseConfiguration(context)
    }
}

/**
 * Configuration helper for Firebase services
 */
class FirebaseConfiguration(private val context: Context) {

    fun getProjectId(): String {
        return getStringResource("project_id") ?: "unknown"
    }

    fun getApplicationId(): String {
        return getStringResource("google_app_id") ?: "unknown"
    }

    fun getApiKey(): String {
        return getStringResource("google_api_key") ?: "unknown"
    }

    fun getDatabaseUrl(): String {
        return getStringResource("firebase_database_url") ?: ""
    }

    fun getStorageBucket(): String {
        return getStringResource("google_storage_bucket") ?: ""
    }

    fun isEmulatorMode(): Boolean {
        return try {
            context.resources.getBoolean(
                context.resources.getIdentifier(
                    "firebase_emulator_mode",
                    "bool",
                    context.packageName
                )
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun getStringResource(name: String): String? {
        return try {
            val resourceId = context.resources.getIdentifier(name, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}