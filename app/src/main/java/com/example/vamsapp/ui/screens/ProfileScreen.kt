package com.example.vamsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.User
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onLogout: () -> Unit
) {
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val muteDuration by viewModel.muteDuration.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var expandedMuteDropdown by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Large Initials Avatar (Accent Gradient Border)
            val initials = user.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(Accent, PrimaryBlue)),
                        shape = CircleShape
                    )
                    .background(CardDark, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }

            // User Info
            Text(text = user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            // Company name chip
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = VamsPrefs.getCompanyName() ?: "My Company", color = Color.White, fontSize = 12.sp)
                }
            }

            Text(text = "Role: ${user.role} | Dept: Line Assembly", fontSize = 12.sp, color = TextSecondary)

            // Info Card (copyable user ID and Email)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(user.email)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("EMAIL", fontSize = 9.sp, color = TextSecondary)
                            Text(user.email, color = Color.White, fontSize = 14.sp)
                        }
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    Divider(color = DividerColor)

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(user.id)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("USER ID", fontSize = 9.sp, color = TextSecondary)
                            Text(user.id, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Active Users Navigation Button (COMPANY_ADMIN only)
            if (user.role == "COMPANY_ADMIN") {
                Button(
                    onClick = onNavigateToUsers,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.People, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Company Users", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Toggles / Preference configs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("System Preferences", fontWeight = FontWeight.Bold, color = Color.White)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Siren & Sound notifications", color = Color.White, fontSize = 13.sp)
                        Switch(checked = isSoundEnabled, onCheckedChange = { viewModel.toggleSoundNotifications(it) })
                    }

                    // Temporary mute sirens config
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mute all sirens for:", color = Color.White, fontSize = 13.sp)
                        Box {
                            Text(
                                text = muteDuration,
                                color = Accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { expandedMuteDropdown = true }
                            )
                            DropdownMenu(expanded = expandedMuteDropdown, onDismissRequest = { expandedMuteDropdown = false }) {
                                listOf("None", "1h", "4h", "Until Tomorrow").forEach { dur ->
                                    DropdownMenuItem(
                                        text = { Text(dur) },
                                        onClick = {
                                            viewModel.setMuteDuration(dur)
                                            expandedMuteDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))

                    val context = LocalContext.current
                    var isTestingSiren by remember { mutableStateOf(false) }

                    DisposableEffect(Unit) {
                        onDispose {
                            if (isTestingSiren) {
                                com.example.vamsapp.service.SoundService.stopAllSounds()
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Test Siren Sound", color = Color.White, fontSize = 13.sp)
                        Button(
                            onClick = {
                                if (isTestingSiren) {
                                    com.example.vamsapp.service.SoundService.stopAllSounds()
                                    isTestingSiren = false
                                } else {
                                    com.example.vamsapp.service.SoundService.startContinuousSiren(context, 2000)
                                    isTestingSiren = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTestingSiren) SeverityCritical else PrimaryBlue
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isTestingSiren) "Stop" else "Play",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SeverityCritical),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Logout / Sign Out", fontWeight = FontWeight.Bold)
            }
        }

        // Confirm Logout Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out from VAMS?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout { onLogout() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeverityCritical)
                    ) {
                        Text("Confirm Sign Out", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
