package com.example.vamsapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vamsapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.model.UpdateDeviceTokenRequest

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val SIREN_CHANNEL_ID = "vams_siren_alerts_v8"
        private const val BEEP_CHANNEL_ID = "vams_beep_alerts_v8"
        private const val DEFAULT_CHANNEL_ID = "vams_default_alerts_v8"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            VamsPrefs.init(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VamsPrefs in onCreate", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        try {
            VamsPrefs.init(applicationContext)
            val userToken = VamsPrefs.getAuthToken()
            if (!userToken.isNullOrEmpty()) {
                ApiClient.apiService.updateDeviceToken(UpdateDeviceTokenRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                    override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                        Log.d(TAG, "Successfully sent updated FCM token to backend: $token")
                    }
                    override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                        Log.e(TAG, "Failed to send updated FCM token to backend", t)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onNewToken", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Allow notifications to trigger heads-up banners only if app is in background/closed
        if (MainActivity.isAppInForeground) {
            return
        }

        val title: String
        val message: String
        val severity: String
        val soundProfile: String
        val alertId: String

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            title = remoteMessage.data["title"] ?: "VAMS Alert"
            message = remoteMessage.data["message"] ?: ""
            alertId = remoteMessage.data["alertId"] ?: ""
            val titleUpper = title.uppercase()
            val isBroadcast = alertId.equals("BROADCAST", ignoreCase = true) || alertId.startsWith("BROADCAST", ignoreCase = true) || titleUpper.contains("BROADCAST")
            severity = remoteMessage.data["severity"] ?: (if (isBroadcast) "HIGH" else "INFO")
            soundProfile = remoteMessage.data["soundProfile"] ?: (if (isBroadcast) "ALERT" else "ALERT")
        } else if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification!!
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            title = notification.title ?: "VAMS Alert"
            message = notification.body ?: ""
            alertId = ""
            val titleUpper = title.uppercase()
            val isBroadcast = titleUpper.contains("BROADCAST")
            severity = if (isBroadcast) "HIGH" else "INFO"
            soundProfile = if (isBroadcast) "ALERT" else "ALERT"
        } else {
            return
        }

        // Deduplicate using shared preferences
        if (!alertId.isNullOrEmpty()) {
            val lastBeep = VamsPrefs.getAlertLastBeepTime(alertId)
            val now = System.currentTimeMillis()
            if (now - lastBeep < 3000) {
                Log.d(TAG, "Duplicate FCM notification detected for alert $alertId within 3s. Skipping to prevent double notifications.")
                return
            }
            VamsPrefs.setAlertLastBeepTime(alertId, now)
        }

        // Post a heads-up notification with custom channel sound based on severity and sound profile
        sendNotification(title, message, alertId, severity, soundProfile)
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val packageName = packageName
                val isEmulator = SoundService.isRunningOnEmulator()
                
                // 1. Siren Channel
                val sirenUri = android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.siren}")
                val sirenAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                val sirenChannel = NotificationChannel(
                    SIREN_CHANNEL_ID,
                    "VAMS Critical Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(sirenUri, sirenAttributes)
                    enableLights(true)
                    enableVibration(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(sirenChannel)

                // 2. Beep Channel
                val beepUri = android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.beep}")
                val beepAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                val beepChannel = NotificationChannel(
                    BEEP_CHANNEL_ID,
                    "VAMS High Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(beepUri, beepAttributes)
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(beepChannel)

                // 3. Default Channel
                val defaultChannel = NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    "VAMS Regular Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(defaultChannel)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channels", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String, alertId: String, severity: String, soundProfile: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("ALERT_ID", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleUpper = title.uppercase()
        val isTaskAction = titleUpper.contains("RESOLVED") || titleUpper.contains("REOPEN") || titleUpper.contains("HANDOVER") || titleUpper.contains("ASSIGN") || titleUpper.contains("TAKEOVER") || titleUpper.contains("TAKE OVER")

        val isSiren = (severity.uppercase() == "CRITICAL" || severity.uppercase() == "EMERGENCY" || soundProfile.uppercase() == "CRITICAL") && !isTaskAction
        val isBeep = severity.uppercase() == "HIGH" || severity.uppercase() == "MEDIUM" || soundProfile.uppercase() == "ALERT" || isTaskAction

        val channelId = when {
            isSiren -> SIREN_CHANNEL_ID
            isBeep -> BEEP_CHANNEL_ID
            else -> DEFAULT_CHANNEL_ID
        }

        val packageName = packageName
        val soundUri = when {
            isSiren -> android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.siren}")
            isBeep -> android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.beep}")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(notificationManager)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
