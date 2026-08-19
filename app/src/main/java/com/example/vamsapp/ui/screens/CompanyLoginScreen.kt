package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.ui.theme.Accent
import com.example.vamsapp.ui.theme.BackgroundDark
import com.example.vamsapp.ui.theme.CardDark
import com.example.vamsapp.ui.theme.PrimaryBlue
import com.example.vamsapp.viewmodel.LoginStep
import com.example.vamsapp.viewmodel.LoginViewModel

// Shake modifier helper
fun Modifier.shake(trigger: Boolean): Modifier = composed {
    val translationX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            repeat(3) {
                translationX.animateTo(
                    targetValue = 15f,
                    animationSpec = tween(50, easing = LinearEasing)
                )
                translationX.animateTo(
                    targetValue = -15f,
                    animationSpec = tween(50, easing = LinearEasing)
                )
            }
            translationX.animateTo(0f, animationSpec = tween(50))
        }
    }
    this.graphicsLayer(translationX = translationX.value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyLoginScreen(
    viewModel: LoginViewModel,
    onNavigateToUserLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val companyIdInput by viewModel.companyIdInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val step by viewModel.step.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    var shakeTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error != null) {
            shakeTrigger = true
            kotlinx.coroutines.delay(300)
            shakeTrigger = false
        }
    }

    LaunchedEffect(step) {
        if (step is LoginStep.UserInput) {
            onNavigateToUserLogin()
        }
    }

    // Glow state handlers
    var isServerFocused by remember { mutableStateOf(false) }
    var isCompanyFocused by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedUrlOption by remember { mutableStateOf("onrender") }
    var customIpInput by remember { mutableStateOf("192.168.124.135") }

    LaunchedEffect(serverUrl, showSettingsDialog) {
        if (!showSettingsDialog) {
            selectedUrlOption = when {
                serverUrl.contains("onrender.com") -> "onrender"
                serverUrl.contains("192.168.124.135") -> "local"
                else -> "custom"
            }
            customIpInput = if (selectedUrlOption == "custom") {
                serverUrl.substringAfter("http://").substringBefore(":3000").substringBefore("/api/v1/")
            } else {
                "192.168.124.135"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Settings Gear Icon
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Server Settings",
                tint = Color.White
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text(text = "Server Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                },
                containerColor = CardDark,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select server connection option:",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        
                        // Onrender Cloud
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUrlOption = "onrender" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedUrlOption == "onrender",
                                onClick = { selectedUrlOption = "onrender" },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Onrender Cloud", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(text = "https://vams-backend.onrender.com", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        // Local PC Server
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUrlOption = "local" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedUrlOption == "local",
                                onClick = { selectedUrlOption = "local" },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Local PC Server", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(text = "http://192.168.124.135:3000", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        // Custom Local IP
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUrlOption = "custom" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedUrlOption == "custom",
                                onClick = { selectedUrlOption = "custom" },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Custom Local IP (Wi-Fi)", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(text = "Connect to PC running NestJS locally", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        if (selectedUrlOption == "custom") {
                            OutlinedTextField(
                                value = customIpInput,
                                onValueChange = { customIpInput = it },
                                label = { Text("PC IP Address") },
                                singleLine = true,
                                placeholder = { Text("e.g. 192.168.124.135") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalUrl = when (selectedUrlOption) {
                                "onrender" -> "https://vams-backend.onrender.com/api/v1/"
                                "local" -> "http://192.168.124.135:3000/api/v1/"
                                else -> {
                                    var ip = customIpInput.trim()
                                    if (ip.startsWith("http://")) ip = ip.substringAfter("http://")
                                    if (ip.endsWith("/")) ip = ip.dropLast(1)
                                    "http://$ip:3000/api/v1/"
                                }
                            }
                            viewModel.setServerUrl(finalUrl)
                            showSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shake(shakeTrigger),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Logo and Tagline
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Build",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "VAMS Login",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Connect to the inspection server",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Editable monospace SERVER URL
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { viewModel.setServerUrl(it) },
                    label = { Text("SERVER URL") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(serverUrl)) }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isServerFocused = it.isFocused }
                        .border(
                            width = if (isServerFocused) 2.dp else 1.dp,
                            color = if (isServerFocused) Accent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // COMPANY ID field
                OutlinedTextField(
                    value = companyIdInput,
                    onValueChange = { viewModel.setCompanyIdInput(it) },
                    label = { Text("COMPANY CODE / ID") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val text = clipboardManager.getText()?.text
                            if (!text.isNullOrEmpty()) {
                                viewModel.setCompanyIdInput(text)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isCompanyFocused = it.isFocused }
                        .border(
                            width = if (isCompanyFocused) 2.dp else 1.dp,
                            color = if (isCompanyFocused) Accent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue Button
                Button(
                    onClick = { viewModel.validateCompany() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Company Confirmed Card Slide-Transition
                AnimatedVisibility(
                    visible = step is LoginStep.CompanyConfirmed,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    val currentCompany = (step as? LoginStep.CompanyConfirmed)?.company
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PrimaryBlue)
                            Text(
                                text = "Confirmed: ${currentCompany?.name ?: "Hyundai"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            TextButton(onClick = { viewModel.proceedToUserLogin() }) {
                                Text("Go to User Login", color = Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text("Register a new company", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}
