package com.xwurfel.tourry.feature.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xwurfel.tourry.R
import com.xwurfel.tourry.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TourryFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received: ${token.take(20)}...")

        // Save token and sync with backend
        fcmTokenManager.updateToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.d("FCM message received from: ${remoteMessage.from}")

        // Handle different message types
        when (remoteMessage.data["type"]) {
            "tour_reminder" -> handleTourReminder(remoteMessage)
            "tour_update" -> handleTourUpdate(remoteMessage)
            "new_tour_available" -> handleNewTourAvailable(remoteMessage)
            "geofence_alert" -> handleGeofenceAlert(remoteMessage)
            else -> handleGenericMessage(remoteMessage)
        }
    }

    private fun handleTourReminder(remoteMessage: RemoteMessage) {
        val tourId = remoteMessage.data["tour_id"]
        val tourTitle = remoteMessage.data["tour_title"] ?: "Your tour"
        val startTime = remoteMessage.data["start_time"]

        val title = "Tour Starting Soon!"
        val body = "$tourTitle starts in 15 minutes. Get ready!"

        showNotification(
            title = title,
            body = body,
            channelId = TOUR_REMINDERS_CHANNEL_ID,
            notificationId = TOUR_REMINDER_NOTIFICATION_ID,
            action = createTourDetailIntent(tourId)
        )
    }

    private fun handleTourUpdate(remoteMessage: RemoteMessage) {
        val tourId = remoteMessage.data["tour_id"]
        val tourTitle = remoteMessage.data["tour_title"] ?: "A tour"
        val updateType = remoteMessage.data["update_type"]

        val title = when (updateType) {
            "cancelled" -> "Tour Cancelled"
            "time_changed" -> "Tour Time Changed"
            "location_changed" -> "Tour Location Updated"
            else -> "Tour Update"
        }

        val body = remoteMessage.notification?.body
            ?: "There's an update for $tourTitle. Tap to view details."

        showNotification(
            title = title,
            body = body,
            channelId = TOUR_UPDATES_CHANNEL_ID,
            notificationId = generateNotificationId(),
            action = createTourDetailIntent(tourId)
        )
    }

    private fun handleNewTourAvailable(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "New Tour Available!"
        val body = remoteMessage.notification?.body
            ?: "A new tour has been added near you. Check it out!"

        showNotification(
            title = title,
            body = body,
            channelId = NEW_TOURS_CHANNEL_ID,
            notificationId = generateNotificationId(),
            action = createExploreIntent()
        )
    }

    private fun handleGeofenceAlert(remoteMessage: RemoteMessage) {
        val tourId = remoteMessage.data["tour_id"]
        val stopName = remoteMessage.data["stop_name"]

        val title = "Welcome to $stopName!"
        val body = "You've arrived at your destination. Enjoy exploring!"

        showNotification(
            title = title,
            body = body,
            channelId = GEOFENCE_ALERTS_CHANNEL_ID,
            notificationId = generateNotificationId(),
            action = createLiveTourIntent(tourId)
        )
    }

    private fun handleGenericMessage(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Tourry"
        val body = remoteMessage.notification?.body ?: "You have a new notification"

        showNotification(
            title = title,
            body = body,
            channelId = GENERAL_CHANNEL_ID,
            notificationId = generateNotificationId(),
            action = createMainIntent()
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        notificationId: Int,
        action: PendingIntent
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(action)
            .setAutoCancel(true)
            .setCategory(getNotificationCategory(channelId))
            .build()

        try {
            notificationManager.notify(notificationId, notification)
            Timber.d("Notification shown: $title")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show notification - permission denied")
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                TOUR_REMINDERS_CHANNEL_ID,
                "Tour Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about upcoming tours"
                enableVibration(true)
                enableLights(true)
            },

            NotificationChannel(
                TOUR_UPDATES_CHANNEL_ID,
                "Tour Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates about your booked tours"
            },

            NotificationChannel(
                NEW_TOURS_CHANNEL_ID,
                "New Tours",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications about new tours in your area"
            },

            NotificationChannel(
                GEOFENCE_ALERTS_CHANNEL_ID,
                "Location Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you arrive at tour stops"
                enableVibration(true)
            },

            NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
        )

        channels.forEach { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationCategory(channelId: String): String {
        return when (channelId) {
            TOUR_REMINDERS_CHANNEL_ID -> NotificationCompat.CATEGORY_REMINDER
            TOUR_UPDATES_CHANNEL_ID -> NotificationCompat.CATEGORY_EVENT
            GEOFENCE_ALERTS_CHANNEL_ID -> NotificationCompat.CATEGORY_LOCATION_SHARING
            else -> NotificationCompat.CATEGORY_MESSAGE
        }
    }

    private fun createMainIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createExploreIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "explore")
        }
        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createTourDetailIntent(tourId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "tour_detail")
            putExtra("tour_id", tourId)
        }
        return PendingIntent.getActivity(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createLiveTourIntent(tourId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "live_tour")
            putExtra("tour_id", tourId)
        }
        return PendingIntent.getActivity(
            this,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    companion object {
        private const val TOUR_REMINDERS_CHANNEL_ID = "tour_reminders"
        private const val TOUR_UPDATES_CHANNEL_ID = "tour_updates"
        private const val NEW_TOURS_CHANNEL_ID = "new_tours"
        private const val GEOFENCE_ALERTS_CHANNEL_ID = "geofence_alerts"
        private const val GENERAL_CHANNEL_ID = "general"

        private const val TOUR_REMINDER_NOTIFICATION_ID = 1000
    }
}