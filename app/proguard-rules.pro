# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the default Android SDK proguard file.

# Keep generic signatures and reflection attributes
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*

# Retrofit / Gson specific rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.example.vamsapp.model.** { *; }
-keep class com.example.vamsapp.network.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp & Okio rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Socket.IO & Engine.IO client rules
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class okhttp3.internal.ws.RealWebSocket { *; }

# Firebase Messaging & Google Services rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Security Crypto & Tink (used by EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
