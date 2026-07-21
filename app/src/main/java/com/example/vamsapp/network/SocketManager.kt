package com.example.vamsapp.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private const val TAG = "SocketManager"
    private var socket: Socket? = null

    interface SocketEventListener {
        fun onAlertCreated(alertId: String, defectName: String, severity: String, vin: String)
        fun onCommentAdded(alertId: String, commentText: String, userName: String)
        fun onAlertResolved(alertId: String, resolvedBy: String, reason: String?)
        fun onAlertReopened(alertId: String, reopenedBy: String)
        fun onAlertAssigned(alertId: String, title: String, message: String)
        fun onBroadcastCreated(broadcastId: String, title: String, message: String)
    }

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<SocketEventListener>()
    private var legacyListener: SocketEventListener? = null

    fun setEventListener(eventListener: SocketEventListener?) {
        legacyListener?.let { unregisterListener(it) }
        legacyListener = eventListener
        eventListener?.let { registerListener(it) }
    }

    fun registerListener(eventListener: SocketEventListener) {
        if (!listeners.contains(eventListener)) {
            listeners.add(eventListener)
        }
    }

    fun unregisterListener(eventListener: SocketEventListener) {
        listeners.remove(eventListener)
    }

    fun connect() {
        if (socket?.connected() == true) return

        val serverUrl = VamsPrefs.getServerUrl()
        // Extract base socket URL (e.g. https://vams-backend.onrender.com)
        val socketUrl = try {
            val idx = serverUrl.indexOf("/api/")
            if (idx != -1) serverUrl.substring(0, idx) else serverUrl
        } catch (e: Exception) {
            serverUrl
        }

        val token = VamsPrefs.getAuthToken()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "Cannot connect to socket: Auth token is null or empty")
            return
        }

        try {
            val options = IO.Options().apply {
                query = "token=$token"
                transports = arrayOf("websocket") // Force WebSocket for clean connection
            }
            socket = IO.socket(socketUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to WebSocket Server at $socketUrl")
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from WebSocket Server")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "WebSocket Connection Error: ${args.firstOrNull()}")
            }

            // Listen to VAMS specific events
            socket?.on("ALERT_CREATED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "ALERT_CREATED: $data")
                val alertId = if (data.has("alertId")) data.optString("alertId") else data.optString("id")
                val defectName = data.optString("defectName")
                val severity = data.optString("severity")
                val vin = data.optString("vin")
                listeners.forEach { it.onAlertCreated(alertId, defectName, severity, vin) }
            }

            socket?.on("COMMENT_ADDED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "COMMENT_ADDED: $data")
                val alertId = data.optString("alertId")
                val commentText = data.optString("commentText")
                val userName = data.optString("userName")
                listeners.forEach { it.onCommentAdded(alertId, commentText, userName) }
            }

            socket?.on("ALERT_RESOLVED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "ALERT_RESOLVED: $data")
                val alertId = data.optString("alertId")
                val resolvedBy = data.optString("resolvedBy")
                val reason = if (data.has("reason")) data.optString("reason") else null
                listeners.forEach { it.onAlertResolved(alertId, resolvedBy, reason) }
            }

            socket?.on("ALERT_REOPENED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "ALERT_REOPENED: $data")
                val alertId = data.optString("alertId")
                val reopenedBy = data.optString("reopenedBy")
                listeners.forEach { it.onAlertReopened(alertId, reopenedBy) }
            }

            socket?.on("ALERT_ASSIGNED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "ALERT_ASSIGNED: $data")
                val alertId = data.optString("alertId")
                val title = data.optString("title")
                val message = data.optString("message")
                listeners.forEach { it.onAlertAssigned(alertId, title, message) }
            }

            socket?.on("BROADCAST_CREATED") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "BROADCAST_CREATED: $data")
                val title = data.optString("title")
                val message = data.optString("message")
                val broadcastId = data.optString("broadcastId").ifEmpty { "BROADCAST_${System.currentTimeMillis()}" }
                
                val targetUserIdsArray = data.optJSONArray("targetUserIds")
                if (targetUserIdsArray != null && targetUserIdsArray.length() > 0) {
                    val targetUserIds = mutableListOf<String>()
                    for (i in 0 until targetUserIdsArray.length()) {
                        targetUserIds.add(targetUserIdsArray.optString(i).trim())
                    }
                    val currentUser = VamsPrefs.getUser()
                    val currentUserId = VamsPrefs.getUserId()?.trim() ?: currentUser?.id?.trim()
                    val currentUserEmail = currentUser?.email?.trim()
                    val currentUserRole = currentUser?.role?.trim()
                    
                    val hasUserInfo = !currentUserId.isNullOrEmpty() || !currentUserEmail.isNullOrEmpty() || !currentUserRole.isNullOrEmpty()
                    if (hasUserInfo) {
                        val isTargeted = targetUserIds.any { target ->
                            (!currentUserId.isNullOrEmpty() && target.equals(currentUserId, ignoreCase = true)) ||
                            (!currentUserEmail.isNullOrEmpty() && target.equals(currentUserEmail, ignoreCase = true)) ||
                            (!currentUserRole.isNullOrEmpty() && target.equals(currentUserRole, ignoreCase = true))
                        }
                        
                        if (!isTargeted) {
                            Log.d(TAG, "Skipping broadcast notification: current user ($currentUserId / $currentUserEmail / $currentUserRole) is not targeted.")
                            return@on
                        }
                    }
                }
                
                listeners.forEach { it.onBroadcastCreated(broadcastId, title, message) }
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Failed to initialize socket connection", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}
