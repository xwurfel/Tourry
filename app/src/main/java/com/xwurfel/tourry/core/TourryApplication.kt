package com.xwurfel.tourry.core

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.xwurfel.tourry.BuildConfig
import com.xwurfel.tourry.core.debug.FirebaseDebugHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TourryApplication : Application() {

    @Inject
    lateinit var firebaseDebugHelper: FirebaseDebugHelper

    override fun onCreate() {
        super.onCreate()

        initializeLogging()
        initializeFirebase()

        configureFirebaseServices()

        if (BuildConfig.DEBUG) {
            firebaseDebugHelper.checkFirebaseConfiguration(this)
        }

        Timber.d("TourryApplication initialized successfully")
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Timber.d("Firebase initialized successfully")
            } else {
                Timber.d("Firebase already initialized")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun configureFirebaseServices() {
        try {
            configureFirestore()
            configureStorage()
            configureAnalytics()
            configureCrashlytics()
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure Firebase services")
        }
    }

    private fun configureFirestore() {
        try {
            val firestore = FirebaseFirestore.getInstance()

            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enable offline persistence
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            firestore.firestoreSettings = settings

            if (BuildConfig.DEBUG) {
                FirebaseFirestore.setLoggingEnabled(true)
            }

            Timber.d("Firestore configured successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure Firestore")
        }
    }

    private fun configureStorage() {
        try {
            val storage = FirebaseStorage.getInstance()

            storage.maxDownloadRetryTimeMillis = 60000
            storage.maxUploadRetryTimeMillis = 120000
            storage.maxOperationRetryTimeMillis = 60000

            Timber.d("Firebase Storage configured successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure Firebase Storage")
        }
    }

    private fun configureAnalytics() {
        try {
            val analytics = FirebaseAnalytics.getInstance(this)

            // Set analytics collection enabled based on build type
            analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

            // Set default parameters
            analytics.setDefaultEventParameters(
                mapOf(
                    "app_version" to BuildConfig.VERSION_NAME,
                    "build_type" to BuildConfig.BUILD_TYPE
                ).toBundle()
            )

            Timber.d("Firebase Analytics configured successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure Firebase Analytics")
        }
    }

    private fun configureCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)

            Timber.d("Firebase Crashlytics configured successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure Firebase Crashlytics")
        }
    }

    private fun Map<String, Any>.toBundle(): android.os.Bundle {
        val bundle = android.os.Bundle()
        forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }

    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= android.util.Log.WARN) {
                val crashlytics = FirebaseCrashlytics.getInstance()

                if (t != null) {
                    crashlytics.recordException(t)
                } else {
                    crashlytics.log("$tag: $message")
                }
            }
        }
    }
}