package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vamsapp.model.*
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class AlertDetailViewModel : ViewModel() {
    private val _alert = MutableStateFlow<Alert?>(null)
    val alert: StateFlow<Alert?> = _alert

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionSuccess = MutableStateFlow(false)
    val actionSuccess: StateFlow<Boolean> = _actionSuccess

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    fun fetchDetails(alertId: String, initialAlert: Alert? = null) {
        if (initialAlert != null && initialAlert.id == alertId) {
            _alert.value = initialAlert
            _isLoading.value = false
        } else if (_alert.value?.id != alertId) {
            _isLoading.value = true
            _alert.value = null
        }
        _isMuted.value = alertId in VamsPrefs.getMutedAlerts()
        
        ApiClient.apiService.getAlertDetails(alertId).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val alertDetail = response.body()
                    _alert.value = alertDetail
                } else {
                    if (_alert.value == null) {
                        _error.value = "Failed to load alert details (Code: ${response.code()})"
                    }
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _isLoading.value = false
                if (_alert.value == null) {
                    _error.value = t.localizedMessage
                }
            }
        })
    }

    fun toggleMuteAlert(alertId: String) {
        val muted = alertId in VamsPrefs.getMutedAlerts()
        if (muted) {
            VamsPrefs.unmuteAlert(alertId)
            _isMuted.value = false
        } else {
            VamsPrefs.muteAlert(alertId)
            _isMuted.value = true
        }
    }

    fun takeOverAlert(alertId: String, user: User) {
        // Optimistically update status and assignment immediately
        _alert.value = _alert.value?.copy(status = "IN_PROGRESS", assignedToUserId = user.id, assignedToRole = user.role)
        _actionSuccess.value = true

        val request = AssignAlertRequest(
            assignedToUserId = user.id,
            assignedToRole = user.role,
            notes = "Alert taken over by ${user.name} (${user.role})"
        )

        ApiClient.apiService.reassignAlert(alertId, request).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                if (response.isSuccessful) {
                    val updated = response.body()
                    if (updated != null) {
                        _alert.value = updated
                    }
                } else {
                    _error.value = "Failed to take over alert: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    fun reassignAlert(alertId: String, userId: String, role: String, department: String, team: String, notes: String) {
        val request = AssignAlertRequest(
            assignedToUserId = userId,
            assignedToRole = if (role == "MANAGER") "FACTORY_MANAGER" else role,
            assignedToDepartment = department,
            assignedToTeam = team,
            notes = notes
        )

        ApiClient.apiService.reassignAlert(alertId, request).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                if (response.isSuccessful) {
                    _actionSuccess.value = true
                    _alert.value = response.body()
                } else {
                    _error.value = "Failed to reassign: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    fun resolveAlert(alertId: String, reason: String, notes: String, audioFile: File?, transcription: String?, imageUrls: List<String>?) {
        _isLoading.value = true

        if (audioFile != null) {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
            )
            val purposePart = "AUDIO_RESOLUTION".toRequestBody("text/plain".toMediaTypeOrNull())

            ApiClient.apiService.uploadMedia(filePart, purposePart).enqueue(object : Callback<UploadMediaResponse> {
                override fun onResponse(call: Call<UploadMediaResponse>, response: Response<UploadMediaResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val mediaUrl = response.body()!!.fileUrl
                        submitResolution(alertId, reason, notes, mediaUrl, transcription, imageUrls)
                    } else {
                        _isLoading.value = false
                        _error.value = "Failed to upload audio: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<UploadMediaResponse>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = "Upload connection failure: ${t.localizedMessage}"
                }
            })
        } else {
            submitResolution(alertId, reason, notes, null, transcription, imageUrls)
        }
    }

    private fun submitResolution(alertId: String, reason: String, notes: String, audioPath: String?, transcription: String?, imageUrls: List<String>?) {
        val request = ResolveAlertRequest(
            reason = reason,
            notes = notes,
            audioPath = audioPath,
            transcription = transcription ?: "",
            imageUrls = imageUrls ?: emptyList()
        )

        ApiClient.apiService.resolveAlert(alertId, request).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    _alert.value = response.body()
                    _actionSuccess.value = true
                } else {
                    _error.value = "Failed to resolve alert: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun addComment(alertId: String, commentText: String) {
        if (commentText.isBlank()) return
        ApiClient.apiService.addComment(alertId, AddCommentRequest(commentText)).enqueue(object : Callback<Comment> {
            override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                if (response.isSuccessful) {
                    fetchDetails(alertId, _alert.value)
                } else {
                    _error.value = "Failed to post comment"
                }
            }

            override fun onFailure(call: Call<Comment>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    fun reopenAlert(alertId: String, reason: String) {
        if (reason.isBlank()) return
        _alert.value = _alert.value?.copy(status = "REOPENED")
        _actionSuccess.value = true

        ApiClient.apiService.reopenAlert(alertId, ReopenAlertRequest(reason)).enqueue(object : Callback<Alert> {
            override fun onResponse(call: Call<Alert>, response: Response<Alert>) {
                if (response.isSuccessful && response.body() != null) {
                    _alert.value = response.body()
                } else {
                    _error.value = "Failed to reopen alert"
                }
            }

            override fun onFailure(call: Call<Alert>, t: Throwable) {
                _error.value = t.localizedMessage
            }
        })
    }

    fun resetActionState() {
        _actionSuccess.value = false
        _error.value = null
    }
}
