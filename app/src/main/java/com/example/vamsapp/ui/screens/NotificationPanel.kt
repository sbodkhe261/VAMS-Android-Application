package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.Alert
import com.example.vamsapp.model.User
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.AlertDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPanel(
    user: User,
    alerts: List<Alert>,
    viewModel: AlertDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String, Alert?) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MINE, CRITICAL, UNREAD
    var alertToTakeOver by remember { mutableStateOf<Alert?>(null) }
    var alertToResolve by remember { mutableStateOf<Alert?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            "MINE" -> alerts.filter { 
                it.assignedToUserId == user.id || 
                (it.assignedToUserId == null && it.assignedToRole?.equals(user.role, ignoreCase = true) == true)
            }
            "CRITICAL" -> alerts.filter { it.severity.equals("CRITICAL", ignoreCase = true) }
            "UNREAD" -> alerts.filter { it.status == "OPEN" } // OPEN means active unread
            else -> alerts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Filter tabs
            TabRow(
                selectedTabIndex = when (selectedFilter) {
                    "ALL" -> 0
                    "MINE" -> 1
                    "CRITICAL" -> 2
                    else -> 3
                },
                containerColor = BackgroundDark,
                contentColor = Accent
            ) {
                listOf("ALL", "MINE", "CRITICAL", "UNREAD").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilter == title,
                        onClick = { selectedFilter = title },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No notifications", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAlerts) { alert ->
                        NotificationItemCard(
                            alert = alert,
                            currentUser = user,
                            onTap = { onNavigateToDetails(alert.id, alert) },
                            onTakeOver = {
                                alertToTakeOver = alert
                                showConfirmDialog = true
                            },
                            onResolve = {
                                alertToResolve = alert
                            }
                        )
                    }
                }
            }
        }

        // Confirm Reassignment dialog
        val takingOverAlert = alertToTakeOver
        if (showConfirmDialog && takingOverAlert != null) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Take Over Alert") },
                text = { Text("Reassign this alert (#${takingOverAlert.id.take(8)}) to yourself?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.takeOverAlert(takingOverAlert.id, user)
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Confirm", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val resolvingAlert = alertToResolve
        if (resolvingAlert != null) {
            ResolveAlertBottomSheet(
                onDismiss = { alertToResolve = null },
                onSubmit = { reason, notes, _, _ ->
                    viewModel.resolveAlert(resolvingAlert.id, reason, notes, null, "", emptyList())
                    alertToResolve = null
                }
            )
        }
    }
}

@Composable
fun NotificationItemCard(
    alert: Alert,
    currentUser: User,
    onTap: () -> Unit,
    onTakeOver: () -> Unit,
    onResolve: () -> Unit
) {
    val indicatorColor = when (alert.severity.uppercase()) {
        "CRITICAL" -> SeverityCritical
        "HIGH" -> SeverityHigh
        "MEDIUM" -> SeverityMedium
        else -> SeverityLow
    }

    val severityIcon = when (alert.severity.uppercase()) {
        "CRITICAL" -> "🔴"
        "HIGH" -> "🟠"
        "MEDIUM" -> "🟡"
        else -> "🟢"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Read/Unread Status Indicator: left blue dot
                if (alert.status == "OPEN") {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(8.dp)
                            .background(Accent, shape = RoundedCornerShape(4.dp))
                    )
                }

                Text(
                    text = "$severityIcon New ${alert.severity} alert: ${alert.defect?.name ?: "Brake Fluid Leak"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Assigned to: ${alert.assignedToRole ?: "None"} | VIN: ${alert.vin}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Status: " + alert.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getRelativeTimeSpan(alert.updatedAt),
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                val showResolve = alert.status != "RESOLVED"
                val showTakeOver = alert.status != "RESOLVED" && alert.assignedToUserId != currentUser.id

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showTakeOver) {
                        Button(
                            onClick = { onTakeOver() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Take Over", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showResolve) {
                        Button(
                            onClick = { onResolve() },
                            colors = ButtonDefaults.buttonColors(containerColor = Success),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Resolve", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun getRelativeTimeSpan(isoString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoString) ?: return isoString
        val diff = System.currentTimeMillis() - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
    } catch (e: Exception) {
        isoString
    }
}
