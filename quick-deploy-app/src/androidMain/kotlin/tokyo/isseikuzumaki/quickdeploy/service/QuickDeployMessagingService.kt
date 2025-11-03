package tokyo.isseikuzumaki.quickdeploy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import tokyo.isseikuzumaki.quickdeploy.MainActivity
import tokyo.isseikuzumaki.quickdeploy.R

/**
 * FCM service to receive APK arrival notifications (A-004, C-002)
 */
class QuickDeployMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Token will be sent to server during next device registration
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "FCM message received from: ${message.from}")
        Log.d(TAG, "Message data: ${message.data}")
        Log.d(TAG, "Message notification: ${message.notification?.body}")

        // Check if message contains a notification payload
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Quick Deploy",
                body = notification.body ?: "新しいAPKを受信しました。タップしてインストールします。",
                deviceToken = message.data["deviceToken"],
                downloadUrl = message.data["downloadUrl"]
            )
        }

        // Handle data payload
        if (message.data.isNotEmpty()) {
            handleDataPayload(message.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val deviceToken = data["deviceToken"]
        val downloadUrl = data["downloadUrl"]
        Log.d(TAG, "Handling APK notification for device token: $deviceToken")
        Log.d(TAG, "Download URL: $downloadUrl")

        // Trigger APK download via broadcast
        val intent = Intent(ACTION_APK_AVAILABLE).apply {
            putExtra(EXTRA_DEVICE_TOKEN, deviceToken)
            putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
        }
        sendBroadcast(intent)
    }

    private fun showNotification(title: String, body: String, deviceToken: String?, downloadUrl: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new APK arrivals"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEVICE_TOKEN, deviceToken)
            putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification shown: $title - $body")
    }

    companion object {
        private const val TAG = "QuickDeployFCM"
        private const val CHANNEL_ID = "quick_deploy_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_APK_AVAILABLE = "tokyo.isseikuzumaki.quickdeploy.APK_AVAILABLE"
        const val EXTRA_DEVICE_TOKEN = "deviceToken"
        const val EXTRA_DOWNLOAD_URL = "downloadUrl"
    }
}
