package com.example.vamsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vamsapp.model.User
import com.example.vamsapp.model.Alert
import com.example.vamsapp.network.ApiClient
import com.example.vamsapp.network.VamsPrefs
import com.example.vamsapp.network.SocketManager
import com.example.vamsapp.service.SoundService
import com.example.vamsapp.ui.screens.*
import com.example.vamsapp.ui.theme.VAMSAppTheme
import com.example.vamsapp.ui.theme.Accent
import com.example.vamsapp.ui.theme.CardDark
import com.example.vamsapp.ui.theme.PrimaryBlue
import com.example.vamsapp.ui.theme.TextSecondary
import com.example.vamsapp.viewmodel.*
import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val pendingAlertId = mutableStateOf<String?>(null)

    companion object {
        var isAppInForeground = false
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val alertId = intent?.getStringExtra("ALERT_ID")
        if (!alertId.isNullOrEmpty()) {
            pendingAlertId.value = alertId
        }
        
        // Initialize Preferences and programmatics sirens
        VamsPrefs.init(applicationContext)
        SoundService.init(applicationContext)

        // Log FCM Token manually for testing & upload if session exists
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            android.util.Log.d("FCM_TOKEN", "Token is: $token")

            val userToken = VamsPrefs.getAuthToken()
            if (!userToken.isNullOrEmpty()) {
                ApiClient.apiService.updateDeviceToken(com.example.vamsapp.model.UpdateDeviceTokenRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                    override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                        if (response.isSuccessful) {
                            android.util.Log.d("FCM", "Successfully uploaded token to server")
                        } else {
                            android.util.Log.e("FCM", "Failed to upload token: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                        android.util.Log.e("FCM", "Error uploading token: ${t.localizedMessage}")
                    }
                })
            }
        }

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            VAMSAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainNavigation(pendingAlertId)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val alertId = intent.getStringExtra("ALERT_ID")
        if (!alertId.isNullOrEmpty()) {
            pendingAlertId.value = alertId
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object CompanyLogin : Screen()
    object UserLogin : Screen()
    object ForgotPassword : Screen()
    object Registration : Screen()
    object CompanyRegistration : Screen()
    object Dashboard : Screen()
    object Notifications : Screen()
    data class AlertDetails(val id: String, val cachedAlert: Alert? = null) : Screen()
    object DefectCatalog : Screen()
    object ActiveUsers : Screen()
    object Profile : Screen()
}

fun scheduleAlertEscalationWorker(context: android.content.Context) {
    val workRequest = PeriodicWorkRequestBuilder<com.example.vamsapp.service.AlertEscalationWorker>(15, TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "AlertEscalationWork",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

fun cancelAlertEscalationWorker(context: android.content.Context) {
    WorkManager.getInstance(context).cancelUniqueWork("AlertEscalationWork")
}

@Composable
fun MainNavigation(pendingAlertId: MutableState<String?>) {
    val screenStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val currentScreen = screenStack.lastOrNull() ?: Screen.Splash
    var currentUser by remember { mutableStateOf<User?>(null) }

    // Init ViewModels
    val splashViewModel: SplashViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val registrationViewModel: RegistrationViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val detailViewModel: AlertDetailViewModel = viewModel()
    val defectViewModel: DefectCatalogViewModel = viewModel()
    val userListViewModel: UserListViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    var activeInAppNotification by remember { mutableStateOf<InAppNotification?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    // Set up Compose BackHandler to intercept hardware back button clicks
    BackHandler(enabled = true) {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
        } else {
            // Move activity to back instead of finishing/closing the app
            val activity = context as? android.app.Activity
            activity?.moveTaskToBack(true)
        }
    }

    // Process notification tap redirections
    LaunchedEffect(currentUser, pendingAlertId.value) {
        val alertId = pendingAlertId.value
        if (!alertId.isNullOrEmpty() && currentUser != null) {
            pendingAlertId.value = null // consume it
            // Redirect to details screen if not already there
            val last = screenStack.lastOrNull()
            if (last !is Screen.AlertDetails || last.id != alertId) {
                screenStack.add(Screen.AlertDetails(alertId))
            }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Persist user session details so re-login uses their actual account
            VamsPrefs.saveUser(currentUser)

            // Fetch and upload FCM token immediately after successful login
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    android.util.Log.d("FCM", "Uploading FCM token after login: $token")
                    ApiClient.apiService.updateDeviceToken(com.example.vamsapp.model.UpdateDeviceTokenRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                        override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                            if (response.isSuccessful) {
                                android.util.Log.d("FCM", "Successfully uploaded token to server after login")
                            } else {
                                android.util.Log.e("FCM", "Failed to upload token: ${response.code()}")
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                            android.util.Log.e("FCM", "Error uploading token: ${t.localizedMessage}")
                        }
                    })
                }
            }
            
            // Start WorkManager periodic remind sync
            scheduleAlertEscalationWorker(context)
            
            // Start Foreground background service for real-time siren notifications
            try {
                val serviceIntent = android.content.Intent(context, com.example.vamsapp.service.VamsNotificationService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("SERVICE_START", "Failed to start background service due to OS restrictions", e)
            }
            
            SocketManager.setEventListener(object : SocketManager.SocketEventListener {
                override fun onAlertCreated(alertId: String, defectName: String, severity: String, vin: String) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        val title = "New $severity Defect Alert"
                        val message = "$defectName detected on VIN $vin"
                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = message,
                            type = "ALERT",
                            alertId = alertId
                        )
                        // Play sound once (de-duplicated)
                        SoundService.playAlertSoundOnce(context.applicationContext, alertId, severity, bypassSeenCheck = true)
                        
                        // Send system heads-up notification (works in all states)
                        sendSystemHeadsUpNotification(context.applicationContext, title, message)
                        
                        // Refresh Dashboard silently
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }

                override fun onCommentAdded(alertId: String, commentText: String, userName: String) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        val title = "New comment from $userName"
                        val message = commentText
                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = message,
                            type = "COMMENT",
                            alertId = alertId
                        )
                        // Play low sound
                        SoundService.playAlertSoundOnce(context.applicationContext, alertId, "COMMENT", bypassSeenCheck = true)
                        
                        // Send system heads-up notification
                        sendSystemHeadsUpNotification(context.applicationContext, title, message)
                        
                        // Refresh Detail view if open for this alert
                        val current = screenStack.lastOrNull()
                        if (current is Screen.AlertDetails && current.id == alertId) {
                            detailViewModel.fetchDetails(alertId)
                        }
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }

                override fun onAlertResolved(alertId: String, resolvedBy: String, reason: String?) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        val title = "Defect Alert Resolved"
                        val messageText = if (!reason.isNullOrEmpty()) {
                            "Resolved by $resolvedBy. Comment: \"$reason\""
                        } else {
                            "Resolved by $resolvedBy"
                        }
                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = messageText,
                            type = "RESOLVED",
                            alertId = alertId
                        )
                        // Play low sound
                        SoundService.playAlertSoundOnce(context.applicationContext, alertId, "RESOLVED", bypassSeenCheck = true)
                        
                        // Send system heads-up notification
                        sendSystemHeadsUpNotification(context.applicationContext, title, messageText)
                        
                        // Refresh Detail view if open for this alert, and Dashboard
                        val current = screenStack.lastOrNull()
                        if (current is Screen.AlertDetails && current.id == alertId) {
                            detailViewModel.fetchDetails(alertId)
                        }
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }

                override fun onAlertReopened(alertId: String, reopenedBy: String) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        val title = "Defect Alert Reopened"
                        val message = "Reopened by $reopenedBy"
                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = message,
                            type = "REOPENED",
                            alertId = alertId
                        )
                        // Play low sound
                        SoundService.playAlertSoundOnce(context.applicationContext, alertId, "REOPENED", bypassSeenCheck = true)
                        
                        // Send system heads-up notification
                        sendSystemHeadsUpNotification(context.applicationContext, title, message)
                        
                        // Refresh Detail view if open for this alert, and Dashboard
                        val current = screenStack.lastOrNull()
                        if (current is Screen.AlertDetails && current.id == alertId) {
                            detailViewModel.fetchDetails(alertId)
                        }
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }

                override fun onAlertAssigned(alertId: String, title: String, message: String) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        
                        if (title.isNullOrEmpty() || message.isNullOrEmpty()) {
                            android.util.Log.d("MainActivity", "onAlertAssigned: Silent update received. Refreshing views silently.")
                            val current = screenStack.lastOrNull()
                            if (current is Screen.AlertDetails && current.id == alertId) {
                                detailViewModel.fetchDetails(alertId)
                            }
                            dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                            return@post
                        }

                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = message,
                            type = "ASSIGNED",
                            alertId = alertId
                        )
                        // Play low sound
                        SoundService.playAlertSoundOnce(context.applicationContext, alertId, "ASSIGNED", bypassSeenCheck = true)
                        
                        // Send system heads-up notification
                        sendSystemHeadsUpNotification(context.applicationContext, title, message)
                        
                        // Refresh Detail view if open for this alert, and Dashboard
                        val current = screenStack.lastOrNull()
                        if (current is Screen.AlertDetails && current.id == alertId) {
                            detailViewModel.fetchDetails(alertId)
                        }
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }

                override fun onBroadcastCreated(broadcastId: String, title: String, message: String) {
                    mainHandler.post {
                        if (!MainActivity.isAppInForeground) return@post
                        val user = currentUser ?: return@post
                        activeInAppNotification = InAppNotification(
                            title = title,
                            message = message,
                            type = "BROADCAST",
                            alertId = broadcastId
                        )
                        // Play notification sound for broadcasts (same sound profile as other notifications)
                        SoundService.playAlertSoundOnce(context.applicationContext, broadcastId, "HIGH", bypassSeenCheck = true)
                        
                        // Send system heads-up notification
                        sendSystemHeadsUpNotification(context.applicationContext, title, message)
                        
                        dashboardViewModel.fetchTelemetryAndAlerts(user, showLoading = false)
                    }
                }
            })
            SocketManager.connect()
        } else {
            SocketManager.setEventListener(null)
            SocketManager.disconnect()
            
            // Stop WorkManager
            cancelAlertEscalationWorker(context)
            
            // Stop Foreground service
            val serviceIntent = android.content.Intent(context, com.example.vamsapp.service.VamsNotificationService::class.java)
            context.stopService(serviceIntent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val activeUser = currentUser
        if (activeUser != null) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        user = activeUser,
                        viewModel = dashboardViewModel,
                        onNavigateToDetails = { alertId, cachedAlert -> screenStack.add(Screen.AlertDetails(alertId, cachedAlert)) },
                        onNavigateToDefects = { screenStack.add(Screen.DefectCatalog) },
                        onNavigateToUsers = { screenStack.add(Screen.ActiveUsers) },
                        onNavigateToNotifications = { screenStack.add(Screen.Notifications) },
                        onNavigateToProfile = { screenStack.add(Screen.Profile) },
                        onLogout = {
                            currentUser = null
                            loginViewModel.reset()
                            dashboardViewModel.reset()
                            VamsPrefs.clearSession()
                            screenStack.clear()
                            screenStack.add(Screen.CompanyLogin)
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                val token = if (task.isSuccessful) task.result else null
                                ApiClient.apiService.logout(com.example.vamsapp.model.LogoutRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                                    override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {}
                                    override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {}
                                })
                            }
                        }
                    )
                }
                is Screen.Notifications -> {
                    LaunchedEffect(Unit) {
                        dashboardViewModel.fetchTelemetryAndAlerts(activeUser, showLoading = false)
                    }
                    val alertsList by dashboardViewModel.rawAlerts.collectAsState()
                    NotificationPanel(
                        user = activeUser,
                        alerts = alertsList,
                        viewModel = detailViewModel,
                        onNavigateBack = {
                            if (screenStack.size > 1) {
                                screenStack.removeAt(screenStack.size - 1)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            }
                        },
                        onNavigateToDetails = { alertId, cachedAlert -> screenStack.add(Screen.AlertDetails(alertId, cachedAlert)) }
                    )
                }
                is Screen.AlertDetails -> {
                    LaunchedEffect(screen.id, screen.cachedAlert) {
                        detailViewModel.fetchDetails(screen.id, screen.cachedAlert)
                    }
                    AlertDetailScreen(
                        user = activeUser,
                        alertId = screen.id,
                        viewModel = detailViewModel,
                        onNavigateBack = {
                            val updatedAlert = detailViewModel.alert.value
                            if (updatedAlert != null) {
                                dashboardViewModel.selectStatus(updatedAlert.status, activeUser)
                            }
                            dashboardViewModel.fetchTelemetryAndAlerts(activeUser, showLoading = false)
                            if (screenStack.size > 1) {
                                screenStack.removeAt(screenStack.size - 1)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            }
                        }
                    )
                }
                is Screen.DefectCatalog -> {
                    DefectCatalogScreen(
                        user = activeUser,
                        viewModel = defectViewModel,
                        onNavigateBack = {
                            if (screenStack.size > 1) {
                                screenStack.removeAt(screenStack.size - 1)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            }
                        }
                    )
                }
                is Screen.ActiveUsers -> {
                    ActiveUsersScreen(
                        companyId = activeUser.companyId,
                        viewModel = userListViewModel,
                        onNavigateBack = {
                            if (screenStack.size > 1) {
                                screenStack.removeAt(screenStack.size - 1)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            }
                        }
                    )
                }
                is Screen.Profile -> {
                    ProfileScreen(
                        user = activeUser,
                        viewModel = profileViewModel,
                        onNavigateBack = {
                            if (screenStack.size > 1) {
                                screenStack.removeAt(screenStack.size - 1)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            }
                        },
                        onNavigateToUsers = { screenStack.add(Screen.ActiveUsers) },
                        onLogout = {
                            currentUser = null
                            loginViewModel.reset()
                            VamsPrefs.clearSession()
                            screenStack.clear()
                            screenStack.add(Screen.CompanyLogin)
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                val token = if (task.isSuccessful) task.result else null
                                ApiClient.apiService.logout(com.example.vamsapp.model.LogoutRequest(token)).enqueue(object : retrofit2.Callback<Void> {
                                    override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {}
                                    override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {}
                                })
                            }
                        }
                    )
                }
                else -> {
                    // Fallback in case state out of sync
                    LaunchedEffect(Unit) {
                        screenStack.clear()
                        screenStack.add(Screen.Dashboard)
                    }
                }
            }
        } else {
            // When user is null, only allow unauthenticated screens
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    SplashScreen(
                        viewModel = splashViewModel,
                        onNavigateToLogin = {
                            screenStack.clear()
                            screenStack.add(Screen.CompanyLogin)
                        },
                        onNavigateToDashboard = {
                            val savedUser = VamsPrefs.getUser()
                            if (savedUser != null) {
                                currentUser = savedUser
                                screenStack.clear()
                                screenStack.add(Screen.Dashboard)
                            } else {
                                screenStack.clear()
                                screenStack.add(Screen.CompanyLogin)
                            }
                        }
                    )
                }
                is Screen.CompanyLogin -> {
                    CompanyLoginScreen(
                        viewModel = loginViewModel,
                        onNavigateToUserLogin = { screenStack.add(Screen.UserLogin) },
                        onNavigateToRegister = {
                            registrationViewModel.reset()
                            screenStack.add(Screen.CompanyRegistration)
                        }
                    )
                }
                is Screen.UserLogin -> {
                    UserLoginScreen(
                        viewModel = loginViewModel,
                        onNavigateBack = { if (screenStack.size > 1) screenStack.removeAt(screenStack.size - 1) },
                        onNavigateToForgotPassword = { screenStack.add(Screen.ForgotPassword) },
                        onNavigateToRegister = {
                            registrationViewModel.reset(VamsPrefs.getCompanyName() ?: VamsPrefs.getCompanyId())
                            screenStack.add(Screen.Registration)
                        },
                        onLoginSuccess = { user ->
                            currentUser = user
                            screenStack.clear()
                            screenStack.add(Screen.Dashboard)
                        }
                    )
                }
                is Screen.ForgotPassword -> {
                    ForgotPasswordScreen(
                        onNavigateBack = { if (screenStack.size > 1) screenStack.removeAt(screenStack.size - 1) }
                    )
                }
                is Screen.Registration -> {
                    RegistrationScreen(
                        viewModel = registrationViewModel,
                        onNavigateBack = { if (screenStack.size > 1) screenStack.removeAt(screenStack.size - 1) }
                    )
                }
                is Screen.CompanyRegistration -> {
                    CompanyRegistrationScreen(
                        viewModel = registrationViewModel,
                        onNavigateBack = { if (screenStack.size > 1) screenStack.removeAt(screenStack.size - 1) }
                    )
                }
                else -> {
                    // Fallback to splash if we are on a secure screen but currentUser is null
                    LaunchedEffect(Unit) {
                        screenStack.clear()
                        screenStack.add(Screen.Splash)
                    }
                }
            }
        }

        // WhatsApp Style In-App Floating Notification Banner
        activeInAppNotification?.let { banner ->
            InAppNotificationBanner(
                notification = banner,
                onDismiss = { activeInAppNotification = null },
                onClick = {
                    activeInAppNotification = null
                    banner.alertId?.let { id ->
                        if (screenStack.lastOrNull() !is Screen.AlertDetails || (screenStack.lastOrNull() as? Screen.AlertDetails)?.id != id) {
                            screenStack.add(Screen.AlertDetails(id))
                        }
                    }
                }
            )
        }
    }
}

data class InAppNotification(
    val title: String,
    val message: String,
    val type: String, // "ALERT", "COMMENT", "RESOLVED"
    val alertId: String? = null
)

fun sendSystemHeadsUpNotification(context: android.content.Context, title: String, message: String) {
    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "vams_realtime_alerts_v5"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            "VAMS Realtime Notifications",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = android.content.Intent(context, MainActivity::class.java).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = android.app.PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setSound(null)
        .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_VIBRATE or androidx.core.app.NotificationCompat.DEFAULT_LIGHTS)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

@Composable
fun InAppNotificationBanner(
    notification: InAppNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(notification) {
        kotlinx.coroutines.delay(4000)
        visible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .statusBarsPadding()
    ) {
        val icon = when (notification.type) {
            "ALERT" -> "⚠️"
            "COMMENT" -> "💬"
            "RESOLVED" -> "✅"
            else -> "🔔"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .border(1.dp, Brush.linearGradient(listOf(Accent, PrimaryBlue)), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notification.message,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
