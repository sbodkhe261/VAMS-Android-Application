package com.example.vamsapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.vamsapp.MainActivity
import com.example.vamsapp.model.Alert
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertEscalationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "AlertEscalationWorker"
    }

    override fun doWork(): Result {
        val token = VamsPrefs.getAuthToken()
        if (token.isNullOrEmpty()) {
            return Result.success()
        }

        try {
            // Fetch open alerts
            val response = ApiClient.apiService.getAlerts(status = "OPEN").execute()
            if (response.isSuccessful) {
                val alerts = response.body() ?: emptyList()
                if (alerts.isNotEmpty()) {
                    processAlerts(alerts)
                }
            } else {
                Log.e(TAG, "Failed to check alerts: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking alerts in background", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun processAlerts(alerts: List<Alert>) {
        val mutedIds = VamsPrefs.getMutedAlerts()
        val now = System.currentTimeMillis()
        val twoHoursMs = 2L * 60L * 60L * 1000L

        // 1. Process 2-hour unseen beep reminder
        alerts.forEach { alert ->
            if (alert.id in mutedIds) return@forEach
            
            // Check if user has not seen the alert
            if (!VamsPrefs.isAlertSeen(alert.id)) {
                val lastBeep = VamsPrefs.getAlertLastBeepTime(alert.id)
                if (lastBeep == 0L) {
                    // Initialize beep time if not set yet (so they aren't bombarded immediately on login/sync)
                    VamsPrefs.setAlertLastBeepTime(alert.id, now)
                } else if (now - lastBeep >= twoHoursMs) {
                    // Play severity sound
                    SoundService.init(applicationContext)
                    when (alert.severity.uppercase()) {
                        "CRITICAL", "EMERGENCY" -> SoundService.playCriticalSound(applicationContext)
                        "HIGH" -> SoundService.playHighSound(applicationContext)
                        "MEDIUM" -> SoundService.playMediumSound(applicationContext)
                        else -> SoundService.playLowSound(applicationContext)
                    }
                    // Update beep time to prevent re-beeping
                    VamsPrefs.setAlertLastBeepTime(alert.id, now)
                    // Send notification reminder
                    sendNotification(
                        "Reminder: Unseen ${alert.severity} Alert",
                        "Alert for VIN ${alert.vin} (${alert.defect?.name ?: "Defect"}) remains unseen."
                    )
                }
            }
        }

        // 2. Perform time-based escalation warning notifications
        alerts.forEach { alert ->
            if (alert.id in mutedIds) return@forEach
            
            val daysElapsed = calculateDaysElapsed(alert.createdAt)
            when {
                daysElapsed >= 10 -> {
                    // Muted yellow warning
                }
                daysElapsed in 7..9 -> {
                    sendNotification("Urgent Alert Escalation Warning", "Alert #${alert.id.take(8)} unresolved for ${daysElapsed} days.")
                }
                daysElapsed in 3..6 -> {
                    sendNotification("CRITICAL ALERT - Near Escalation Limit", "Alert #${alert.id.take(8)} is approaching limits (${daysElapsed} days pending).")
                }
                daysElapsed == 2 || daysElapsed == 1 -> {
                    // Continuous mode warning
                    sendNotification("IMMEDIATE ACTION REQUIRED", "Alert #${alert.id.take(8)} will breach escalation protocol within 24 hours!")
                }
            }
        }
    }

    private fun calculateDaysElapsed(createdAtStr: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val createdDate = format.parse(createdAtStr) ?: return 0
            val diffMs = Date().time - createdDate.time
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vams_background_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VAMS System Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
