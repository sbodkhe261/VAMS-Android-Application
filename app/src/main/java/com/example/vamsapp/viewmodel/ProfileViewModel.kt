package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled

    private val _muteDuration = MutableStateFlow("None") // None, 1h, 4h, Tomorrow
    val muteDuration: StateFlow<String> = _muteDuration

    fun toggleSoundNotifications(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        com.example.vamsapp.service.SoundService.setAppMuted(!enabled)
    }

    fun setMuteDuration(duration: String) {
        _muteDuration.value = duration
        if (duration == "None") {
            com.example.vamsapp.service.SoundService.setAppMuted(!_isSoundEnabled.value)
        } else {
            // Under muted state, stop all active alarm sound generators
            com.example.vamsapp.service.SoundService.setAppMuted(true)
        }
    }

    fun logout(onComplete: () -> Unit) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result else null
            ApiClient.apiService.logout(com.example.vamsapp.model.LogoutRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                    VamsPrefs.clearSession()
                    onComplete()
                }
                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    VamsPrefs.clearSession()
                    onComplete()
                }
            })
        }
    }
}
