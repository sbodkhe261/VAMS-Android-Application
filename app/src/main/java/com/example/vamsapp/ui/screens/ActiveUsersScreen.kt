package com.example.vamsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.User
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.UserListViewModel
import androidx.compose.foundation.border
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveUsersScreen(
    companyId: String,
    viewModel: UserListViewModel,
    onNavigateBack: () -> Unit
) {
    val filteredUsers by viewModel.filteredUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRoleFilter by viewModel.selectedRoleFilter.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedUserForProfileView by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(companyId) {
        viewModel.fetchUsers(companyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Users", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text("Search by name or email...", color = TextSecondary) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )

                // Filter by role chips
                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("ALL", "COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "SERVICE_ENGINEER", "WORKER", "QUALITY_INSPECTOR", "DEALER", "VEHICLE_OWNER", "READ_ONLY_USER")) { role ->
                        FilterChip(
                            selected = selectedRoleFilter == role,
                            onClick = { viewModel.filterByRole(role) },
                            label = { Text(role, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No users found", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserItemCard(user = user, onClick = { selectedUserForProfileView = user })
                        }
                    }
                }
            }

            // Read-only profile view dialog
            selectedUserForProfileView?.let { selectedUser ->
                AlertDialog(
                    onDismissRequest = { selectedUserForProfileView = null },
                    title = { Text("User Information") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Name: ${selectedUser.name}", fontWeight = FontWeight.Bold)
                            Text("Email: ${selectedUser.email}")
                            Text("Role: ${if (selectedUser.role.equals("FACTORY_MANAGER", ignoreCase = true)) "MANAGER" else selectedUser.role}")
                            Text("ID: ${selectedUser.id}")
                            Text("Status: Active")
                        }
                    },
                    confirmButton = {
                        Button(onClick = { selectedUserForProfileView = null }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserItemCard(
    user: User,
    onClick: () -> Unit
) {
    val roleColor = when (user.role.uppercase()) {
        "COMPANY_ADMIN", "SUPER_ADMIN" -> SeverityCritical
        "MANAGER", "FACTORY_MANAGER" -> SeverityHigh
        "SUPERVISOR" -> SeverityMedium
        else -> SeverityLow
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar
            val initials = user.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(roleColor.copy(alpha = 0.2f), shape = CircleShape)
                    .border(1.dp, roleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Text(text = user.email, color = TextSecondary, fontSize = 11.sp)
                Text(text = "Dept: General Assembly | Team: Line 1", color = TextSecondary, fontSize = 11.sp)
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Online indicator dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Success, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Online", fontSize = 10.sp, color = TextSecondary)
                }

                // Role chip pill
                Card(
                    colors = CardDefaults.cardColors(containerColor = roleColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (user.role.equals("FACTORY_MANAGER", ignoreCase = true)) "MANAGER" else user.role,
                        color = roleColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
