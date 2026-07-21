package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vamsapp.model.Alert
import com.example.vamsapp.model.DashboardTelemetry
import com.example.vamsapp.model.User
import com.example.vamsapp.model.AssignAlertRequest
import com.example.vamsapp.model.ResolveAlertRequest
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.network.SocketManager
import com.example.vamsapp.service.SoundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardViewModel : ViewModel(), SocketManager.SocketEventListener {
    private val _telemetry = MutableStateFlow<DashboardTelemetry?>(null)
    val telemetry: StateFlow<DashboardTelemetry?> = _telemetry

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts

    private val _roleFilteredAlerts = MutableStateFlow<List<Alert>>(emptyList())
    val roleFilteredAlerts: StateFlow<List<Alert>> = _roleFilteredAlerts

    private val _rawAlerts = MutableStateFlow<List<Alert>>(emptyList())
    val rawAlerts: StateFlow<List<Alert>> = _rawAlerts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Selected filters
    private val _selectedStatus = MutableStateFlow("ALL") // ALL, OPEN, IN_PROGRESS, RESOLVED, REOPENED
    val selectedStatus: StateFlow<String> = _selectedStatus

    private val _selectedSeverity = MutableStateFlow("ALL") // ALL, CRITICAL, HIGH, MEDIUM, LOW
    val selectedSeverity: StateFlow<String> = _selectedSeverity

    private var pollJob: Job? = null

    fun startPolling(user: User) {
        SocketManager.registerListener(this)
        fetchTelemetryAndAlerts(user, showLoading = false)
    }

    fun stopPolling() {
        SocketManager.unregisterListener(this)
        pollJob?.cancel()
    }

    fun selectStatus(status: String, user: User) {
        _selectedStatus.value = status
        filterAndPostAlerts(user)
    }

    fun selectSeverity(severity: String, user: User) {
        _selectedSeverity.value = severity
        filterAndPostAlerts(user)
    }

    private fun filterAndPostAlerts(user: User) {
        val rawList = _rawAlerts.value
        val statusFilter = _selectedStatus.value
        val severityFilter = _selectedSeverity.value

        // First apply role filter
        val roleFiltered = rawList
        _roleFilteredAlerts.value = roleFiltered

        // Then apply status and severity filters
        val statusFiltered = if (statusFilter == "ALL") {
            roleFiltered
        } else {
            roleFiltered.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        val finalFiltered = if (severityFilter == "ALL") {
            statusFiltered
        } else {
            statusFiltered.filter { it.severity.equals(severityFilter, ignoreCase = true) }
        }

        _alerts.value = finalFiltered
    }

    fun fetchTelemetryAndAlerts(user: User, showLoading: Boolean = true) {
        if (showLoading) {
            _isLoading.value = true
        }
        
        // Fetch dashboard telemetry
        ApiClient.apiService.getDashboard().enqueue(object : Callback<DashboardTelemetry> {
            override fun onResponse(call: Call<DashboardTelemetry>, response: Response<DashboardTelemetry>) {
                if (response.isSuccessful) {
                    _telemetry.value = response.body()
                }
            }
            override fun onFailure(call: Call<DashboardTelemetry>, t: Throwable) {
                // Silently ignore telemetry failure on background poll
            }
        })

        // Fetch alerts
        ApiClient.apiService.getAlerts(
            status = null,
            severity = null
        ).enqueue(object : Callback<List<Alert>> {
            override fun onResponse(call: Call<List<Alert>>, response: Response<List<Alert>>) {
                if (showLoading) {
                    _isLoading.value = false
                }
                if (response.isSuccessful) {
                    val rawList = response.body() ?: emptyList()
                    _rawAlerts.value = rawList
                    filterAndPostAlerts(user)
                } else {
                    if (showLoading) {
                        _error.value = "Failed to load alerts: ${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<List<Alert>>, t: Throwable) {
                if (showLoading) {
                    _isLoading.value = false
                    _error.value = t.localizedMessage
                }
            }
        })
    }

    fun reset() {
        _telemetry.value = null
        _alerts.value = emptyList()
        _roleFilteredAlerts.value = emptyList()
        _rawAlerts.value = emptyList()
        _isLoading.value = false
        _error.value = null
        _selectedStatus.value = "ALL"
        _selectedSeverity.value = "ALL"
    }

    fun clearError() {
        _error.value = null
    }

    fun takeOverAlert(alertId: String, user: User) {
        val currentList = _rawAlerts.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == alertId }
        if (idx != -1) {
            val old = currentList[idx]
            currentList[idx] = old.copy(status = "IN_PROGRESS", assignedToUserId = user.id, assignedToRole = user.role)
            _rawAlerts.value = currentList
            filterAndPostAlerts(user)
        }

        val request = AssignAlertRequest(
            assignedToUserId = user.id,
            assignedToRole = user.role,
            notes = "Alert taken over by ${user.name} (${user.role})"
        )

        ApiClient.apiService.reassignAlert(alertId, request).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                if (response.isSuccessful) {
                    fetchTelemetryAndAlerts(user, showLoading = false)
                } else {
                    _error.value = "Failed to take over: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    fun resolveAlert(alertId: String, reason: String, notes: String, user: User) {
        val currentList = _rawAlerts.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == alertId }
        if (idx != -1) {
            val old = currentList[idx]
            currentList[idx] = old.copy(status = "RESOLVED")
            _rawAlerts.value = currentList
            filterAndPostAlerts(user)
        }

        val request = ResolveAlertRequest(
            reason = reason,
            notes = notes,
            transcription = "",
            audioPath = null,
            imageUrls = emptyList()
        )

        ApiClient.apiService.resolveAlert(alertId, request).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                if (response.isSuccessful) {
                    fetchTelemetryAndAlerts(user, showLoading = false)
                } else {
                    _error.value = "Failed to resolve: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    // Socket.IO event handler implementations for immediate foreground UI sync
    override fun onAlertCreated(alertId: String, defectName: String, severity: String, vin: String) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onCommentAdded(alertId: String, commentText: String, userName: String) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onAlertResolved(alertId: String, resolvedBy: String, reason: String?) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onAlertReopened(alertId: String, reopenedBy: String) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onAlertAssigned(alertId: String, title: String, message: String) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onBroadcastCreated(broadcastId: String, title: String, message: String) {
        val user = VamsPrefs.getUser()
        if (user != null) {
            fetchTelemetryAndAlerts(user, showLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
