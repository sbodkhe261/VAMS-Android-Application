package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SplashNavigationState {
    object Idle : SplashNavigationState()
    object NavigateToCompanyLogin : SplashNavigationState()
    object NavigateToDashboard : SplashNavigationState()
}

class SplashViewModel : ViewModel() {
    private val _navState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Idle)
    val navState: StateFlow<SplashNavigationState> = _navState

    fun checkSession() {
        viewModelScope.launch {
            // Splash screens must feel premium, wait for a pulse animation (approx 1200ms)
            delay(1200)
            val token = VamsPrefs.getAuthToken()
            val companyId = VamsPrefs.getCompanyId()
            val user = VamsPrefs.getUser()

            if (!token.isNullOrEmpty() && !companyId.isNullOrEmpty() && user != null) {
                // Pre-configure ApiClient with token
                ApiClient.authToken = token
                _navState.value = SplashNavigationState.NavigateToDashboard
            } else {
                _navState.value = SplashNavigationState.NavigateToCompanyLogin
            }
        }
    }
}
