package com.example.vamsapp.network

import android.content.Context
import android.content.SharedPreferences
import com.example.vamsapp.model.User

object VamsPrefs {
    private const val PREFS_NAME = "vams_preferences"
    private const val FALLBACK_PREFS_NAME = "vams_preferences_standard"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_COMPANY_ID = "company_id"
    private const val KEY_COMPANY_NAME = "company_name"
    private const val KEY_MUTED_ALERTS = "muted_alerts"
    private const val DEFAULT_URL = "https://vams-backend.onrender.com/api/v1/"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        try {
            preferences = createEncryptedPrefs(context)
        } catch (e: Throwable) {
            android.util.Log.e("VamsPrefs", "Error creating EncryptedSharedPreferences, resetting keystore...", e)
            try {
                // Delete the master key from Keystore so getOrCreate recreates it
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply {
                    load(null)
                }
                keyStore.deleteEntry("_androidx_security_master_key_")
            } catch (keystoreEx: Throwable) {
                android.util.Log.e("VamsPrefs", "Failed to delete master key from Keystore", keystoreEx)
            }
            try {
                // Clear the SharedPreferences file physically to start clean
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
                preferences = createEncryptedPrefs(context)
            } catch (retryEx: Throwable) {
                android.util.Log.e("VamsPrefs", "Failed to recreate EncryptedSharedPreferences, falling back to standard SharedPreferences", retryEx)
                preferences = context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
        
        // Upgrade stale local IPs to the current active host IP, or reset loopback references
        val savedUrl = preferences.getString(KEY_SERVER_URL, null)
        if (savedUrl == null) {
            preferences.edit().putString(KEY_SERVER_URL, DEFAULT_URL).apply()
        } else if (savedUrl.contains("192.168.230.135") || savedUrl.contains("192.168.8.135") || savedUrl.contains("192.168.126.135")) {
            val upgradedUrl = savedUrl
                .replace("192.168.230.135", "192.168.124.135")
                .replace("192.168.8.135", "192.168.124.135")
                .replace("192.168.126.135", "192.168.124.135")
            preferences.edit().putString(KEY_SERVER_URL, upgradedUrl).apply()
        }

        // Also sync the static retrofit auth token upon startup
        ApiClient.authToken = getAuthToken()
        val currentUrl = getServerUrl()
        ApiClient.setBaseUrl(currentUrl)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
        return androidx.security.crypto.EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAuthToken(): String? {
        return preferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveAuthToken(token: String?) {
        preferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
        ApiClient.authToken = token
    }

    fun getServerUrl(): String {
        return preferences.getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun saveServerUrl(url: String) {
        var formattedUrl = url.trim()
        if (formattedUrl.endsWith("/")) {
            formattedUrl = formattedUrl.dropLast(1)
        }
        if (!formattedUrl.endsWith("/api/v1")) {
            if (formattedUrl.endsWith("/api")) {
                formattedUrl += "/v1"
            } else {
                formattedUrl += "/api/v1"
            }
        }
        formattedUrl += "/"
        preferences.edit().putString(KEY_SERVER_URL, formattedUrl).apply()
        ApiClient.setBaseUrl(formattedUrl)
    }

    fun getCompanyId(): String? {
        return preferences.getString(KEY_COMPANY_ID, null)
    }

    fun saveCompanyId(id: String?) {
        preferences.edit().putString(KEY_COMPANY_ID, id).apply()
    }

    fun getCompanyName(): String? {
        return preferences.getString(KEY_COMPANY_NAME, null)
    }

    fun saveCompanyName(name: String?) {
        preferences.edit().putString(KEY_COMPANY_NAME, name).apply()
    }

    private const val KEY_SEEN_ALERTS = "seen_alerts"
    private const val PREFIX_BEEP_TIME = "beep_time_"

    fun getMutedAlerts(): Set<String> {
        return preferences.getStringSet(KEY_MUTED_ALERTS, emptySet()) ?: emptySet()
    }

    fun muteAlert(alertId: String) {
        val current = getMutedAlerts().toMutableSet()
        current.add(alertId)
        preferences.edit().putStringSet(KEY_MUTED_ALERTS, current).apply()
    }

    fun unmuteAlert(alertId: String) {
        val current = getMutedAlerts().toMutableSet()
        current.remove(alertId)
        preferences.edit().putStringSet(KEY_MUTED_ALERTS, current).apply()
    }

    fun getSeenAlerts(): Set<String> {
        return preferences.getStringSet(KEY_SEEN_ALERTS, emptySet()) ?: emptySet()
    }

    fun isAlertSeen(alertId: String): Boolean {
        return getSeenAlerts().contains(alertId)
    }

    fun markAlertSeen(alertId: String) {
        val current = getSeenAlerts().toMutableSet()
        current.add(alertId)
        preferences.edit().putStringSet(KEY_SEEN_ALERTS, current).apply()
    }

    fun getAlertLastBeepTime(alertId: String): Long {
        return preferences.getLong(PREFIX_BEEP_TIME + alertId, 0L)
    }

    fun setAlertLastBeepTime(alertId: String, timestamp: Long) {
        preferences.edit().putLong(PREFIX_BEEP_TIME + alertId, timestamp).apply()
    }

    fun clearAlertSoundsState() {
        val allKeys = preferences.all.keys
        val editor = preferences.edit()
        editor.remove(KEY_SEEN_ALERTS)
        allKeys.forEach { key ->
            if (key.startsWith(PREFIX_BEEP_TIME)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun clearSession() {
        clearAlertSoundsState()
        preferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_COMPANY_ID)
            .remove(KEY_COMPANY_NAME)
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .remove("user_role")
            .apply()
        ApiClient.authToken = null
    }

    fun saveUser(user: User?) {
        if (user == null) {
            preferences.edit()
                .remove("user_id")
                .remove("user_email")
                .remove("user_name")
                .remove("user_role")
                .apply()
        } else {
            preferences.edit()
                .putString("user_id", user.id)
                .putString("user_email", user.email)
                .putString("user_name", user.name)
                .putString("user_role", user.role)
                .putString(KEY_COMPANY_ID, user.companyId)
                .apply()
        }
    }

    fun getUserId(): String? {
        return preferences.getString("user_id", null) ?: getUser()?.id
    }

    fun getUser(): User? {
        val id = preferences.getString("user_id", null) ?: return null
        val email = preferences.getString("user_email", null) ?: return null
        val name = preferences.getString("user_name", null) ?: return null
        val role = preferences.getString("user_role", null) ?: return null
        val companyId = preferences.getString(KEY_COMPANY_ID, null) ?: return null
        return User(id, email, name, role, companyId)
    }
}
