package com.hackathon.voicenavigator.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hackathon.voicenavigator.ui.theme.*

/**
 * Source attribution card with tappable link to the official document.
 */
@Composable
fun SourceInfoCard(
    sourceLabel: String,
    documentUrl: String,
    lastModified: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
                    context.startActivity(intent)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (!lastModified.isNullOrEmpty()) {
                    Text(
                        "Last updated: $lastModified",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Text(
                    "Tap to view official source ↗",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue,
                    textDecoration = TextDecoration.Underline
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Open source",
                tint = PrimaryBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Update notification banner — shown when freshness check detects a newer version.
 */
@Composable
fun UpdateBanner(
    isVisible: Boolean,
    documentUrl: String,
    message: String = "The source document may have been updated.",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Update,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        "Tap to view latest version ↗",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
                            context.startActivity(intent)
                        }
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}