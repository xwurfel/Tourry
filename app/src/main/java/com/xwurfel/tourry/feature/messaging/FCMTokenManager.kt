package com.xwurfel.tourry.feature.messaging

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.fcmDataStore: DataStore<Preferences> by preferencesDataStore(name = "fcm_preferences")

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fcmTokenKey = stringPreferencesKey("fcm_token")
    private val lastSyncedTokenKey = stringPreferencesKey("last_synced_token")

    /**
     * Flow of the current FCM token
     */
    val currentToken: Flow<String?> = context.fcmDataStore.data.map { preferences ->
        preferences[fcmTokenKey]
    }

    /**
     * Initialize FCM and get the current token
     */
    fun initialize() {
        scope.launch {
            try {
                requestAndSaveToken()
                syncTokenWithBackend()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize FCM token")
            }
        }
    }

    /**
     * Update the FCM token (called from FirebaseMessagingService)
     */
    fun updateToken(newToken: String) {
        scope.launch {
            try {
                // Save token locally
                context.fcmDataStore.edit { preferences ->
                    preferences[fcmTokenKey] = newToken
                }

                Timber.d("FCM token updated locally")

                // Sync with backend if user is authenticated
                syncTokenWithBackend()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update FCM token")
            }
        }
    }

    /**
     * Request new token from Firebase Messaging
     */
    suspend fun requestAndSaveToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()

            context.fcmDataStore.edit { preferences ->
                preferences[fcmTokenKey] = token
            }

            Timber.d("FCM token retrieved and saved: ${token.take(20)}...")
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve FCM token")
            throw e
        }
    }

    /**
     * Sync FCM token with backend (Firestore)
     */
    suspend fun syncTokenWithBackend() {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Timber.d("User not authenticated, skipping FCM token sync")
                return
            }

            val localToken = context.fcmDataStore.data.first()[fcmTokenKey]
            if (localToken == null) {
                Timber.w("No FCM token available to sync")
                return
            }

            val lastSyncedToken = context.fcmDataStore.data.first()[lastSyncedTokenKey]
            if (localToken == lastSyncedToken) {
                Timber.d("FCM token already synced with backend")
                return
            }

            // Update user document with FCM token
            val userDocRef = firestore.collection("users").document(currentUser.uid)

            userDocRef.update(
                mapOf(
                    "fcmToken" to localToken,
                    "fcmTokenUpdatedAt" to com.google.firebase.Timestamp.now(),
                    "deviceInfo" to mapOf(
                        "platform" to "android",
                        "appVersion" to getAppVersion()
                    )
                )
            ).await()

            // Mark as synced
            context.fcmDataStore.edit { preferences ->
                preferences[lastSyncedTokenKey] = localToken
            }

            Timber.d("FCM token synced with backend successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync FCM token with backend")
        }
    }

    /**
     * Subscribe to a topic for receiving notifications
     */
    suspend fun subscribeToTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Timber.d("Subscribed to FCM topic: $topic")
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to FCM topic: $topic")
        }
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Timber.d("Unsubscribed from FCM topic: $topic")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unsubscribe from FCM topic: $topic")
        }
    }

    /**
     * Subscribe to user-specific topics based on preferences
     */
    suspend fun updateTopicSubscriptions(
        notificationsEnabled: Boolean = true,
        tourRemindersEnabled: Boolean = true,
        newToursNearbyEnabled: Boolean = true,
        userLocation: String? = null
    ) {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Timber.d("User not authenticated, skipping topic subscriptions")
                return
            }

            // Unsubscribe from all topics first
            unsubscribeFromAllTopics()

            if (!notificationsEnabled) {
                Timber.d("Notifications disabled, skipping topic subscriptions")
                return
            }

            // Subscribe to general topics
            if (tourRemindersEnabled) {
                subscribeToTopic("tour_reminders_${currentUser.uid}")
            }

            if (newToursNearbyEnabled && userLocation != null) {
                subscribeToTopic("new_tours_$userLocation")
            }

            // Subscribe to user-specific topic
            subscribeToTopic("user_${currentUser.uid}")

            Timber.d("Topic subscriptions updated successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update topic subscriptions")
        }
    }

    /**
     * Clear FCM token when user signs out
     */
    suspend fun clearToken() {
        try {
            context.fcmDataStore.edit { preferences ->
                preferences.remove(fcmTokenKey)
                preferences.remove(lastSyncedTokenKey)
            }

            // Unsubscribe from all topics
            unsubscribeFromAllTopics()

            Timber.d("FCM token cleared successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear FCM token")
        }
    }

    private suspend fun unsubscribeFromAllTopics() {
        val commonTopics = listOf(
            "general_announcements",
            "app_updates",
            "tour_updates"
        )

        commonTopics.forEach { topic ->
            try {
                unsubscribeFromTopic(topic)
            } catch (e: Exception) {
                Timber.w(e, "Failed to unsubscribe from topic: $topic")
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}