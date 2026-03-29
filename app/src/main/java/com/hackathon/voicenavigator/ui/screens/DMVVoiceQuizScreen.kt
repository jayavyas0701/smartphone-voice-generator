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
import androidx.compose.material.icons.filled.Close
import com.hackathon.voicenavigator.viewmodel.DMVViewModel

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
    onCancel: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Large animated mic circle
        MicAnimation(
            isListening = isListening,
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
        )
        // Progress dots
        ProgressDots(
            progress = progress,
            total = total,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )
        // Subtitles: question/options
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            options.forEach {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            if (recognizedText.isNotEmpty()) {
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
        // Pause/cancel buttons
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onStopListening) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(32.dp))
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun MicAnimation(isListening: Boolean, modifier: Modifier = Modifier) {
    // Simple animated circle for mic state
    val color = if (isListening) Color(0xFF90CAF9) else Color(0xFF1E1E1E)
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

