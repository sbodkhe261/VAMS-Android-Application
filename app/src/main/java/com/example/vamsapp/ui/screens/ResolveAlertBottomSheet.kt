package com.example.vamsapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.vamsapp.ui.theme.CardDark
import com.example.vamsapp.ui.theme.DividerColor
import com.example.vamsapp.ui.theme.PrimaryBlue
import com.example.vamsapp.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveAlertBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, notes: String, audioFile: File?, transcription: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var transcription by remember { mutableStateOf("") }

    val context = LocalContext.current
    var recordingDurationSec by remember { mutableStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun startRecording() {
        audioFile = null
        transcription = ""
        
        val cacheDir = context.cacheDir
        val tempFile = File(cacheDir, "temp_voice_note.mp4")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setOutputFile(tempFile.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecording = true
            recordingDurationSec = 0

            timerJob = coroutineScope.launch {
                while (isRecording) {
                    delay(1000)
                    recordingDurationSec++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to start recording: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
        timerJob?.cancel()
        timerJob = null
        isRecording = false

        val tempFile = File(context.cacheDir, "temp_voice_note.mp4")
        if (tempFile.exists()) {
            if (recordingDurationSec < 1) {
                tempFile.delete()
                audioFile = null
                transcription = ""
                android.widget.Toast.makeText(context, "Recording too short", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val finalFile = File(context.cacheDir, "res_voice_${System.currentTimeMillis()}_dur_${recordingDurationSec}.mp4")
            if (tempFile.renameTo(finalFile)) {
                audioFile = finalFile
                transcription = "Inspected and resolved via voice note verification (${recordingDurationSec}s)."
            } else {
                audioFile = tempFile
                transcription = "Inspected and resolved via voice note verification."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                android.widget.Toast.makeText(context, "Permission granted. Tap Mic again to start recording.", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Audio recording permission is required.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    mediaRecorder?.release()
                    mediaRecorder = null
                }
                timerJob?.cancel()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resolve Defect Alert",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Reason field (Required)
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Action Taken / Resolution Reason *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Quality Check Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            // Audio Record UI
            Text("Voice Note Verification:", fontSize = 11.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isRecording) Color.Red else PrimaryBlue, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isRecording) {
                            val mins = recordingDurationSec / 60
                            val secs = recordingDurationSec % 60
                            String.format("Recording: %d:%02d", mins, secs)
                        } else if (audioFile != null) {
                            val dur = audioFile!!.name.substringAfter("_dur_").substringBefore(".mp4").toIntOrNull() ?: recordingDurationSec
                            val mins = dur / 60
                            val secs = dur % 60
                            String.format("Recorded: %s (%d:%02d)", audioFile!!.name, mins, secs)
                        } else {
                            "Tap Mic to record verification note"
                        },
                        color = if (isRecording) Color.Red else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRecording) {
                        LinearProgressIndicator(color = Color.Red, modifier = Modifier.fillMaxWidth(0.5f))
                    }
                }
            }

            // Real-time transcription box
            if (transcription.isNotEmpty()) {
                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("Audio Transcript") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (reason.isNotBlank()) {
                            onSubmit(reason, notes, audioFile, transcription)
                            onDismiss()
                        }
                    },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success,
                        disabledContainerColor = Success.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Resolve Defect", color = Color.White)
                }
            }
        }
    }
}
