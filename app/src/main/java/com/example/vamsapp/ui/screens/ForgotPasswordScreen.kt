package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.ui.theme.Accent
import com.example.vamsapp.ui.theme.BackgroundDark
import com.example.vamsapp.ui.theme.CardDark
import com.example.vamsapp.ui.theme.PrimaryBlue
import com.example.vamsapp.ui.theme.Success
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.model.ForgotPasswordRequest
import com.example.vamsapp.model.ForgotPasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Password",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                AnimatedContent(
                    targetState = isSubmitted,
                    label = "forgot_password_state"
                ) { submitted ->
                    if (submitted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Success,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Check your inbox!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "A reset link has been dispatched to $email.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { 
                                    email = it 
                                    errorMsg = null
                                },
                                label = { Text("EMAIL ADDRESS") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )

                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMsg ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { 
                                    if (email.isNotEmpty()) {
                                        val companyId = VamsPrefs.getCompanyId() ?: ""
                                        if (companyId.isEmpty()) {
                                            errorMsg = "Company session is invalid. Please go back to login."
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMsg = null
                                        ApiClient.apiService.forgotPassword(ForgotPasswordRequest(email.trim(), companyId))
                                            .enqueue(object : Callback<ForgotPasswordResponse> {
                                                override fun onResponse(
                                                    call: Call<ForgotPasswordResponse>,
                                                    response: Response<ForgotPasswordResponse>
                                                ) {
                                                    isLoading = false
                                                    if (response.isSuccessful && response.body()?.success == true) {
                                                        isSubmitted = true
                                                    } else {
                                                        errorMsg = response.body()?.message ?: "Failed to dispatch reset link. Please check the email and try again."
                                                    }
                                                }

                                                override fun onFailure(
                                                    call: Call<ForgotPasswordResponse>,
                                                    t: Throwable
                                                ) {
                                                    isLoading = false
                                                    errorMsg = "Connection error: ${t.localizedMessage}"
                                                }
                                            })
                                    }
                                },
                                enabled = !isLoading && email.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Send reset link", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onNavigateBack) {
                    Text("Back to Login", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}
