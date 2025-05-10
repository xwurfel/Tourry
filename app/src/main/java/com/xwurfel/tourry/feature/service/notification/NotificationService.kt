package com.xwurfel.tourry.feature.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val GEOFENCE_CHANNEL_ID = "geofence_notification_channel"
        const val REGROUP_CHANNEL_ID = "regroup_notification_channel"

        const val GEOFENCE_EXIT_NOTIFICATION_ID = 2001
        const val REGROUP_NOTIFICATION_ID = 2002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Geofence notification channel
        val geofenceChannel = NotificationChannel(
            GEOFENCE_CHANNEL_ID,
            context.getString(R.string.geofence_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.geofence_channel_description)
        }

        val regroupChannel = NotificationChannel(
            REGROUP_CHANNEL_ID,
            context.getString(R.string.regroup_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.regroup_channel_description)
        }

        notificationManager.createNotificationChannel(geofenceChannel)
        notificationManager.createNotificationChannel(regroupChannel)
    }

    fun showGeofenceExitNotification(context: Context, groupId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("groupId", groupId)
            putExtra("openTracking", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.geofence_exit_title))
            .setContentText(context.getString(R.string.geofence_exit_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(GEOFENCE_EXIT_NOTIFICATION_ID, notification)
    }

    fun showRegroupNotification(context: Context, groupId: String, groupName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("groupId", groupId)
            putExtra("openTracking", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REGROUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.regroup_title))
            .setContentText(context.getString(R.string.regroup_text, groupName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(REGROUP_NOTIFICATION_ID, notification)
    }
}