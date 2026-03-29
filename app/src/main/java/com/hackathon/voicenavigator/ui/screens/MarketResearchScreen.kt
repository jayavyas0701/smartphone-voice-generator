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
import com.hackathon.voicenavigator.data.model.ESGIndicator
import com.hackathon.voicenavigator.ui.components.*
import com.hackathon.voicenavigator.ui.theme.*
import com.hackathon.voicenavigator.viewmodel.MarketResearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketResearchScreen(
    viewModel: MarketResearchViewModel,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gdpData by viewModel.gdpData.collectAsState()
    val co2Data by viewModel.co2Data.collectAsState()
    val agriLandData by viewModel.agriLandData.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatResponse by viewModel.chatResponse.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val dowStocks by viewModel.dowStocks.collectAsState()

    // Load GDP data and trigger AI description on first load
    LaunchedEffect(Unit) {
        viewModel.selectTab(ESGIndicator.GDP)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
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
                Text(
                    text = "Mobile App Voice Navigator",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "for Market Research",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ESG Indicator Tabs — FIX: removed clearChatResponse() which was cancelling the AI call
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ESGTabButton(
                text = "GDP",
                selected = selectedTab == ESGIndicator.GDP,
                onClick = { viewModel.selectTab(ESGIndicator.GDP) }
            )
            ESGTabButton(
                text = "CO2",
                selected = selectedTab == ESGIndicator.CO2,
                onClick = { viewModel.selectTab(ESGIndicator.CO2) }
            )
            ESGTabButton(
                text = "Agri. Land",
                selected = selectedTab == ESGIndicator.AGRI_LAND,
                onClick = { viewModel.selectTab(ESGIndicator.AGRI_LAND) }
            )
        }


        // Chart Area — FIX: show ChartLoading when data is null (not loaded yet)
        // instead of silently rendering nothing
        if (isLoading) {
            ChartLoading(modifier = Modifier.padding(16.dp))
        } else {
            val currentData = when (selectedTab) {
                ESGIndicator.GDP -> gdpData
                ESGIndicator.CO2 -> co2Data
                ESGIndicator.AGRI_LAND -> agriLandData
                else -> null
            }
            val chartColor = when (selectedTab) {
                ESGIndicator.GDP -> ChartBlue
                ESGIndicator.CO2 -> ChartRed
                ESGIndicator.AGRI_LAND -> ChartGreen
                else -> ChartBlue
            }

            if (currentData != null) {
                LineChart(
                    chartData = currentData,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineColor = chartColor
                )
            } else {
                // Data not yet loaded — show placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardResponseBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading chart data from World Bank...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // DOW Stocks Pie Chart
        if (dowStocks.isNotEmpty()) {
            PieChart(stocks = dowStocks, modifier = Modifier.padding(16.dp))
        }

        // AI Analysis Response
        chatResponse?.let { response ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardResponseBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AI Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CardResponseText,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }

        if (isChatLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating AI analysis...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium)
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
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CardInfoText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Voice Control Buttons (Listen, Read Aloud, Stop)
        VoiceControlButtonsMarket(
            isListening = isListening,
            isSpeaking = false,
            onStartListening = onStartListening,
            currentResponse = chatResponse
        )

        // API Info — FIX: updated CO2 URL to the working indicator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardInfoBackground),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "APIs",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardInfoText,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (selectedTab) {
                        ESGIndicator.GDP -> "https://api.worldbank.org/v2/country/WLD/indicator/NY.GDP.MKTP.KD.ZG?format=json"
                        ESGIndicator.CO2 -> "https://api.worldbank.org/v2/country/WLD/indicator/EN.ATM.CO2E.KT?format=json"
                        ESGIndicator.AGRI_LAND -> "https://api.worldbank.org/v2/country/WLD/indicator/AG.LND.AGRI.ZS?format=json"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ESGTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PrimaryBlue else Color.Transparent,
            contentColor = if (selected) Color.White else PrimaryBlue
        ),
        border = BorderStroke(1.5.dp, PrimaryBlue),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text(text = text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun VoiceControlButtonsMarket(
    isListening: Boolean,
    isSpeaking: Boolean,
    onStartListening: () -> Unit,
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
            onClick = { onStartListening() },
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
                onClick = { },
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
    }
}
