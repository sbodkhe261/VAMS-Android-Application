package com.example.vamsapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vamsapp.MainActivity
import com.example.vamsapp.network.SocketManager
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.network.ApiClient
import android.media.AudioAttributes
import android.media.RingtoneManager

class VamsNotificationService : Service() {

    companion object {
        private const val TAG = "VamsNotificationService"
        private const val CHANNEL_ID = "vams_service_channel"
        private const val NOTIFICATION_ID = 9999
        private const val SIREN_CHANNEL_ID = "vams_siren_alerts_v7"
        private const val BEEP_CHANNEL_ID = "vams_beep_alerts_v7"
        private const val DEFAULT_CHANNEL_ID = "vams_default_alerts_v7"
    }

    private fun handleSocketAlert(alertId: String, title: String, message: String, defaultSeverity: String) {
        ApiClient.apiService.getAlertDetails(alertId).enqueue(object : retrofit2.Callback<com.example.vamsapp.model.Alert> {
            override fun onResponse(
                call: retrofit2.Call<com.example.vamsapp.model.Alert>,
                response: retrofit2.Response<com.example.vamsapp.model.Alert>
            ) {
                val severity = if (response.isSuccessful) {
                    response.body()?.severity ?: defaultSeverity
                } else {
                    defaultSeverity
                }
                sendSystemHeadsUpNotification(title, message, alertId, severity)
            }

            override fun onFailure(call: retrofit2.Call<com.example.vamsapp.model.Alert>, t: Throwable) {
                sendSystemHeadsUpNotification(title, message, alertId, defaultSeverity)
            }
        })
    }

    private val socketListener = object : SocketManager.SocketEventListener {
        override fun onAlertCreated(alertId: String, defectName: String, severity: String, vin: String) {
            Log.d(TAG, "onAlertCreated: $defectName")
            sendSystemHeadsUpNotification(
                "New $severity Defect Alert",
                "$defectName detected on VIN $vin",
                alertId,
                severity
            )
        }

        override fun onCommentAdded(alertId: String, commentText: String, userName: String) {
            Log.d(TAG, "onCommentAdded: $commentText")
            handleSocketAlert(
                alertId,
                "New Comment by $userName",
                commentText,
                "INFO"
            )
        }

        override fun onAlertResolved(alertId: String, resolvedBy: String, reason: String?) {
            Log.d(TAG, "onAlertResolved: $alertId")
            val messageText = if (!reason.isNullOrEmpty()) {
                "Resolved by $resolvedBy. Comment: \"$reason\""
            } else {
                "Resolved by $resolvedBy"
            }
            handleSocketAlert(
                alertId,
                "Defect Alert Resolved",
                messageText,
                "INFO"
            )
        }

        override fun onAlertReopened(alertId: String, reopenedBy: String) {
            Log.d(TAG, "onAlertReopened: $alertId")
            handleSocketAlert(
                alertId,
                "Defect Alert Reopened",
                "Reopened by $reopenedBy",
                "INFO"
            )
        }

        override fun onAlertAssigned(alertId: String, title: String, message: String) {
            Log.d(TAG, "onAlertAssigned: $alertId")
            handleSocketAlert(
                alertId,
                title,
                message,
                "INFO"
            )
        }

        override fun onBroadcastCreated(broadcastId: String, title: String, message: String) {
            Log.d(TAG, "onBroadcastCreated: $title")
            sendSystemHeadsUpNotification(
                title,
                message,
                broadcastId,
                "HIGH"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VamsNotificationService created")
        VamsPrefs.init(applicationContext)
        createNotificationChannel()
        startForegroundServiceNotification()
        
        // Register listener and connect
        SocketManager.registerListener(socketListener)
        SocketManager.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VamsNotificationService onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VamsNotificationService destroyed")
        SocketManager.unregisterListener(socketListener)
        SocketManager.disconnect()
        SoundService.stopAllSounds()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VAMS Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VAMS Background Service")
            .setContentText("Listening for vehicle alerts in background...")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val packageName = packageName
                
                // 1. Siren Channel
                val sirenUri = android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.beep}")
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

    private fun sendSystemHeadsUpNotification(
        title: String,
        message: String,
        alertId: String,
        severity: String
    ) {
        if (com.example.vamsapp.MainActivity.isAppInForeground) {
            return
        }

        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(notificationManager)

        val titleUpper = title.uppercase()
        val isTaskAction = titleUpper.contains("RESOLVED") || titleUpper.contains("REOPEN") || titleUpper.contains("HANDOVER") || titleUpper.contains("ASSIGN")
        
        // Siren should only sound for CRITICAL/EMERGENCY alerts that are NOT simple task updates
        val isSiren = (severity.uppercase() == "CRITICAL" || severity.uppercase() == "EMERGENCY") && !isTaskAction
        val isBeep = severity.uppercase() == "HIGH" || severity.uppercase() == "MEDIUM" || isTaskAction

        val channelId = when {
            isSiren -> SIREN_CHANNEL_ID
            isBeep -> BEEP_CHANNEL_ID
            else -> DEFAULT_CHANNEL_ID
        }

        val packageName = packageName
        val soundUri = when {
            isSiren -> android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.beep}")
            isBeep -> android.net.Uri.parse("android.resource://$packageName/${com.example.vamsapp.R.raw.beep}")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ALERT_ID", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .build()

        notificationManager.notify(alertId.hashCode(), notification)
    }
}
