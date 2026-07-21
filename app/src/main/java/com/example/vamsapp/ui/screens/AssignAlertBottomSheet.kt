package com.example.vamsapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.ui.theme.CardDark
import com.example.vamsapp.ui.theme.PrimaryBlue
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.model.User
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignAlertBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (userId: String, role: String, department: String, team: String, notes: String) -> Unit
) {
    var userId by remember { mutableStateOf("d50a29e4-bcde-4211-8fa1-71ca36df201a") } // Joe Worker
    var role by remember { mutableStateOf("WORKER") }
    var department by remember { mutableStateOf("Assembly Station A") }
    var team by remember { mutableStateOf("Hydraulics Team") }
    var notes by remember { mutableStateOf("Caliper seal checks needed.") }

    var expandedRoleDropdown by remember { mutableStateOf(false) }
    val roles = listOf("COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "SERVICE_ENGINEER", "WORKER", "QUALITY_INSPECTOR", "DEALER", "VEHICLE_OWNER", "READ_ONLY_USER")

    var usersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var expandedUserDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val companyId = VamsPrefs.getUser()?.companyId
    LaunchedEffect(companyId) {
        if (!companyId.isNullOrEmpty()) {
            isLoadingUsers = true
            ApiClient.apiService.getCompanyUsers(companyId).enqueue(object : retrofit2.Callback<List<User>> {
                override fun onResponse(call: retrofit2.Call<List<User>>, response: retrofit2.Response<List<User>>) {
                    isLoadingUsers = false
                    if (response.isSuccessful) {
                        usersList = response.body() ?: emptyList()
                    }
                }
                override fun onFailure(call: retrofit2.Call<List<User>>, t: Throwable) {
                    isLoadingUsers = false
                }
            })
        }
    }

    var hasInitializedSearchQuery by remember { mutableStateOf(false) }
    LaunchedEffect(usersList) {
        if (usersList.isNotEmpty() && !hasInitializedSearchQuery) {
            val selectedUser = usersList.find { it.id == userId }
            if (selectedUser != null) {
                searchQuery = selectedUser.name
                hasInitializedSearchQuery = true
            }
        }
    }

    val filteredUsers = remember(usersList, searchQuery) {
        if (searchQuery.isEmpty()) {
            usersList
        } else {
            usersList.filter { u ->
                u.name.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                (if (u.role == "FACTORY_MANAGER") "MANAGER" else u.role).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Assign Alert Incident",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // User Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        expandedUserDropdown = true
                    },
                    label = { Text("Select/Search Assignee User") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    expandedUserDropdown = true
                                }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = Color.LightGray)
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUserDropdown)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedUserDropdown,
                    onDismissRequest = { expandedUserDropdown = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (isLoadingUsers) {
                        DropdownMenuItem(
                            text = { Text("Loading users...") },
                            onClick = {}
                        )
                    } else if (filteredUsers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No matching users found") },
                            onClick = {}
                        )
                    } else {
                        filteredUsers.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.name} (${if (u.role == "FACTORY_MANAGER") "MANAGER" else u.role})") },
                                onClick = {
                                    userId = u.id
                                    role = if (u.role == "FACTORY_MANAGER") "MANAGER" else u.role
                                    searchQuery = u.name
                                    expandedUserDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // User ID input
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("Assignee User ID (or enter manually)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Role selection dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assignee Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleDropdown) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedRoleDropdown = !expandedRoleDropdown }
                )
                DropdownMenu(
                    expanded = expandedRoleDropdown,
                    onDismissRequest = { expandedRoleDropdown = false }
                ) {
                    roles.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = {
                                role = r
                                expandedRoleDropdown = false
                            }
                        )
                    }
                }
            }

            // Department input
            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth()
            )

            // Team input
            OutlinedTextField(
                value = team,
                onValueChange = { team = it },
                label = { Text("Team") },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Assignee Instructions / Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        onSubmit(userId, role, department, team, notes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Assign Alert", color = Color.White)
                }
            }
        }
    }
}
