package com.example.vamsapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vamsapp.model.Company
import com.example.vamsapp.model.CreateCompanyRequest
import com.example.vamsapp.model.RegisterUserRequest
import com.example.vamsapp.model.User
import com.example.vamsapp.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationViewModel : ViewModel() {
    
    // --- Step 1: Company Registration (SUPER_ADMIN only) ---
    private val _newCompanyName = MutableStateFlow("")
    val newCompanyName: StateFlow<String> = _newCompanyName

    private val _createdCompany = MutableStateFlow<Company?>(null)
    val createdCompany: StateFlow<Company?> = _createdCompany

    // --- Step 2: User Registration (Under Company) ---
    private val _companyId = MutableStateFlow("")
    val companyId: StateFlow<String> = _companyId

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword

    private val _selectedRole = MutableStateFlow("WORKER")
    val selectedRole: StateFlow<String> = _selectedRole

    private val _department = MutableStateFlow("")
    val department: StateFlow<String> = _department

    private val _team = MutableStateFlow("")
    val team: StateFlow<String> = _team

    private val _currentStep = MutableStateFlow(1) // Connected stepper dots (1 to 2)
    val currentStep: StateFlow<Int> = _currentStep

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess

    fun setNewCompanyName(name: String) {
        _newCompanyName.value = name
        _error.value = null
    }

    fun setCompanyId(id: String) {
        _companyId.value = id
        _error.value = null
    }

    fun setFullName(name: String) {
        _fullName.value = name
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

    fun setConfirmPassword(pass: String) {
        _confirmPassword.value = pass
        _error.value = null
    }

    fun setSelectedRole(role: String) {
        _selectedRole.value = role
    }

    fun setDepartment(dept: String) {
        _department.value = dept
    }

    fun setTeam(tm: String) {
        _team.value = tm
    }

    fun nextStep() {
        if (_currentStep.value == 1) {
            if (_companyId.value.isBlank() || _fullName.value.isBlank() || _email.value.isBlank()) {
                _error.value = "Please complete all fields in Step 1"
                return
            }
            val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
            if (!emailPattern.matches(_email.value.trim())) {
                _error.value = "Invalid email address format"
                return
            }
            _currentStep.value = 2
            _error.value = null
        }
    }

    fun prevStep() {
        if (_currentStep.value == 2) {
            _currentStep.value = 1
            _error.value = null
        }
    }

    // Dynamic password strength indicator
    fun getPasswordStrength(): Int {
        val pass = _password.value
        if (pass.isEmpty()) return 0
        var score = 0
        if (pass.length >= 6) score++
        if (pass.any { it.isUpperCase() }) score++
        if (pass.any { it.isDigit() }) score++
        if (pass.any { !it.isLetterOrDigit() }) score++
        return score // returns 0 to 4 (red -> orange -> yellow -> green)
    }

    fun registerCompany() {
        val name = _newCompanyName.value.trim()
        if (name.isBlank()) {
            _error.value = "Company name is required"
            return
        }

        _isLoading.value = true
        ApiClient.apiService.createCompany(CreateCompanyRequest(name)).enqueue(object : Callback<Company> {
            override fun onResponse(call: Call<Company>, response: Response<Company>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _createdCompany.value = response.body()
                } else {
                    val errorMsg = try {
                        val json = response.errorBody()?.string()
                        val parser = com.google.gson.JsonParser.parseString(json)
                        parser.asJsonObject.get("message").asString
                    } catch (e: Exception) {
                        null
                    }
                    _error.value = errorMsg ?: if (response.code() == 409) {
                        "Company by this name is already registered"
                    } else {
                        "Failed to create company (Code: ${response.code()})"
                    }
                }
            }

            override fun onFailure(call: Call<Company>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun registerUser() {
        if (_password.value != _confirmPassword.value) {
            _error.value = "Passwords do not match"
            return
        }
        if (getPasswordStrength() < 2) {
            _error.value = "Password is too weak"
            return
        }

        _isLoading.value = true
        val request = RegisterUserRequest(
            name = _fullName.value.trim(),
            email = _email.value.trim(),
            password = _password.value,
            role = _selectedRole.value,
            companyId = _companyId.value.trim()
        )

        ApiClient.apiService.register(request).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _registrationSuccess.value = true
                } else {
                    val errorMsg = try {
                        val json = response.errorBody()?.string()
                        val parser = com.google.gson.JsonParser.parseString(json)
                        parser.asJsonObject.get("message").asString
                    } catch (e: Exception) {
                        null
                    }
                    _error.value = errorMsg ?: if (response.code() == 409) {
                        "User with this email is already registered"
                    } else {
                        "Registration failed (Code: ${response.code()})"
                    }
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.localizedMessage
            }
        })
    }

    fun clearError() {
        _error.value = null
    }

    fun reset(prefilledCompanyId: String? = null) {
        _companyId.value = prefilledCompanyId ?: ""
        _fullName.value = ""
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
        _selectedRole.value = "WORKER"
        _department.value = ""
        _team.value = ""
        _currentStep.value = 1
        _isLoading.value = false
        _error.value = null
        _registrationSuccess.value = false
        _newCompanyName.value = ""
        _createdCompany.value = null
    }
}
