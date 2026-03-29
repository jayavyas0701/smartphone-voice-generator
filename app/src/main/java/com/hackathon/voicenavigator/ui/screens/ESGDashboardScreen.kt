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
    modifier: Modifier = Modifier
) {
    val ragResponse by viewModel.ragResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val initStatus by viewModel.initStatus.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()

    // Auto-initialize RAG on first load
    LaunchedEffect(Unit) {
        viewModel.initializeRAG()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with FAO/WHO/UNICEF branding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FAO", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WHO", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UNICEF", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The State of Food Security\nand Nutrition in the World",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RAG-Powered Analysis (2023-2025)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Quick Action Buttons - Prompt Buttons from Wireframe
        Text(
            "Quick Queries",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            promptButtons.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (label, action) ->
                        ElevatedButton(
                            onClick = { action() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2
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

        // RAG Pipeline Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isInitializing) AccentGold.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentOrange)
                } else {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "RAG Vector Store: $initStatus",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInitializing) AccentOrange else SuccessGreen
                )
            }
        }

        // Important Notice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "No internet data. Only the data from reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Chat History
        if (chatHistory.isNotEmpty()) {
            chatHistory.forEach { (question, answer) ->
                // User Question
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }

                // AI Response
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Response", style = MaterialTheme.typography.labelMedium, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyzing food security data...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // Recognized Voice Text
        if (recognizedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\"$recognizedText\"", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VoiceButton(isListening = isListening, onClick = onStartListening)
        }

        // Data Sources
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Data Sources:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• FAO: State of Food Security 2024", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                Text("• FAO: State of Food Security 2025", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                Text("• UNICEF/WHO Joint Reports", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
