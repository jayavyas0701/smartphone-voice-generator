package com.hackathon.voicenavigator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hackathon.voicenavigator.BuildConfig
import com.hackathon.voicenavigator.data.api.GeminiApiService
import com.hackathon.voicenavigator.ui.components.*
import com.hackathon.voicenavigator.ui.screens.*
import com.hackathon.voicenavigator.ui.theme.VoiceNavigatorTheme
import com.hackathon.voicenavigator.viewmodel.*
import com.hackathon.voicenavigator.voice.*

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var voiceManager: VoiceRecognitionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Permission denied - show a message
            Log.w(TAG, "RECORD_AUDIO permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize voice recognition
        voiceManager = VoiceRecognitionManager(this)
        voiceManager.initialize()

        // Set Gemini API key (embeddings + LLM)
        val geminiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (geminiKey.isNotEmpty() && geminiKey != "your-api-key-here") {
            GeminiApiService.setApiKey(geminiKey)
            Log.d(TAG, "✓ Gemini API key initialized (length: ${geminiKey.length})")
        } else {
            Log.e(TAG, "✗ CRITICAL: Gemini API key not set!")
        }

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            VoiceNavigatorTheme {
                MainApp(voiceManager = voiceManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(voiceManager: VoiceRecognitionManager) {
    val marketResearchVM: MarketResearchViewModel = viewModel()
    val dmvVM: DMVViewModel = viewModel()
    val esgVM: ESGViewModel = viewModel()

    var selectedNav by remember { mutableStateOf(BottomNavItem.API) }
    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()

    // TTS readback: speak AI responses aloud for voice-first UX
    val esgResponse by esgVM.ragResponse.collectAsState()
    val dmvResponse by dmvVM.ragResponse.collectAsState()
    val marketResponse by marketResearchVM.chatResponse.collectAsState()

    LaunchedEffect(esgResponse) {
        esgResponse?.let { if (it.length < 500) voiceManager.speak(it) else voiceManager.speak(it.take(400) + "... See the full response on screen.") }
    }
    LaunchedEffect(dmvResponse) {
        dmvResponse?.let { if (it.length < 500) voiceManager.speak(it) else voiceManager.speak(it.take(400) + "... See the full response on screen.") }
    }
    LaunchedEffect(marketResponse) {
        marketResponse?.let { if (it.length < 500) voiceManager.speak(it) else voiceManager.speak(it.take(400) + "... See the full response on screen.") }
    }

    // Handle voice commands
    fun handleVoiceResult(text: String) {
        val command = VoiceCommandParser.parse(text)
        when (command) {
            // Market Research Commands
            is VoiceCommandParser.VoiceCommand.ShowGDPGraph -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.selectTab(com.hackathon.voicenavigator.data.model.ESGIndicator.GDP)
            }
            is VoiceCommandParser.VoiceCommand.ShowCO2Graph -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.selectTab(com.hackathon.voicenavigator.data.model.ESGIndicator.CO2)
            }
            is VoiceCommandParser.VoiceCommand.ShowAgriLandGraph -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.selectTab(com.hackathon.voicenavigator.data.model.ESGIndicator.AGRI_LAND)
            }
            is VoiceCommandParser.VoiceCommand.DescribeCO2 -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.describeCO2Emissions()
            }
            is VoiceCommandParser.VoiceCommand.ShowDOWStocks -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.loadDOWStocks()
            }
            is VoiceCommandParser.VoiceCommand.ShowMilkPrice -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.queryMarketResearch(command.query)
            }
            is VoiceCommandParser.VoiceCommand.ShowGasPrice -> {
                selectedNav = BottomNavItem.API
                marketResearchVM.queryMarketResearch(command.query)
            }

            // Food Security Commands
            is VoiceCommandParser.VoiceCommand.MajorFoodSecurityIssues -> {
                selectedNav = BottomNavItem.ESG
                esgVM.listFoodInsecurityReasons2024()
            }
            is VoiceCommandParser.VoiceCommand.FoodSecurityQuery -> {
                selectedNav = BottomNavItem.ESG
                esgVM.queryFoodSecurity(command.query)
            }

            // DMV Commands
            is VoiceCommandParser.VoiceCommand.ShowSignalingSigns -> {
                selectedNav = BottomNavItem.DMV
                dmvVM.querySignalingSigns()
            }
            is VoiceCommandParser.VoiceCommand.ShowBACLimits -> {
                selectedNav = BottomNavItem.DMV
                dmvVM.queryBACLimits()
            }
            is VoiceCommandParser.VoiceCommand.DMVQuery -> {
                selectedNav = BottomNavItem.DMV
                dmvVM.queryDMVHandbook(command.query)
            }

            // Navigation
            is VoiceCommandParser.VoiceCommand.GoToMarketResearch -> selectedNav = BottomNavItem.API
            is VoiceCommandParser.VoiceCommand.GoToDMV -> selectedNav = BottomNavItem.DMV
            is VoiceCommandParser.VoiceCommand.GoToESG -> selectedNav = BottomNavItem.ESG
            is VoiceCommandParser.VoiceCommand.GoHome -> selectedNav = BottomNavItem.API

            // General
            is VoiceCommandParser.VoiceCommand.GeneralQuery -> {
                when (selectedNav) {
                    BottomNavItem.API -> marketResearchVM.queryMarketResearch(command.query)
                    BottomNavItem.ESG -> esgVM.queryFoodSecurity(command.query)
                    BottomNavItem.DMV -> dmvVM.queryDMVHandbook(command.query)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                selectedItem = selectedNav,
                onItemSelected = { selectedNav = it }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedNav) {
                BottomNavItem.API -> MarketResearchScreen(
                    viewModel = marketResearchVM,
                    isListening = isListening,
                    recognizedText = recognizedText,
                    onStartListening = {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening { text -> handleVoiceResult(text) }
                        }
                    }
                )
                BottomNavItem.ESG -> ESGDashboardScreen(
                    viewModel = esgVM,
                    isListening = isListening,
                    recognizedText = recognizedText,
                    onStartListening = {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening { text -> handleVoiceResult(text) }
                        }
                    }
                )
                BottomNavItem.DMV -> DMVScreen(
                    viewModel = dmvVM,
                    isListening = isListening,
                    recognizedText = recognizedText,
                    onStartListening = {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening { text -> handleVoiceResult(text) }
                        }
                    }
                )
            }
        }
    }
}
