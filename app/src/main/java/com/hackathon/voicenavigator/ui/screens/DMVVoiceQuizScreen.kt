package com.hackathon.voicenavigator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import kotlinx.coroutines.delay

@Composable
fun DMVVoiceQuizScreen(
    question: String,
    options: List<String>,
    progress: Int,
    total: Int,
    isListening: Boolean,
    recognizedText: String,
    feedback: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onReadAloud: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onNextQuestion: () -> Unit
) {
    // Track what's being spoken via TTS
    var isSpeaking by remember { mutableStateOf(false) }
    var spokenText by remember { mutableStateOf("") }
    var ttsCompleted by remember { mutableStateOf(false) }

    // Speak question + options when first loaded or when question changes
    LaunchedEffect(question) {
        if (question.isNotEmpty()) {
            val textToSpeak = "$question. Options: ${options.joinToString(", ")}"
            isSpeaking = true
            spokenText = textToSpeak
            onReadAloud(textToSpeak)
            // Wait for TTS to finish (~2-3 seconds for average question)
            delay(2500)
            isSpeaking = false
            ttsCompleted = true
        }
    }

    // Auto-advance after feedback for 2 seconds
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(2000)
            onNextQuestion()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Layout in 3 zones: top (progress), center (mic + subtitles), bottom (buttons)
        Column(Modifier.fillMaxSize()) {
            // Top: Progress dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ProgressDots(
                    progress = progress,
                    total = total
                )
            }

            // Center: Animated mic circle + Subtitles
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large animated mic circle
                MicAnimation(
                    isListening = isListening || isSpeaking,
                    isSpeaking = isSpeaking,
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Subtitles: question/options/recognized/feedback
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show TTS output when speaking
                    if (isSpeaking && spokenText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speaking",
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = spokenText.take(100), // Show first 100 chars
                                color = Color(0xFF90CAF9),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = question,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    options.forEach { option ->
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (recognizedText.isNotEmpty() && !isSpeaking && isListening) {
                        Text(
                            text = recognizedText,
                            color = Color(0xFFFFEB3B), // Yellow for recognized speech
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    if (feedback != null) {
                        val feedbackColor = if (feedback == "Correct!") Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(
                            text = feedback,
                            color = feedbackColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            // Bottom: Play/Pause + Reset buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button with clear state indication
                IconButton(
                    onClick = {
                        when {
                            isListening -> onStopListening()
                            isSpeaking -> {
                                // Stop TTS, mark as completed, and start listening
                                onStopSpeech()
                                isSpeaking = false
                                ttsCompleted = true
                                onStartListening()
                            }
                            else -> onStartListening()
                        }
                    },
                    enabled = feedback == null // Enable except during feedback
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    feedback != null -> Color.Gray.copy(alpha = 0.3f)
                                    isSpeaking -> Color(0xFF2196F3).copy(alpha = 0.3f) // Blue tint for TTS
                                    isListening -> Color(0xFFFF9800).copy(alpha = 0.3f) // Orange tint for listening
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSpeaking -> Icons.Default.VolumeUp // Speaker icon when TTS
                                isListening -> Icons.Default.Pause // Pause icon when listening
                                else -> Icons.Default.PlayArrow // Play icon when idle
                            },
                            contentDescription = when {
                                isSpeaking -> "Stop and listen to answer..."
                                isListening -> "Listening for answer..."
                                else -> "Play to hear question"
                            },
                            tint = when {
                                feedback != null -> Color.Gray
                                isSpeaking -> Color(0xFF2196F3)
                                isListening -> Color(0xFFFF9800)
                                else -> Color.White
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(48.dp))

                // Reset button (replaced X)
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset quiz",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Status indicator text (for clarity)
            Text(
                text = when {
                    isSpeaking -> "🔊 Listening to question..."
                    isListening -> "🎤 Listening to your answer..."
                    ttsCompleted && feedback == null -> "Press ▶️ to begin answering"
                    feedback != null -> ""
                    else -> ""
                },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun MicAnimation(
    isListening: Boolean,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Animated circle for mic state: Blue = Listening, Cyan = Speaking, Gray = Idle
    val color = when {
        isSpeaking -> Color(0xFF00BCD4) // Cyan for TTS/speaking
        isListening -> Color(0xFF90CAF9) // Light blue for listening
        else -> Color(0xFF1E1E1E) // Dark gray for idle
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(color)
            .size(120.dp)
    )
}

@Composable
fun ProgressDots(progress: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier) {
        repeat(total) { i ->
            val dotColor = if (i < progress) Color(0xFF90CAF9) else Color(0xFF1E1E1E)
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
