package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vamsapp.model.Company
import com.example.vamsapp.model.LoginRequest
import com.example.vamsapp.model.LoginResponse
import com.example.vamsapp.model.User
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed class LoginStep {
    object CompanyInput : LoginStep()
    data class CompanyConfirmed(val company: Company) : LoginStep()
    object UserInput : LoginStep()
}

class LoginViewModel : ViewModel() {
    private val _step = MutableStateFlow<LoginStep>(LoginStep.CompanyInput)
    val step: StateFlow<LoginStep> = _step

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    private val _companyIdInput = MutableStateFlow("")
    val companyIdInput: StateFlow<String> = _companyIdInput

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginSuccessUser = MutableStateFlow<User?>(null)
    val loginSuccessUser: StateFlow<User?> = _loginSuccessUser

    init {
        // Pre-fill from Preferences
        _serverUrl.value = VamsPrefs.getServerUrl()
        _companyIdInput.value = VamsPrefs.getCompanyName() ?: VamsPrefs.getCompanyId() ?: ""
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url
        _error.value = null
    }

    fun setCompanyIdInput(id: String) {
        _companyIdInput.value = id
        _error.value = null
    }

    fun setEmail(mail: String) {
        _email.value = mail
        _error.value = null
    }

    fun setPassword(pass: String) {
        _password.value = pass
        _error.value = null
    }

    fun validateCompany() {
        val url = _serverUrl.value.trim()
        val compId = _companyIdInput.value.trim()

        if (url.isBlank() || compId.isBlank()) {
            _error.value = "Server URL and Company ID cannot be blank"
            return
        }

        // Save server URL immediately to ApiClient configuration
        VamsPrefs.saveServerUrl(url)
        _serverUrl.value = VamsPrefs.getServerUrl()
        _isLoading.value = true

        ApiClient.apiService.getCompany(compId).enqueue(object : Callback<Company> {
            override fun onResponse(call: Call<Company>, response: Response<Company>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    val company = response.body()!!
                    VamsPrefs.saveCompanyId(company.id)
                    VamsPrefs.saveCompanyName(company.name)
                    _step.value = LoginStep.CompanyConfirmed(company)
                } else {
                    _error.value = "Company not found"
                }
            }

            override fun onFailure(call: Call<Company>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Connection error: ${t.localizedMessage}"
            }
        })
    }

    fun proceedToUserLogin() {
        _step.value = LoginStep.UserInput
    }

    fun goBackToCompanyInput() {
        _step.value = LoginStep.CompanyInput
    }

    fun login() {
        val mail = _email.value.trim()
        val pass = _password.value

        if (mail.isBlank() || pass.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }

        val enteredCompanyId = VamsPrefs.getCompanyId()
        _isLoading.value = true
        ApiClient.apiService.login(LoginRequest(mail, pass, enteredCompanyId)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Verify company isolation rule: JWT companyId matches entered companyId (UUID) or company name/code
                    val enteredCompanyId = VamsPrefs.getCompanyId()
                    val enteredCompanyName = VamsPrefs.getCompanyName()
                    
                    val belongsToCompany = (body.user.companyId == enteredCompanyId) ||
                            (!enteredCompanyId.isNullOrBlank() && body.user.companyCode?.equals(enteredCompanyId, ignoreCase = true) == true) ||
                            (!enteredCompanyName.isNullOrBlank() && body.user.companyCode?.equals(enteredCompanyName, ignoreCase = true) == true) ||
                            (!enteredCompanyName.isNullOrBlank() && body.user.companyName?.equals(enteredCompanyName, ignoreCase = true) == true)

                    if (!belongsToCompany) {
                        _error.value = "This account does not belong to this company"
                        VamsPrefs.clearSession()
                        return
                    }

                    // Save session details
                    VamsPrefs.saveAuthToken(body.accessToken)

                    // Fetch and upload FCM device token on login
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result != null) {
                            val fcmToken = task.result
                            ApiClient.apiService.updateDeviceToken(com.example.vamsapp.model.UpdateDeviceTokenRequest(fcmToken))
                                .enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        android.util.Log.d("FCM", "Device token uploaded on login: ${response.code()}")
                                    }
                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        android.util.Log.e("FCM", "Failed to upload token on login: ${t.localizedMessage}")
                                    }
                                })
                        }
                    }

                    _loginSuccessUser.value = body.user
                } else {
                    _error.value = "Invalid credentials"
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Auth failed: ${t.localizedMessage}"
            }
        })
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _loginSuccessUser.value = null
        _email.value = ""
        _password.value = ""
        _error.value = null
        _isLoading.value = false
        _step.value = LoginStep.CompanyInput
    }
}
