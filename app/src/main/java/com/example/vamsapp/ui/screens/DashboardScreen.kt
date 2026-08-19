package com.example.vamsapp.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.Alert
import com.example.vamsapp.model.User
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.ui.components.*
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay

@Composable
fun CountUpText(
    value: Int,
    fontSize: TextUnit = 28.sp,
    color: Color = Color.White
) {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(value) {
        val duration = 800L
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= duration) {
                count = value
                break
            }
            count = ((elapsed.toFloat() / duration) * value).toInt()
            delay(16)
        }
    }
    Text(
        text = count.toString(),
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        color = color
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    viewModel: DashboardViewModel,
    onNavigateToDetails: (String, Alert?) -> Unit,
    onNavigateToDefects: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedSeverity by viewModel.selectedSeverity.collectAsState()

    val companyName = VamsPrefs.getCompanyName() ?: "My Company"
    val context = LocalContext.current
    var alertToResolve by remember { mutableStateOf<Alert?>(null) }

    LaunchedEffect(user.id) {
        viewModel.startPolling(user)
    }

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }



    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
            com.example.vamsapp.service.SoundService.stopAllSounds()
        }
    }

    // Critical alarm badge pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VAMS Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(companyName, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                actions = {
                    // Notification Bell with Pulsing Ring on Critical Alerts
                    val rawAlerts by viewModel.rawAlerts.collectAsState()
                    val unreadCount = rawAlerts.count { it.status == "OPEN" }
                    val hasCritical = rawAlerts.any { it.severity.equals("CRITICAL", ignoreCase = true) && it.status == "OPEN" }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onNavigateToNotifications() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .scale(if (hasCritical) badgeScale else 1f)
                                    .background(SeverityCritical, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Profile Circle Initials Avatar
                    val initials = user.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .background(Accent, shape = CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
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
            // Role chip badge
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = user.role,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // 2x2 Metric Cards Grid
            // Let's fetch metrics dynamically or fallback to list sizes
            val roleFilteredAlerts by viewModel.roleFilteredAlerts.collectAsState()
            val total = roleFilteredAlerts.size
            val open = roleFilteredAlerts.count { it.status.uppercase() == "OPEN" }
            val resolved = roleFilteredAlerts.count { it.status.uppercase() == "RESOLVED" }
            val critical = roleFilteredAlerts.count { it.severity.equals("CRITICAL", ignoreCase = true) && it.status.uppercase() == "OPEN" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Defects",
                    value = total,
                    stripeColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Open Defects",
                    value = open,
                    stripeColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Resolved Defects",
                    value = resolved,
                    stripeColor = Success,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Critical Alerts",
                    value = critical,
                    stripeColor = SeverityCritical,
                    isPulse = critical > 0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Defect Status Filter Bar (Row 1)
            Text("Status Filter", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "REOPENED")) { status ->
                    CustomFilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.selectStatus(status, user) },
                        label = status
                    )
                }
            }

            // Severity Filter Bar (Row 2)
            Text("Severity Filter", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW")) { severity ->
                    val color = when (severity) {
                        "CRITICAL" -> SeverityCritical
                        "HIGH" -> SeverityHigh
                        "MEDIUM" -> SeverityMedium
                        "LOW" -> SeverityLow
                        else -> Accent
                    }
                    CustomFilterChip(
                        selected = selectedSeverity == severity,
                        onClick = { viewModel.selectSeverity(severity, user) },
                        label = severity,
                        activeColor = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Defects List Header
            Text(
                text = "Latest Defect Incidents",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No alerts yet", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Events will appear when defects are detected on the line",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { alert ->
                        DefectIncidentCard(
                            alert = alert,
                            currentUser = user,
                            onClick = { onNavigateToDetails(alert.id, alert) },
                            onTakeOver = { viewModel.takeOverAlert(alert.id, user) },
                            onResolve = { alertToResolve = alert }
                        )
                    }
                }
            }
        }

        val resolvingAlert = alertToResolve
        if (resolvingAlert != null) {
            ResolveAlertBottomSheet(
                onDismiss = { alertToResolve = null },
                onSubmit = { reason, notes, _, _ ->
                    viewModel.resolveAlert(resolvingAlert.id, reason, notes, user)
                    alertToResolve = null
                }
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: Int,
    stripeColor: Color,
    isPulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pulse animation details
    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .height(96.dp)
            .border(
                width = if (isPulse) 2.dp else 0.dp,
                color = if (isPulse) stripeColor.copy(alpha = borderAlpha) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left stripe indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(stripeColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontSize = 11.sp, color = TextSecondary)
                CountUpText(value = value, color = Color.White)
            }
        }
    }
}

@Composable
fun DefectIncidentCard(
    alert: Alert,
    currentUser: User,
    onClick: () -> Unit,
    onTakeOver: () -> Unit,
    onResolve: () -> Unit
) {
    val stripeColor = when (alert.severity.uppercase()) {
        "CRITICAL" -> SeverityCritical
        "HIGH" -> SeverityHigh
        "MEDIUM" -> SeverityMedium
        else -> SeverityLow
    }

    val statusColor = when (alert.status.uppercase()) {
        "OPEN" -> Warning
        "RESOLVED" -> Success
        "IN_PROGRESS" -> PrimaryBlue
        else -> SeverityMedium
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.defectName ?: alert.defect?.name ?: "Not specified",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = alert.status,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.definition ?: "No definition provided",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "VIN: " + (alert.vin ?: "Not specified"), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                    if (alert.status.equals("RESOLVED", ignoreCase = true)) {
                        Text(text = "Resolved by: ${alert.resolution?.resolvedByUser?.name ?: "Someone"}", fontSize = 11.sp, color = Success, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "Assigned: " + (alert.assignedToRole ?: "Unassigned"), fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = stripeColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = alert.severity,
                            color = stripeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(text = getRelativeTimeSpan(alert.updatedAt), fontSize = 10.sp, color = TextSecondary)
                }

                // Inline action buttons
                val showResolve = false
                val showTakeOver = alert.status != "RESOLVED" && alert.assignedToUserId != currentUser.id

                if (showTakeOver || showResolve) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DividerColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showTakeOver) {
                            Button(
                                onClick = onTakeOver,
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
                                onClick = onResolve,
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
}

@Composable
private fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    activeColor: Color = PrimaryBlue
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) activeColor else CardDark,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) activeColor else DividerColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun getRelativeTimeSpan(isoString: String): String {
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = sdf.parse(isoString) ?: return "Just now"
        val diffMs = System.currentTimeMillis() - date.time
        val diffMin = diffMs / (1000 * 60)
        val diffHours = diffMin / 60
        val diffDays = diffHours / 24

        return when {
            diffMin < 1 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            else -> "${diffDays}d ago"
        }
    } catch (e: Exception) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoString) ?: return "Just now"
            val diffMs = System.currentTimeMillis() - date.time
            val diffMin = diffMs / (1000 * 60)
            val diffHours = diffMin / 60
            val diffDays = diffHours / 24

            return when {
                diffMin < 1 -> "Just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffHours < 24 -> "${diffHours}h ago"
                else -> "${diffDays}d ago"
            }
        } catch (e2: Exception) {
            return "Just now"
        }
    }
}
