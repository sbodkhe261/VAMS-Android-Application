package com.example.vamsapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://192.168.156.135:3000/api/v1/"
    private var currentBaseUrl = BASE_URL

    var authToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun configureCertificatePinner(hostname: String, vararg pins: String) {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (pins.isNotEmpty()) {
            val pinner = okhttp3.CertificatePinner.Builder()
            for (pin in pins) {
                pinner.add(hostname, pin)
            }
            builder.certificatePinner(pinner.build())
        }

        val client = builder.build()
        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(VamsApiService::class.java)
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private var retrofit = createRetrofit(currentBaseUrl)

    var apiService: VamsApiService = retrofit.create(VamsApiService::class.java)
        private set

    fun setBaseUrl(url: String) {
        if (currentBaseUrl != url) {
            currentBaseUrl = url
            retrofit = createRetrofit(url)
            apiService = retrofit.create(VamsApiService::class.java)
        }
    }

    fun getAbsoluteUrl(path: String?): String? {
        if (path == null) return null
        if (path.startsWith("http://") || path.startsWith("https://")) {
            if (path.contains("s3.vams-platform.com/uploads/")) {
                val serverBase = currentBaseUrl.substringBefore("/api/v1/")
                return path.replace("https://s3.vams-platform.com/uploads/", "$serverBase/uploads/")
            }
            return path
        }
        val serverBase = currentBaseUrl.substringBefore("/api/v1/")
        return "$serverBase/$path"
    }
}