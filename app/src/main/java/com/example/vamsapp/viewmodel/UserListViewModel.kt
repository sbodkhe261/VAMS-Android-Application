package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vamsapp.model.User
import com.example.vamsapp.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserListViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedRoleFilter = MutableStateFlow("ALL")
    val selectedRoleFilter: StateFlow<String> = _selectedRoleFilter

    fun fetchUsers(companyId: String) {
        _isLoading.value = true
        ApiClient.apiService.getCompanyUsers(companyId).enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val userList = response.body() ?: emptyList()
                    _users.value = userList
                    applyFilters()
                } else {
                    _error.value = "Failed to load users: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun search(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun filterByRole(role: String) {
        _selectedRoleFilter.value = role
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val roleFilter = _selectedRoleFilter.value

        _filteredUsers.value = _users.value.filter { user ->
            val matchesQuery = user.name.lowercase().contains(query) || user.email.lowercase().contains(query)
            val matchesRole = if (roleFilter == "ALL") {
                true
            } else if (roleFilter.equals("MANAGER", ignoreCase = true)) {
                user.role.equals("MANAGER", ignoreCase = true) || user.role.equals("FACTORY_MANAGER", ignoreCase = true)
            } else {
                user.role.equals(roleFilter, ignoreCase = true)
            }
            matchesQuery && matchesRole
        }
    }
}
