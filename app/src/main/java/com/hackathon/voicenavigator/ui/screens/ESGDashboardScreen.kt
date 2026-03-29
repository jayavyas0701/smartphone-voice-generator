package com.hackathon.voicenavigator.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.voicenavigator.ui.components.*
import com.hackathon.voicenavigator.ui.theme.*
import com.hackathon.voicenavigator.viewmodel.ESGViewModel

@Composable
fun ESGDashboardScreen(
    viewModel: ESGViewModel,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit,
    onStopSpeech: () -> Unit,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ragResponse by viewModel.ragResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val initStatus by viewModel.initStatus.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val isSourceUpdated by viewModel.isSourceUpdated.collectAsState()
    val sourceLastModified by viewModel.sourceLastModified.collectAsState()

    // Auto-initialize RAG on first load
    LaunchedEffect(Unit) {
        viewModel.initializeRAG()
    }

    LaunchedEffect(Unit) {
        viewModel.initializeRAG()
        viewModel.checkSourceFreshness()   // ← ADD THIS LINE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {


        // Source Attribution + Freshness
        SourceInfoCard(
            sourceLabel = com.hackathon.voicenavigator.data.api.FreshnessChecker.ESG_SOURCE_LABEL,
            documentUrl = com.hackathon.voicenavigator.data.api.FreshnessChecker.SOFI_2024_URL,
            lastModified = sourceLastModified
        )
        UpdateBanner(
            isVisible = isSourceUpdated,
            documentUrl = com.hackathon.voicenavigator.data.api.FreshnessChecker.SOFI_2024_URL,
            message = "The SOFI report may have been updated — tap to view latest.",
            onDismiss = { viewModel.dismissUpdateBanner() }
        )
        // Header with FAO/WHO/UNICEF branding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FAO", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WHO", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UNICEF", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "The State of Food Security\nand Nutrition in the World",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RAG-Powered Analysis (2023-2025)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Quick Action Buttons - Prompt Buttons from Wireframe
        Text(
            "Quick Queries",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        val promptButtons = listOf(
            "List major Food insecurity reasons in 2024" to { viewModel.listFoodInsecurityReasons2024() },
            "Explain malnutrition in war zones" to { viewModel.explainMalnutritionInWarZones() },
            "Explain price impact on food security" to { viewModel.explainPriceImpact() },
            "Compare 2023 vs 2024 food insecurity" to { viewModel.compareFoodInsecurity2023vs2024() },
            "Quantitative differences in numbers" to { viewModel.explainQuantitativeDifferences() },
            "Economic Sustainability statements" to { viewModel.listEconomicSustainability() },
            "Social Sustainability statements" to { viewModel.listSocialSustainability() },
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            promptButtons.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (label, action) ->
                        ElevatedButton(
                            onClick = { action() },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = CardSuccessBackground,
                                contentColor = CardSuccessText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                            enabled = !isLoading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                            )
                        }
                    }
                    // Fill space if odd number of items
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // RAG Pipeline Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isInitializing) CardWarningBackground else CardSuccessBackground
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CardWarningText)
                } else {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = CardSuccessText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "RAG Vector Store: $initStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isInitializing) CardWarningText else CardSuccessText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Important Notice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardInfoBackground),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CardInfoText, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "No internet data. Only the data from reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardInfoText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Chat History
        if (chatHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            chatHistory.forEach { (question, answer) ->
                // User Question
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWarningBackground),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CardWarningText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = CardWarningText)
                    }
                }

                // AI Response
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardResponseBackground),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI Response", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinkedResponseText(
                            text = answer,
                            linkUrl = "https://www.fao.org/publications/sofi"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing food security data...", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Recognized Voice Text
        if (recognizedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = CardInfoBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = CardInfoText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("\"$recognizedText\"", style = MaterialTheme.typography.bodyMedium, color = CardInfoText, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Voice Control Buttons (Listen/Stop, Read Aloud, Stop Speech)
        VoiceControlButtonsESG(
            isListening = isListening,
            isSpeaking = isSpeaking,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onReadAloud = onReadAloud,
            onStopSpeech = onStopSpeech,
            currentResponse = ragResponse
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Data Sources
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWarningBackground),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Data Sources:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = CardWarningText)
                Spacer(modifier = Modifier.height(10.dp))
                Text("• FAO: State of Food Security 2024", style = MaterialTheme.typography.bodySmall, color = CardWarningText, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• FAO: State of Food Security 2025", style = MaterialTheme.typography.bodySmall, color = CardWarningText, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• UNICEF/WHO Joint Reports", style = MaterialTheme.typography.bodySmall, color = CardWarningText, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun VoiceControlButtonsESG(
    isListening: Boolean,
    isSpeaking: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit = {},
    onStopSpeech: () -> Unit = {},
    currentResponse: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 🎤 Listen / Stop Listening (toggle)
        ElevatedButton(
            onClick = { if (isListening) onStopListening() else onStartListening() },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (isListening) Color(0xFFE53935) else Color(0xFF43A047), // Red/Green
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
        ) {
            Text(if (isListening) "Stop Listening" else "🎤 Listen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }

        // 🔊 Read Aloud (only when response available and not speaking)
        if (!currentResponse.isNullOrBlank() && !isSpeaking) {
            ElevatedButton(
                onClick = { onReadAloud(currentResponse) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Text("Read Aloud", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        // 🔇 Stop (only when speaking)
        if (isSpeaking) {
            ElevatedButton(
                onClick = onStopSpeech,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFFE53935), // Red
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Text("Stop", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

