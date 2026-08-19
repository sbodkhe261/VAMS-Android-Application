package com.example.vamsapp.network

import com.example.vamsapp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface VamsApiService {

    // --- Authentication ---
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("auth/register")
    fun register(@Body request: RegisterUserRequest): Call<User>

    @POST("auth/device-token")
    fun updateDeviceToken(@Body request: UpdateDeviceTokenRequest): Call<Void>

    @POST("auth/logout")
    fun logout(@Body request: LogoutRequest): Call<Void>

    // --- Companies & Settings ---
    @POST("companies")
    fun createCompany(@Body request: CreateCompanyRequest): Call<Company>

    @GET("companies/{id}")
    fun getCompany(@Path("id") id: String): Call<Company>

    @GET("companies/{companyId}/users")
    fun getCompanyUsers(@Path("companyId") companyId: String): Call<List<User>>

    @GET("companies/settings")
    fun getSettings(): Call<CompanySettings>

    @PATCH("companies/settings")
    fun updateSettings(@Body request: UpdateSettingsRequest): Call<CompanySettings>

    // --- Defect Master Catalog ---
    @POST("defects")
    fun createDefect(@Body request: CreateDefectRequest): Call<DefectMaster>

    @GET("defects")
    fun getDefects(): Call<List<DefectMaster>>

    @DELETE("defects/{id}")
    fun deleteDefect(@Path("id") id: String): Call<DefectMaster>

    // --- Ingestion Event Webhook ---
    @POST("alerts/event")
    fun ingestEvent(@Body request: IngestEventRequest): Call<Alert>

    // --- Alerts Engine Lifecycle ---
    @GET("alerts")
    fun getAlerts(
        @Query("status") status: String? = null,
        @Query("severity") severity: String? = null,
        @Query("assignedToUserId") assignedToUserId: String? = null,
        @Query("assignedToRole") assignedToRole: String? = null
    ): Call<List<Alert>>

    @GET("alerts/dashboard")
    fun getDashboard(): Call<DashboardTelemetry>

    @GET("alerts/{id}")
    fun getAlertDetails(@Path("id") id: String): Call<Alert>

    @PATCH("alerts/{id}/assign")
    fun reassignAlert(
        @Path("id") id: String,
        @Body request: AssignAlertRequest
    ): Call<Alert>

    @POST("alerts/{id}/resolve")
    fun resolveAlert(
        @Path("id") id: String,
        @Body request: ResolveAlertRequest
    ): Call<Alert>

    @POST("alerts/{id}/reopen")
    fun reopenAlert(
        @Path("id") id: String,
        @Body request: ReopenAlertRequest
    ): Call<Alert>

    @POST("alerts/{id}/comments")
    fun addComment(
        @Path("id") id: String,
        @Body request: AddCommentRequest
    ): Call<Comment>

    // --- Storage & Media ---
    @Multipart
    @POST("media/upload")
    fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("purpose") purpose: RequestBody
    ): Call<UploadMediaResponse>

    @GET("media/transcription/{fileId}")
    fun getTranscription(@Path("fileId") fileId: String): Call<TranscriptionResponse>
}
