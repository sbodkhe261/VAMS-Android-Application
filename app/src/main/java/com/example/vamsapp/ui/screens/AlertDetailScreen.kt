package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.Alert
import com.example.vamsapp.model.Comment
import com.example.vamsapp.model.User
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.ui.components.*
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.AlertDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    user: User,
    alertId: String,
    viewModel: AlertDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val alert by viewModel.alert.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()

    var showAssignSheet by remember { mutableStateOf(false) }
    var showResolveSheet by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf(false) }
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var reopenReason by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(alertId) {
        viewModel.fetchDetails(alertId)
        VamsPrefs.markAlertSeen(alertId)
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alertId.hashCode())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetActionState()
        }
    }

    LaunchedEffect(actionSuccess) {
        if (actionSuccess) {
            onNavigateBack()
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = alert?.defectName ?: alert?.defect?.name ?: "Defect Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Alert Mute Button
                    IconButton(onClick = { viewModel.toggleMuteAlert(alertId) }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (isMuted) SeverityCritical else Success
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        bottomBar = {
            alert?.let { currentAlert ->
                BottomActionBar(
                    user = user,
                    alert = currentAlert,
                    onAssign = { showAssignSheet = true },
                    onResolve = { showResolveSheet = true },
                    onReopen = { showReopenDialog = true },
                    onAddComment = { showCommentInput = !showCommentInput },
                    onTakeOver = { viewModel.takeOverAlert(alertId, user) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    alert?.let { currentAlert ->
                        // 1. Vehicle Info
                        item {
                            SectionTitle("Vehicle Information")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "VIN: " + (currentAlert.vin ?: "Not specified"),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(currentAlert.vin ?: ""))
                                            }
                                        )
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Source Station: " + (if (currentAlert.isManual) "Manual Dispatch" else "Automated Webhook"), color = TextSecondary, fontSize = 12.sp)
                                    Text(text = "Ingested At: " + formatAbsoluteTime(currentAlert.createdAt, includeSeconds = true), color = TextSecondary, fontSize = 12.sp)
                                    Text(text = "Last Action At: " + formatAbsoluteTime(currentAlert.updatedAt, includeSeconds = true), color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. Defect Info
                        item {
                            SectionTitle("Defect Information")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = currentAlert.defectName ?: currentAlert.defect?.name ?: "Not specified", fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SeverityBadge(currentAlert.severity)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = currentAlert.defect?.category ?: "Not specified",
                                                color = Accent,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Sound Profile: " + (currentAlert.defect?.soundProfile ?: "Not specified"),
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "Trigger Definition", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = currentAlert.definition ?: "Not specified",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "Escalation Timeout", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (currentAlert.alertDefinition != null) "${currentAlert.alertDefinition.escalationTimeout} minutes" else "Not specified",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        // 3. Assignment
                        item {
                            SectionTitle("Assignment details")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(PrimaryBlue, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (currentAlert.assignedToUserId != null) "US" else "?",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (currentAlert.assignedToUserId != null) "Assigned Engineer" else "Unassigned",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Role: " + (currentAlert.assignedToRole ?: "None") +
                                                        " | Dept: " + (currentAlert.assignedToDepartment ?: "Not specified") +
                                                        " | Team: " + (currentAlert.assignedToTeam ?: "Not specified"),
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Timeline Audit Log
                        item {
                            SectionTitle("Timeline & Audit Log")
                            TimelineSection(alert = currentAlert)
                        }

                        // 5. Resolution Block
                        if (currentAlert.status == "RESOLVED") {
                            item {
                                SectionTitle("Resolution Report")
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(1.dp, Success, RoundedCornerShape(10.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Resolved by: ${currentAlert.resolution?.resolvedByUser?.name ?: "Unknown"} (${currentAlert.resolution?.resolvedByUser?.role ?: "WORKER"})",
                                            fontWeight = FontWeight.Bold,
                                            color = Success,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Resolution Reason", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                                        Text(
                                            text = currentAlert.resolution?.reason ?: "No reason provided",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        
                                        if (!currentAlert.resolution?.notes.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Quality Check Notes", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                                            Text(
                                                text = currentAlert.resolution.notes,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        
                                        // Real Audio player
                                        val audioPath = currentAlert.resolution?.audioPath
                                        if (!audioPath.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                var isPlaying by remember { mutableStateOf(false) }
                                                var playDurationMs by remember { mutableIntStateOf(0) }
                                                var playElapsedMs by remember { mutableIntStateOf(0) }
                                                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                                                val context = androidx.compose.ui.platform.LocalContext.current

                                                var localFile by remember { mutableStateOf<java.io.File?>(null) }
                                                
                                                LaunchedEffect(audioPath) {
                                                    val absoluteUrl = com.example.vamsapp.network.ApiClient.getAbsoluteUrl(audioPath)
                                                    if (!absoluteUrl.isNullOrEmpty()) {
                                                        val ext = if (audioPath.endsWith(".mp4", ignoreCase = true)) ".mp4" else ".m4a"
                                                        val cacheFile = java.io.File(context.cacheDir, "cached_res_voice_${currentAlert.id}$ext")
                                                        if (cacheFile.exists() && cacheFile.length() > 0) {
                                                            localFile = cacheFile
                                                        } else {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                try {
                                                                    val url = java.net.URL(absoluteUrl)
                                                                    val connection = url.openConnection() as java.net.HttpURLConnection
                                                                    connection.requestMethod = "GET"
                                                                    connection.connect()
                                                                    if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                                                        java.io.BufferedInputStream(connection.inputStream).use { input ->
                                                                            java.io.FileOutputStream(cacheFile).use { output ->
                                                                                val data = ByteArray(1024)
                                                                                var count: Int
                                                                                while (input.read(data).also { count = it } != -1) {
                                                                                    output.write(data, 0, count)
                                                                                }
                                                                                output.flush()
                                                                            }
                                                                        }
                                                                        if (cacheFile.length() > 0) {
                                                                            localFile = cacheFile
                                                                        } else {
                                                                            cacheFile.delete()
                                                                        }
                                                                    } else {
                                                                        if (cacheFile.exists()) {
                                                                            cacheFile.delete()
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                    if (cacheFile.exists()) {
                                                                        cacheFile.delete()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Parse duration from URL/path name (e.g. _dur_8.m4a or .mp4)
                                                val durationFromUrl = audioPath.substringAfter("_dur_").substringBefore(".m4a").substringBefore(".mp4").toIntOrNull()
                                                val totalDurationSec = durationFromUrl ?: 12
                                                val displayDuration = if (playDurationMs > 0) playDurationMs / 1000 else totalDurationSec

                                                val currentProgress = if (playDurationMs > 0) {
                                                    playElapsedMs.toFloat() / playDurationMs.toFloat()
                                                } else {
                                                    0f
                                                }

                                                val displayElapsed = playElapsedMs / 1000

                                                fun stopPlayback() {
                                                    isPlaying = false
                                                    try {
                                                        mediaPlayer?.stop()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    } finally {
                                                        mediaPlayer?.release()
                                                        mediaPlayer = null
                                                    }
                                                    playElapsedMs = 0
                                                }

                                                fun startPlayback() {
                                                    try {
                                                        val player = android.media.MediaPlayer().apply {
                                                            setAudioAttributes(
                                                                android.media.AudioAttributes.Builder()
                                                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                                                    .build()
                                                            )
                                                            
                                                            val cached = localFile
                                                            if (cached != null && cached.exists() && cached.length() > 0) {
                                                                val fis = java.io.FileInputStream(cached)
                                                                setDataSource(fis.fd)
                                                                prepare()
                                                                fis.close()
                                                                start()
                                                                isPlaying = true
                                                                playDurationMs = duration
                                                            } else {
                                                                val absoluteUrl = com.example.vamsapp.network.ApiClient.getAbsoluteUrl(audioPath)
                                                                if (absoluteUrl.isNullOrEmpty()) return
                                                                
                                                                android.util.Log.d("MediaPlayer", "Loading audio from: $absoluteUrl")
                                                                android.widget.Toast.makeText(context, "Loading audio verification note...", android.widget.Toast.LENGTH_SHORT).show()
                                                                
                                                                setDataSource(absoluteUrl)
                                                                prepareAsync()
                                                                setOnPreparedListener { mp ->
                                                                    mp.start()
                                                                    isPlaying = true
                                                                    playDurationMs = mp.duration
                                                                }
                                                            }
                                                            
                                                            setOnCompletionListener {
                                                                stopPlayback()
                                                            }
                                                            setOnErrorListener { _, _, _ ->
                                                                stopPlayback()
                                                                try {
                                                                    val ext = if (audioPath.endsWith(".mp4", ignoreCase = true)) ".mp4" else ".m4a"
                                                                    val cacheFile = java.io.File(context.cacheDir, "cached_res_voice_${currentAlert.id}$ext")
                                                                    if (cacheFile.exists()) {
                                                                        cacheFile.delete()
                                                                    }
                                                                    localFile = null
                                                                } catch (ex: Exception) {
                                                                    ex.printStackTrace()
                                                                }
                                                                android.widget.Toast.makeText(context, "Error playing audio note, retrying...", android.widget.Toast.LENGTH_SHORT).show()
                                                                true
                                                            }
                                                        }
                                                        mediaPlayer = player
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        android.widget.Toast.makeText(context, "Failed to load audio: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }

                                                LaunchedEffect(isPlaying) {
                                                    if (isPlaying) {
                                                        while (isPlaying) {
                                                            try {
                                                                mediaPlayer?.let { mp ->
                                                                    if (mp.isPlaying) {
                                                                        playElapsedMs = mp.currentPosition
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                            kotlinx.coroutines.delay(250)
                                                        }
                                                    }
                                                }

                                                DisposableEffect(mediaPlayer) {
                                                    onDispose {
                                                        mediaPlayer?.let { mp ->
                                                            try {
                                                                mp.release()
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = {
                                                        if (isPlaying) {
                                                            stopPlayback()
                                                        } else {
                                                            startPlayback()
                                                        }
                                                    }) {
                                                        Icon(
                                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = if (isPlaying) "Stop" else "Play",
                                                            tint = Success
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    LinearProgressIndicator(
                                                        progress = { currentProgress.coerceIn(0f, 1f) },
                                                        modifier = Modifier.weight(1f),
                                                        color = Success,
                                                        trackColor = DividerColor,
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isPlaying) {
                                                            String.format(java.util.Locale.US, "%d:%02d / %d:%02d", displayElapsed / 60, displayElapsed % 60, displayDuration / 60, displayDuration % 60)
                                                        } else {
                                                            String.format(java.util.Locale.US, "%d:%02d", displayDuration / 60, displayDuration % 60)
                                                        },
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Comments Box Toggle
                        if (showCommentInput) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardDark),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = commentText,
                                            onValueChange = { commentText = it },
                                            placeholder = { Text("Add supervisor notes...", color = TextSecondary) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = {
                                            if (commentText.isNotEmpty()) {
                                                viewModel.addComment(alertId, commentText)
                                                commentText = ""
                                                showCommentInput = false
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Accent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Sheets & Dialogs
        if (showAssignSheet) {
            AssignAlertBottomSheet(
                onDismiss = { showAssignSheet = false },
                onSubmit = { userId, role, dept, team, notes ->
                    viewModel.reassignAlert(alertId, userId, role, dept, team, notes)
                    showAssignSheet = false
                }
            )
        }

        if (showResolveSheet) {
            ResolveAlertBottomSheet(
                onDismiss = { showResolveSheet = false },
                onSubmit = { reason, notes, file, text ->
                    viewModel.resolveAlert(alertId, reason, notes, file, text, emptyList())
                    showResolveSheet = false
                }
            )
        }

        if (showReopenDialog) {
            AlertDialog(
                onDismissRequest = { showReopenDialog = false },
                title = { Text("Reopen Alert Incident") },
                text = {
                    OutlinedTextField(
                        value = reopenReason,
                        onValueChange = { reopenReason = it },
                        label = { Text("Reason for Reopening") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.reopenAlert(alertId, reopenReason)
                            showReopenDialog = false
                            reopenReason = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeverityCritical)
                    ) {
                        Text("Reopen Alert", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReopenDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun TimelineSection(alert: Alert) {
    // Render timeline steps
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, shape = RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        val timelineEvents = alert.timeline
        if (!timelineEvents.isNullOrEmpty()) {
            timelineEvents.forEachIndexed { index, event ->
                val isLast = index == timelineEvents.size - 1
                val (title, icon, color) = when (event.actionType) {
                    "CREATED" -> Triple("Defect Incident Created", Icons.Default.Add, PrimaryBlue)
                    "ASSIGNED" -> {
                        val isTakeOver = event.details.contains("take over", ignoreCase = true) ||
                                event.details.contains("taken over", ignoreCase = true)
                        val displayTitle = if (isTakeOver) "Defect Incident Taken Over" else "Defect Incident Assigned"
                        Triple(displayTitle, Icons.Default.Person, Accent)
                    }
                    "RESOLVED" -> Triple("Defect Incident Resolved", Icons.Default.Check, Success)
                    "REOPENED" -> Triple("Defect Incident Reopened", Icons.Default.Refresh, SeverityCritical)
                    "ESCALATED" -> Triple("Defect Incident Escalated", Icons.Default.Warning, SeverityHigh)
                    "NOTE_ADDED" -> Triple("Supervisor Note Added", Icons.Default.Comment, TextSecondary)
                    else -> Triple("Defect Incident Update", Icons.Default.Warning, TextSecondary)
                }

                val formattedTime = formatAbsoluteTime(event.createdAt, includeSeconds = true)

                TimelineNode(
                    title = title,
                    subtitle = event.details,
                    time = formattedTime,
                    color = color,
                    icon = icon,
                    showConnector = !isLast
                )
            }
        } else {
            // Fallback to original static/dummy rendering
            val showAssigned = alert.assignedToUserId != null
            val showResolved = alert.status == "RESOLVED"

            TimelineNode(
                title = "Defect Incident Created",
                subtitle = "Vision inspection camera sensor triggered alert",
                time = formatAbsoluteTime(alert.createdAt, includeSeconds = true),
                color = PrimaryBlue,
                icon = Icons.Default.Add,
                showConnector = showAssigned || showResolved
            )
            if (showAssigned) {
                TimelineNode(
                    title = "Defect Incident Assigned",
                    subtitle = "Assigned to: " + (alert.assignedToRole ?: "Worker"),
                    time = formatAbsoluteTime(alert.updatedAt, includeSeconds = true),
                    color = Accent,
                    icon = Icons.Default.Person,
                    showConnector = showResolved
                )
            }
            if (showResolved) {
                TimelineNode(
                    title = "Defect Incident Resolved",
                    subtitle = "Resolved and quality checked",
                    time = formatAbsoluteTime(alert.updatedAt, includeSeconds = true),
                    color = Success,
                    icon = Icons.Default.Check,
                    showConnector = false
                )
            }
        }
    }
}

@Composable
fun TimelineNode(
    title: String,
    subtitle: String,
    time: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showConnector: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = 0.2f), shape = CircleShape)
                    .border(1.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            if (showConnector) {
                // Timeline connector line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(DividerColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            Text(text = "Time: $time", color = TextSecondary, fontSize = 9.sp, fontStyle = FontStyle.Italic)
            if (!showConnector) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BottomActionBar(
    user: User,
    alert: Alert,
    onAssign: () -> Unit,
    onResolve: () -> Unit,
    onReopen: () -> Unit,
    onAddComment: () -> Unit,
    onTakeOver: () -> Unit
) {
    Surface(
        color = CardDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // General "Add Comment" button visible to almost everyone
            IconButton(onClick = onAddComment) {
                Icon(imageVector = Icons.Default.Comment, contentDescription = "Add Comment", tint = Color.White)
            }

            // Global Assign/Reassign Button (available to all users)
            Button(
                onClick = onAssign,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (alert.assignedToUserId != null) "Reassign" else "Assign",
                    color = Color.White,
                    maxLines = 1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Global Reopen Button if status is RESOLVED (available to all users)
            if (alert.status == "RESOLVED") {
                Button(
                    onClick = onReopen,
                    colors = ButtonDefaults.buttonColors(containerColor = SeverityCritical),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reopen", color = Color.White, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                }
            }

            // Global action buttons
            val showResolve = alert.status != "RESOLVED"
            val showTakeOver = alert.status != "RESOLVED" && alert.assignedToUserId == null

            if (showResolve) {
                Button(
                    onClick = onResolve,
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve", color = Color.White, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                }
            }

            if (showTakeOver) {
                Button(
                    onClick = onTakeOver,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Take Over", color = Color.White, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun formatAbsoluteTime(isoString: String, includeSeconds: Boolean = false): String {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ"
    )
    var date: java.util.Date? = null
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            date = sdf.parse(isoString)
            if (date != null) break
        } catch (e: Exception) {
            // continue
        }
    }
    if (date == null) return isoString
    
    val pattern = if (includeSeconds) "hh:mm:ss a" else "hh:mm a"
    val localSdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    return localSdf.format(date)
}
