package com.example.vamsapp.model

import com.google.gson.annotations.SerializedName

// --- Authentication ---
data class LoginRequest(
    val email: String,
    val password: String,
    val companyId: String? = null
)

data class LoginResponse(
    val accessToken: String,
    val user: User
)

data class RegisterUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val companyId: String
)

data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val companyId: String,
    val companyCode: String? = null,
    val companyName: String? = null
)

// --- Company & Settings ---
data class CreateCompanyRequest(
    val name: String
)

data class Company(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val settings: CompanySettings? = null
)

data class CompanySettings(
    val id: String,
    val companyId: String,
    val soundInfo: String,
    val soundWarning: String,
    val soundCritical: String,
    val soundEmergency: String,
    val escalationGraceMin: Int,
    val createdAt: String,
    val updatedAt: String
)

data class UpdateSettingsRequest(
    val soundInfo: String? = null,
    val soundWarning: String? = null,
    val soundCritical: String? = null,
    val soundEmergency: String? = null,
    val escalationGraceMin: Int? = null
)

// --- Defect Master ---
data class CreateDefectRequest(
    val name: String,
    val category: String,
    val severity: String,
    val defaultAssigneeRole: String? = null,
    val ownerVisible: Boolean? = null,
    val soundProfile: String? = null
)

data class DefectMaster(
    val id: String,
    val name: String,
    val category: String,
    val severity: String,
    val defaultAssigneeRole: String? = null,
    val ownerVisible: Boolean? = null,
    val soundProfile: String? = null,
    val active: Boolean,
    val companyId: String,
    val createdAt: String,
    val updatedAt: String
)

// --- Event Ingestion ---
data class IngestEventRequest(
    val source: String,
    val event_type: String,
    val companyId: String,
    val vin: String,
    val defectName: String
)

data class ResolutionUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class Resolution(
    val id: String,
    val alertId: String,
    val resolvedByUserId: String,
    val resolvedByUser: ResolutionUser? = null,
    val resolvedAt: String,
    val reason: String,
    val notes: String? = null,
    val audioPath: String? = null,
    val transcription: String? = null,
    val imageUrls: List<String>? = null
)

data class TimelineUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class TimelineEvent(
    val id: String,
    val alertId: String,
    val actionType: String,
    val performedByUserId: String? = null,
    val performedByRole: String? = null,
    val details: String,
    val createdAt: String,
    val performedByUser: TimelineUser? = null
)

data class AlertDefinition(
    val id: String,
    val companyId: String,
    val alertId: String,
    val name: String,
    val definition: String? = null,
    val type: String,
    val severity: String,
    val primaryAssigneeId: String,
    val escalationChain: List<String>,
    val escalationTimeout: Int,
    val criticalOverride: Boolean,
    val isActive: Boolean
)

// --- Alerts ---
data class Alert(
    val id: String,
    val vin: String? = null,
    val companyId: String,
    val defectId: String? = null,
    val defectName: String? = null,
    val severity: String,
    val status: String,
    val assignedToUserId: String? = null,
    val assignedToRole: String? = null,
    val assignedToUserName: String? = null,
    val assignedToDepartment: String? = null,
    val assignedToTeam: String? = null,
    val createdById: String? = null,
    val escalationStep: Int,
    val nextEscalationAt: String? = null,
    val isManual: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val definition: String? = null,
    val alertDefinition: AlertDefinition? = null,
    val defect: DefectMaster? = null,
    val resolution: Resolution? = null,
    val timeline: List<TimelineEvent>? = null
)

data class AssignAlertRequest(
    val assignedToUserId: String? = null,
    val assignedToRole: String? = null,
    val assignedToDepartment: String? = null,
    val assignedToTeam: String? = null,
    val notes: String? = null
)

data class ResolveAlertRequest(
    val reason: String,
    val notes: String? = null,
    val audioPath: String? = null,
    val transcription: String? = null,
    val imageUrls: List<String>? = null
)

data class ReopenAlertRequest(
    val reason: String
)

data class AddCommentRequest(
    val commentText: String,
    val audioPath: String? = null,
    val transcription: String? = null
)

data class Comment(
    val id: String,
    val alertId: String,
    val userId: String,
    val commentText: String,
    val audioPath: String? = null,
    val transcription: String? = null,
    val createdAt: String,
    val user: CommentUser
)

data class CommentUser(
    val id: String,
    val name: String,
    val role: String
)

// --- Telemetry Dashboard ---
data class DashboardTelemetry(
    val openAlertsCount: Int,
    val criticalAlertsCount: Int,
    val resolvedTodayCount: Int,
    val alertsBySeverity: Map<String, Int>,
    val alertsByCategory: Map<String, Int>
)

// --- Storage / Transcription ---
data class UploadMediaResponse(
    val fileUrl: String,
    val fileName: String
)

data class TranscriptionResponse(
    val fileId: String,
    val status: String,
    val transcription: String
)

data class UpdateDeviceTokenRequest(
    val token: String
)

data class LogoutRequest(
    val token: String?
)

data class ForgotPasswordRequest(
    val email: String,
    val companyId: String
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)
