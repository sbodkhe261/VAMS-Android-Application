package com.example.vamsapp.service

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.example.vamsapp.network.VamsPrefs

object SoundService {
    private const val TAG = "SoundService"
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun isRunningOnEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        return (brand.startsWith("generic") && device.startsWith("generic"))
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
    }

    fun init(context: Context) {
        // No-op, kept for signature compatibility
    }

    fun isDeviceSilenced(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    fun setAppMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopAllSounds()
        }
    }

    private fun playRawSound(context: Context, resId: Int) {
        if (isMuted) return
        try {
            stopAllSounds()
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId).apply {
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) {
                        mediaPlayer = null
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play raw sound via MediaPlayer", e)
        }
    }

    fun playLowSound(context: Context) {
        if (isMuted) return
        try {
            stopAllSounds()
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context.applicationContext, soundUri).apply {
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) {
                        mediaPlayer = null
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play default system sound", e)
        }
    }

    private fun generateCensorBeepWav(context: Context): java.io.File {
        val file = java.io.File(context.cacheDir, "censor_beep.wav")
        if (file.exists() && file.length() > 0) return file

        val sampleRate = 44100
        val duration = 2.0 // 2 seconds
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 1000.0 // 1000 Hz pure sine wave
            val value = (Math.sin(2.0 * Math.PI * freq * t) * 32767.0).toInt()
            buffer[i] = value.coerceIn(-32768, 32767).toShort()
        }

        val byteBuffer = ByteArray(44 + numSamples * 2)
        byteBuffer[0] = 'R'.toByte(); byteBuffer[1] = 'I'.toByte(); byteBuffer[2] = 'F'.toByte(); byteBuffer[3] = 'F'.toByte()
        val totalSize = 36 + numSamples * 2
        byteBuffer[4] = (totalSize and 0xff).toByte()
        byteBuffer[5] = ((totalSize shr 8) and 0xff).toByte()
        byteBuffer[6] = ((totalSize shr 16) and 0xff).toByte()
        byteBuffer[7] = ((totalSize shr 24) and 0xff).toByte()

        byteBuffer[8] = 'W'.toByte(); byteBuffer[9] = 'A'.toByte(); byteBuffer[10] = 'V'.toByte(); byteBuffer[11] = 'E'.toByte()

        byteBuffer[12] = 'f'.toByte(); byteBuffer[13] = 'm'.toByte(); byteBuffer[14] = 't'.toByte(); byteBuffer[15] = ' '.toByte()
        byteBuffer[16] = 16.toByte()
        byteBuffer[17] = 0; byteBuffer[18] = 0; byteBuffer[19] = 0
        byteBuffer[20] = 1.toByte()
        byteBuffer[21] = 0
        byteBuffer[22] = 1.toByte()
        byteBuffer[23] = 0

        byteBuffer[24] = (sampleRate and 0xff).toByte()
        byteBuffer[25] = ((sampleRate shr 8) and 0xff).toByte()
        byteBuffer[26] = ((sampleRate shr 16) and 0xff).toByte()
        byteBuffer[27] = ((sampleRate shr 24) and 0xff).toByte()

        val byteRate = sampleRate * 2
        byteBuffer[28] = (byteRate and 0xff).toByte()
        byteBuffer[29] = ((byteRate shr 8) and 0xff).toByte()
        byteBuffer[30] = ((byteRate shr 16) and 0xff).toByte()
        byteBuffer[31] = ((byteRate shr 24) and 0xff).toByte()

        byteBuffer[32] = 2.toByte()
        byteBuffer[33] = 0
        byteBuffer[34] = 16.toByte()
        byteBuffer[35] = 0

        byteBuffer[36] = 'd'.toByte(); byteBuffer[37] = 'a'.toByte(); byteBuffer[38] = 't'.toByte(); byteBuffer[39] = 'a'.toByte()
        val dataSize = numSamples * 2
        byteBuffer[40] = (dataSize and 0xff).toByte()
        byteBuffer[41] = ((dataSize shr 8) and 0xff).toByte()
        byteBuffer[42] = ((dataSize shr 16) and 0xff).toByte()
        byteBuffer[43] = ((dataSize shr 24) and 0xff).toByte()

        var byteIdx = 44
        for (i in 0 until numSamples) {
            val sample = buffer[i]
            byteBuffer[byteIdx++] = (sample.toInt() and 0xff).toByte()
            byteBuffer[byteIdx++] = ((sample.toInt() shr 8) and 0xff).toByte()
        }

        try {
            java.io.FileOutputStream(file).use { fos ->
                fos.write(byteBuffer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing censor beep wav file", e)
        }

        return file
    }

    private fun playCensorBeep(context: Context) {
        if (isMuted) return
        try {
            stopAllSounds()
            val file = generateCensorBeepWav(context)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) {
                        mediaPlayer = null
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play censor beep", e)
        }
    }

    private fun generateEmergencySirenWav(context: Context): java.io.File {
        val file = java.io.File(context.cacheDir, "emergency_siren_v2.wav")
        if (file.exists() && file.length() > 0) return file

        val sampleRate = 44100
        val duration = 3.0 // 3 seconds wailing loop
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Oscillate frequency smoothly between 800 Hz and 1600 Hz (wailing emergency siren)
            val phase = 2.0 * Math.PI * (800.0 * t - (800.0 / (2.0 * Math.PI * 2.5)) * Math.cos(2.0 * Math.PI * 2.5 * t))
            val value = (Math.sin(phase) * 32767.0).toInt()
            buffer[i] = value.coerceIn(-32768, 32767).toShort()
        }

        val byteBuffer = ByteArray(44 + numSamples * 2)
        byteBuffer[0] = 'R'.toByte(); byteBuffer[1] = 'I'.toByte(); byteBuffer[2] = 'F'.toByte(); byteBuffer[3] = 'F'.toByte()
        val totalSize = 36 + numSamples * 2
        byteBuffer[4] = (totalSize and 0xff).toByte()
        byteBuffer[5] = ((totalSize shr 8) and 0xff).toByte()
        byteBuffer[6] = ((totalSize shr 16) and 0xff).toByte()
        byteBuffer[7] = ((totalSize shr 24) and 0xff).toByte()

        byteBuffer[8] = 'W'.toByte(); byteBuffer[9] = 'A'.toByte(); byteBuffer[10] = 'V'.toByte(); byteBuffer[11] = 'E'.toByte()

        byteBuffer[12] = 'f'.toByte(); byteBuffer[13] = 'm'.toByte(); byteBuffer[14] = 't'.toByte(); byteBuffer[15] = ' '.toByte()
        byteBuffer[16] = 16.toByte()
        byteBuffer[17] = 0; byteBuffer[18] = 0; byteBuffer[19] = 0
        byteBuffer[20] = 1.toByte()
        byteBuffer[21] = 0
        byteBuffer[22] = 1.toByte()
        byteBuffer[23] = 0

        byteBuffer[24] = (sampleRate and 0xff).toByte()
        byteBuffer[25] = ((sampleRate shr 8) and 0xff).toByte()
        byteBuffer[26] = ((sampleRate shr 16) and 0xff).toByte()
        byteBuffer[27] = ((sampleRate shr 24) and 0xff).toByte()

        val byteRate = sampleRate * 2
        byteBuffer[28] = (byteRate and 0xff).toByte()
        byteBuffer[29] = ((byteRate shr 8) and 0xff).toByte()
        byteBuffer[30] = ((byteRate shr 16) and 0xff).toByte()
        byteBuffer[31] = ((byteRate shr 24) and 0xff).toByte()

        byteBuffer[32] = 2.toByte()
        byteBuffer[33] = 0
        byteBuffer[34] = 16.toByte()
        byteBuffer[35] = 0

        byteBuffer[36] = 'd'.toByte(); byteBuffer[37] = 'a'.toByte(); byteBuffer[38] = 't'.toByte(); byteBuffer[39] = 'a'.toByte()
        val dataSize = numSamples * 2
        byteBuffer[40] = (dataSize and 0xff).toByte()
        byteBuffer[41] = ((dataSize shr 8) and 0xff).toByte()
        byteBuffer[42] = ((dataSize shr 16) and 0xff).toByte()
        byteBuffer[43] = ((dataSize shr 24) and 0xff).toByte()

        var byteIdx = 44
        for (i in 0 until numSamples) {
            val sample = buffer[i]
            byteBuffer[byteIdx++] = (sample.toInt() and 0xff).toByte()
            byteBuffer[byteIdx++] = ((sample.toInt() shr 8) and 0xff).toByte()
        }

        try {
            java.io.FileOutputStream(file).use { fos ->
                fos.write(byteBuffer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing emergency siren wav file", e)
        }

        return file
    }

    fun playMediumSound(context: Context) {
        playRawSound(context, com.example.vamsapp.R.raw.beep)
    }

    fun playHighSound(context: Context) {
        playRawSound(context, com.example.vamsapp.R.raw.beep)
    }

    fun playCriticalSound(context: Context) {
        playRawSound(context, com.example.vamsapp.R.raw.siren)
    }

    fun playAlertSoundOnce(context: Context, alertId: String, severity: String?, bypassSeenCheck: Boolean = false) {
        if (!bypassSeenCheck && VamsPrefs.isAlertSeen(alertId)) return

        val lastBeep = VamsPrefs.getAlertLastBeepTime(alertId)
        val now = System.currentTimeMillis()
        if (now - lastBeep > 5000) { // 5-second de-duplication window
            VamsPrefs.setAlertLastBeepTime(alertId, now)
            when (severity?.uppercase() ?: "INFO") {
                "CRITICAL", "EMERGENCY" -> playCriticalSound(context)
                "HIGH", "MEDIUM", "RESOLVED", "REOPENED", "ASSIGNED" -> playHighSound(context)
                else -> playLowSound(context)
            }
        }
    }

    fun startContinuousSiren(context: Context, intervalMs: Long = 10000) {
        playCriticalSound(context)
    }

    fun stopAllSounds() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
    }
}
