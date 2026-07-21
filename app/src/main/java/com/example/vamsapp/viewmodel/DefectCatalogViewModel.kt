package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vamsapp.model.CreateDefectRequest
import com.example.vamsapp.model.DefectMaster
import com.example.vamsapp.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DefectCatalogViewModel : ViewModel() {
    private val _defects = MutableStateFlow<List<DefectMaster>>(emptyList())
    val defects: StateFlow<List<DefectMaster>> = _defects

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionSuccess = MutableStateFlow(false)
    val actionSuccess: StateFlow<Boolean> = _actionSuccess

    fun fetchDefects() {
        _isLoading.value = true
        ApiClient.apiService.getDefects().enqueue(object : Callback<List<DefectMaster>> {
            override fun onResponse(call: Call<List<DefectMaster>>, response: Response<List<DefectMaster>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _defects.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load defects: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<DefectMaster>>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun addDefect(name: String, category: String, severity: String, defaultAssigneeRole: String, ownerVisible: Boolean, soundProfile: String) {
        if (name.isBlank() || category.isBlank()) {
            _error.value = "Please complete all fields"
            return
        }
        _isLoading.value = true
        val request = CreateDefectRequest(
            name = name,
            category = category,
            severity = severity,
            defaultAssigneeRole = if (defaultAssigneeRole == "MANAGER") "FACTORY_MANAGER" else defaultAssigneeRole,
            ownerVisible = ownerVisible,
            soundProfile = soundProfile
        )

        ApiClient.apiService.createDefect(request).enqueue(object : Callback<DefectMaster> {
            override fun onResponse(call: Call<DefectMaster>, response: Response<DefectMaster>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _actionSuccess.value = true
                    fetchDefects()
                } else {
                    _error.value = "Failed to create defect: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<DefectMaster>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun deactivateDefect(id: String) {
        _isLoading.value = true
        ApiClient.apiService.deleteDefect(id).enqueue(object : Callback<DefectMaster> {
            override fun onResponse(call: Call<DefectMaster>, response: Response<DefectMaster>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    fetchDefects()
                } else {
                    _error.value = "Failed to deactivate defect"
                }
            }

            override fun onFailure(call: Call<DefectMaster>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun resetActionState() {
        _actionSuccess.value = false
        _error.value = null
    }
}
