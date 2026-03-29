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
    val errorMessage by viewModel.errorMessage.collectAsState()
    val chatResponse by viewModel.chatResponse.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val dowStocks by viewModel.dowStocks.collectAsState()

    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadGDP()
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mobile App Voice Navigator",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "for Market Research",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ESG Indicator Tabs (GDP, CO2, Agri. Land)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ESGTabButton(
                text = "GDP",
                selected = selectedTab == ESGIndicator.GDP,
                onClick = {
                    viewModel.selectTab(ESGIndicator.GDP)
                    viewModel.clearChatResponse()
                }
            )
            ESGTabButton(
                text = "CO2",
                selected = selectedTab == ESGIndicator.CO2,
                onClick = {
                    viewModel.selectTab(ESGIndicator.CO2)
                    viewModel.clearChatResponse()
                }
            )
            ESGTabButton(
                text = "Agri. Land",
                selected = selectedTab == ESGIndicator.AGRI_LAND,
                onClick = {
                    viewModel.selectTab(ESGIndicator.AGRI_LAND)
                    viewModel.clearChatResponse()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart Area
        if (isLoading) {
            ChartLoading(modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Error: $errorMessage",
                    modifier = Modifier.padding(16.dp),
                    color = ErrorRed
                )
            }
        } else {
            when (selectedTab) {
                ESGIndicator.GDP -> gdpData?.let {
                    LineChart(chartData = it, modifier = Modifier.padding(horizontal = 16.dp), lineColor = ChartBlue)
                }
                ESGIndicator.CO2 -> co2Data?.let {
                    LineChart(chartData = it, modifier = Modifier.padding(horizontal = 16.dp), lineColor = ChartRed)
                }
                ESGIndicator.AGRI_LAND -> agriLandData?.let {
                    LineChart(chartData = it, modifier = Modifier.padding(horizontal = 16.dp), lineColor = ChartGreen)
                }
                else -> {}
            }
        }

        // DOW Stocks Pie Chart (if data available)
        if (dowStocks.isNotEmpty()) {
            PieChart(
                stocks = dowStocks,
                modifier = Modifier.padding(16.dp)
            )
        }

        // ChatGPT Response Area
        chatResponse?.let { response ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }

        if (isChatLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
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
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VoiceButton(
                isListening = isListening,
                onClick = onStartListening
            )
        }

        // API Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "APIs",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
                Text(
                    text = when (selectedTab) {
                        ESGIndicator.GDP -> "https://api.worldbank.org/v2/country/WLD/indicator/NY.GDP.MKTP.KD.ZG?format=json"
                        ESGIndicator.CO2 -> "https://api.worldbank.org/v2/country/WLD/indicator/EN.GHG.CO2.AG.MT.CE.AR5?format=json"
                        ESGIndicator.AGRI_LAND -> "https://api.worldbank.org/v2/country/WLD/indicator/AG.LND.AGRI.ZS?format=json"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Bottom nav space
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
        border = BorderStroke(1.dp, PrimaryBlue),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
