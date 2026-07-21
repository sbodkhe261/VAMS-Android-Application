package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.RegistrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel, 
    onNavigateBack: () -> Unit
) {
    val companyId by viewModel.companyId.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val department by viewModel.department.collectAsState()
    val team by viewModel.team.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val registrationSuccess by viewModel.registrationSuccess.collectAsState()

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        if (registrationSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Success,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Account Created",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Awaiting admin approval before access is granted.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Return to Login", color = Color.White)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Top Navigator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register User", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Connected Dots Stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepDot(stepNum = 1, active = currentStep >= 1)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(2.dp)
                            .background(if (currentStep >= 2) PrimaryBlue else DividerColor)
                    )
                    StepDot(stepNum = 2, active = currentStep >= 2)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                    } else {
                                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                    }
                                },
                                label = "registration_stepper"
                            ) { step ->
                                when (step) {
                                    1 -> Step1Content(
                                        viewModel = viewModel,
                                        companyId = companyId,
                                        fullName = fullName,
                                        email = email
                                    )
                                    2 -> Step2Content(
                                        viewModel = viewModel,
                                        password = password,
                                        confirmPassword = confirmPassword,
                                        selectedRole = selectedRole,
                                        department = department,
                                        team = team
                                    )
                                }
                            }

                            if (error != null) {
                                Text(
                                    text = error ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (currentStep == 2) {
                                    OutlinedButton(onClick = { viewModel.prevStep() }) {
                                        Text("Previous", color = Accent)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Button(
                                    onClick = {
                                        if (currentStep == 1) {
                                            viewModel.nextStep()
                                        } else {
                                            viewModel.registerUser()
                                        }
                                    },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(if (currentStep == 1) "Next" else "Register", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepDot(stepNum: Int, active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (active) PrimaryBlue else DividerColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stepNum.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun Step1Content(
    viewModel: RegistrationViewModel,
    companyId: String,
    fullName: String,
    email: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Step 1: Profile Isolation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        OutlinedTextField(
            value = companyId,
            onValueChange = { viewModel.setCompanyId(it) },
            label = { Text("Company ID") },
            singleLine = true,
            trailingIcon = {
                if (companyId.length >= 10) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Success)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = { viewModel.setFullName(it) },
            label = { Text("Full Name") },
            singleLine = true,
            trailingIcon = {
                if (fullName.length > 2) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Success)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.setEmail(it) },
            label = { Text("Email Address") },
            singleLine = true,
            trailingIcon = {
                val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
                if (emailPattern.matches(email.trim())) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Success)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2Content(
    viewModel: RegistrationViewModel,
    password: String,
    confirmPassword: String,
    selectedRole: String,
    department: String,
    team: String
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val roles = listOf("COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "SERVICE_ENGINEER", "WORKER", "QUALITY_INSPECTOR", "DEALER", "VEHICLE_OWNER", "READ_ONLY_USER")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Step 2: Access & Credentials", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.setPassword(it) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // Segmented Strength Indicator
        val strength = viewModel.getPasswordStrength()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val colors = listOf(SeverityCritical, SeverityHigh, SeverityMedium, SeverityLow)
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (strength > i) colors[strength - 1] else DividerColor,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text(
            text = "Password Strength: " + when(strength) {
                0 -> "Blank"
                1 -> "Weak"
                2 -> "Moderate"
                3 -> "Strong"
                else -> "Exceptional"
            },
            fontSize = 10.sp,
            color = TextSecondary
        )

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { viewModel.setConfirmPassword(it) },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (confirmPassword.isNotEmpty() && password == confirmPassword) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Success)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Role Dropdown Chip Selector
        Text("Assigned Role:", fontSize = 12.sp, color = TextSecondary)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expandedDropdown = !expandedDropdown }
            )
            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            viewModel.setSelectedRole(role)
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        // Department & Team
        OutlinedTextField(
            value = department,
            onValueChange = { viewModel.setDepartment(it) },
            label = { Text("Department") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = team,
            onValueChange = { viewModel.setTeam(it) },
            label = { Text("Team") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
