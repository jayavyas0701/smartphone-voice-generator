package com.hackathon.voicenavigator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackathon.voicenavigator.ui.theme.*

@Composable
fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isListening) 0f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer ripple ring when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = rippleAlpha))
            )
        }

        // Main button
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            containerColor = if (isListening) VoiceListening else VoiceActive,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isListening) 8.dp else 4.dp
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start listening",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Renders response text with the citation line as a clickable link.
 * Splits on the \uD83D\uDD17 emoji — text before it is normal, the citation becomes tappable.
 */
@Composable
fun LinkedResponseText(
    text: String,
    linkUrl: String,
    modifier: Modifier = Modifier
) {
    val linkMarker = "\uD83D\uDD17"
    if (text.contains(linkMarker)) {
        val parts = text.split(linkMarker, limit = 2)
        val bodyText = parts[0].trimEnd()
        val citationLabel = parts.getOrElse(1) { "" }.trim()
        val uriHandler = LocalUriHandler.current

        Column(modifier = modifier) {
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = CardResponseText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clickable { uriHandler.openUri(linkUrl) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "Source link",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = citationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = CardResponseText,
            modifier = modifier
        )
    }
}

@Composable
fun MiniVoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isListening) VoiceListening else VoiceActive)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = "Voice input",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
