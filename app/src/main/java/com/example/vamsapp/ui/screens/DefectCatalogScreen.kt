package com.example.vamsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vamsapp.model.CreateDefectRequest
import com.example.vamsapp.model.DefectMaster
import com.example.vamsapp.model.User
import com.example.vamsapp.ui.components.*
import com.example.vamsapp.ui.theme.*
import com.example.vamsapp.viewmodel.DefectCatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefectCatalogScreen(
    user: User,
    viewModel: DefectCatalogViewModel,
    onNavigateBack: () -> Unit
) {
    val defects by viewModel.defects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()

    var showCreateScreen by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showCreateScreen) {
        showCreateScreen = false
    }

    LaunchedEffect(Unit) {
        viewModel.fetchDefects()
    }

    LaunchedEffect(actionSuccess) {
        if (actionSuccess) {
            showCreateScreen = false
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showCreateScreen) "Add Defect definition" else "Defect Catalog", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showCreateScreen) {
                            showCreateScreen = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // COMPANY_ADMIN or MANAGER FAB or Action Button to open creation
                    if ((user.role == "COMPANY_ADMIN" || user.role == "MANAGER" || user.role == "FACTORY_MANAGER") && !showCreateScreen) {
                        IconButton(onClick = { showCreateScreen = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Defect", tint = Accent)
                        }
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
            AnimatedContent(
                targetState = showCreateScreen,
                transitionSpec = {
                    slideInVertically(animationSpec = tween(300)) { it } + fadeIn() togetherWith
                            slideOutVertically(animationSpec = tween(250)) { it } + fadeOut()
                },
                label = "defect_form_transition"
            ) { isAdding ->
                if (isAdding) {
                    AddDefectForm(onSubmit = { name, cat, sev, role, visible, sound ->
                        viewModel.addDefect(name, cat, sev, role, visible, sound)
                    }, error = error, isLoading = isLoading)
                } else {
                    DefectsList(
                        defects = defects,
                        user = user,
                        isLoading = isLoading,
                        onDeactivate = { id -> viewModel.deactivateDefect(id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DefectsList(
    defects: List<DefectMaster>,
    user: User,
    isLoading: Boolean,
    onDeactivate: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else if (defects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No defects registered.", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(defects) { defect ->
                DefectMasterItemCard(defect = defect, user = user, onDeactivate = onDeactivate)
            }
        }
    }
}

@Composable
fun DefectMasterItemCard(
    defect: DefectMaster,
    user: User,
    onDeactivate: (String) -> Unit
) {
    val stripeColor = when (defect.severity.uppercase()) {
        "CRITICAL" -> SeverityCritical
        "HIGH" -> SeverityHigh
        "MEDIUM" -> SeverityMedium
        else -> SeverityLow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = defect.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = stripeColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = defect.severity,
                            color = stripeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Category: ${defect.category} | Default Assignee: ${defect.defaultAssigneeRole ?: "WORKER"}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Sound Profile: ${defect.soundProfile ?: "NORMAL"} | Owner Visible: ${defect.ownerVisible ?: true}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // Deactivate option for COMPANY_ADMIN or MANAGER
                if ((user.role == "COMPANY_ADMIN" || user.role == "MANAGER" || user.role == "FACTORY_MANAGER") && defect.active) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deactivate",
                        color = SeverityCritical,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDeactivate(defect.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDefectForm(
    onSubmit: (name: String, category: String, severity: String, role: String, visible: Boolean, sound: String) -> Unit,
    error: String?,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("HIGH") }
    var defaultRole by remember { mutableStateOf("WORKER") }
    var ownerVisible by remember { mutableStateOf(true) }
    var soundProfile by remember { mutableStateOf("ALERT") }

    var expandedRole by remember { mutableStateOf(false) }
    var expandedSound by remember { mutableStateOf(false) }

    val roles = listOf("COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "SERVICE_ENGINEER", "WORKER", "QUALITY_INSPECTOR", "DEALER", "VEHICLE_OWNER", "READ_ONLY_USER")
    val sounds = listOf("NORMAL", "ALERT", "CRITICAL")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Defect Name *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category *") },
            modifier = Modifier.fillMaxWidth()
        )

        // Severity Selector
        Text("Severity", fontSize = 12.sp, color = TextSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { s ->
                val active = severity == s
                val activeColor = when (s) {
                    "CRITICAL" -> SeverityCritical
                    "HIGH" -> SeverityHigh
                    "MEDIUM" -> SeverityMedium
                    else -> SeverityLow
                }
                CustomFilterChip(
                    selected = active,
                    onClick = { severity = s },
                    label = s,
                    activeColor = activeColor
                )
            }
        }

        // Default Assignee Role dropdown
        Text("Default Assignee Role", fontSize = 12.sp, color = TextSecondary)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = defaultRole,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole) },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expandedRole = !expandedRole }
            )
            DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                roles.forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = { defaultRole = r; expandedRole = false })
                }
            }
        }

        // Owner Visible Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Owner Visible to Factory Floor", color = Color.White, fontSize = 14.sp)
            Switch(checked = ownerVisible, onCheckedChange = { ownerVisible = it })
        }

        // Sound Profile Dropdown
        Text("Sound Notification Profile", fontSize = 12.sp, color = TextSecondary)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = soundProfile,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSound) },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expandedSound = !expandedSound }
            )
            DropdownMenu(expanded = expandedSound, onDismissRequest = { expandedSound = false }) {
                sounds.forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { soundProfile = s; expandedSound = false })
                }
            }
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSubmit(name, category, severity, defaultRole, ownerVisible, soundProfile) },
            enabled = !isLoading && name.isNotBlank() && category.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text("Register Defect", color = Color.White)
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
