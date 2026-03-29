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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.voicenavigator.ui.components.*
import com.hackathon.voicenavigator.ui.theme.*
import com.hackathon.voicenavigator.viewmodel.DMVViewModel
import com.hackathon.voicenavigator.viewmodel.QuizMode
import androidx.compose.material3.ExperimentalMaterial3Api
import com.hackathon.voicenavigator.ui.screens.DMVVoiceQuizScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMVScreen(
    viewModel: DMVViewModel,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit,
    onStopSpeech: () -> Unit,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0=Handbook, 1=Quiz
    var pendingQuizMode by remember { mutableStateOf<QuizMode?>(null) }
    var showModeSwitchDialog by remember { mutableStateOf(false) }
    val ragResponse by viewModel.ragResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val quizScore by viewModel.quizScore.collectAsState()
    val questionIndex by viewModel.questionIndex.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showExplanation by viewModel.showExplanation.collectAsState()
    val initStatus by viewModel.initStatus.collectAsState()
    val isSourceUpdated by viewModel.isSourceUpdated.collectAsState()
    val sourceLastModified by viewModel.sourceLastModified.collectAsState()
    val quizMode by viewModel.quizMode.collectAsState()

    // Auto-initialize RAG
   // LaunchedEffect(Unit) { viewModel.initializeRAG() }

    LaunchedEffect(Unit) { viewModel.checkSourceFreshness() }

    // Clear voice input when switching tabs - prevents voice persistence between Handbook and Quiz
    LaunchedEffect(selectedSection) {
        viewModel.clearVoiceInput()
        // Stop listening if active when switching tabs
        if (isListening) {
            onStopListening()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with DMV car icon
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DMVGreen),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "California DMV License",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Knowledge Test Preparation Navigator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }


        Spacer(modifier = Modifier.height(12.dp))

        // Section Tabs: Handbook / Quiz
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                label = { Text("📖 Handbook Q&A") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DMVGreen,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = {
                    selectedSection = 1
                    if (currentQuestion == null) viewModel.startQuiz()
                },
                label = { Text("📝 Practice Quiz") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DMVGreen,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quiz Mode Toggle (only show when Practice Quiz tab is selected)
        if (selectedSection == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedButton(
                    onClick = {
                        if (quizMode != QuizMode.VOICE) {
                            pendingQuizMode = QuizMode.VOICE
                            showModeSwitchDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (quizMode == QuizMode.VOICE) DMVGreen else Color(0xFF404040),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Voice Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }

                ElevatedButton(
                    onClick = {
                        if (quizMode != QuizMode.MANUAL) {
                            pendingQuizMode = QuizMode.MANUAL
                            showModeSwitchDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (quizMode == QuizMode.MANUAL) DMVGreen else Color(0xFF404040),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Source Attribution + Freshness
        SourceInfoCard(
            sourceLabel = com.hackathon.voicenavigator.data.api.FreshnessChecker.DMV_SOURCE_LABEL,
            documentUrl = com.hackathon.voicenavigator.data.api.FreshnessChecker.DMV_PDF_URL,
            lastModified = sourceLastModified
        )
        UpdateBanner(
            isVisible = isSourceUpdated,
            documentUrl = com.hackathon.voicenavigator.data.api.FreshnessChecker.DMV_PDF_URL,
            message = "The CA DMV Handbook may have been updated — tap to view latest.",
            onDismiss = { viewModel.dismissUpdateBanner() }
        )

        // RAG Pipeline Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        )

        {
            Row(
                modifier = Modifier.padding(12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RAG Vector Store: $initStatus", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        // Quiz Section
        when (selectedSection) {
            0 -> DMVHandbookSection(
                viewModel = viewModel,
                ragResponse = ragResponse,
                isLoading = isLoading,
                chatHistory = chatHistory,
                isListening = isListening,
                recognizedText = recognizedText,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onReadAloud = onReadAloud,
                onStopSpeech = onStopSpeech,
                isSpeaking = isSpeaking
            )
            1 -> {
                if (quizMode == QuizMode.VOICE) {
                    DMVVoiceQuizContainerScreen(
                        viewModel = viewModel,
                        isListening = isListening,
                        recognizedText = recognizedText,
                        onStartListening = onStartListening,
                        onStopListening = onStopListening,
                        onReadAloud = onReadAloud,
                        onSelectedSectionChange = { selectedSection = it }
                    )
                } else {
                    DMVQuizSection(
                        viewModel = viewModel,
                        currentQuestion = viewModel.currentQuestion.collectAsState().value,
                        quizScore = viewModel.quizScore.collectAsState().value,
                        questionIndex = viewModel.questionIndex.collectAsState().value,
                        selectedAnswer = viewModel.selectedAnswer.collectAsState().value,
                        showExplanation = viewModel.showExplanation.collectAsState().value,
                        isListening = isListening,
                        recognizedText = recognizedText,
                        onStartListening = onStartListening,
                        onStopListening = onStopListening,
                        onReadAloud = onReadAloud,
                        onStopSpeech = onStopSpeech,
                        isSpeaking = isSpeaking,
                        quizMode = quizMode
                    )
                }
            }
        }

        // Mode Switch Confirmation Dialog
        if (showModeSwitchDialog && pendingQuizMode != null) {
            AlertDialog(
                onDismissRequest = {}, // Modal: cannot dismiss by tapping outside
                title = { Text("Switch Quiz Mode?") },
                text = { Text("Switching modes will restart the quiz and all progress will be lost. Are you sure you want to switch to ${pendingQuizMode!!.name.lowercase().replaceFirstChar { it.uppercase() }} Mode?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setQuizMode(pendingQuizMode!!)
                        viewModel.startQuiz()
                        showModeSwitchDialog = false
                        pendingQuizMode = null
                    }) {
                        Text("Restart Quiz in ${pendingQuizMode!!.name.lowercase().replaceFirstChar { it.uppercase() }} Mode")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showModeSwitchDialog = false
                        pendingQuizMode = null
                    }) {
                        Text("Cancel")
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}



@Composable
private fun DMVHandbookSection(
    viewModel: DMVViewModel,
    ragResponse: String?,
    isLoading: Boolean,
    chatHistory: List<Pair<String, String>>,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit,
    onStopSpeech: () -> Unit,
    isSpeaking: Boolean
) {
    // Quick DMV Query Buttons
    Text(
        "Quick Queries",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ElevatedButton(
            onClick = { viewModel.querySignalingSigns() },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CardSuccessBackground,
                contentColor = CardSuccessText
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text("Signaling Signs", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
        ElevatedButton(
            onClick = { viewModel.queryBACLimits() },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CardSuccessBackground,
                contentColor = CardSuccessText
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text("BAC Limits", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ElevatedButton(
            onClick = { viewModel.queryDMVHandbook("What are the California speed limits for different areas?") },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CardSuccessBackground,
                contentColor = CardSuccessText
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text("Speed Limits", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
        ElevatedButton(
            onClick = { viewModel.queryDMVHandbook("What are the right-of-way rules in California?") },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CardSuccessBackground,
                contentColor = CardSuccessText
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text("Right-of-Way", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }

    // Voice Control Buttons
    VoiceControlButtons(
        isListening = isListening,
        isSpeaking = isSpeaking,
        onStartListening = onStartListening,
        onStopListening = onStopListening,
        onReadAloud = onReadAloud,
        onStopSpeech = onStopSpeech,
        currentResponse = ragResponse
    )

    // Chat History
    chatHistory.forEach { (question, answer) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = CardWarningBackground),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Person, contentDescription = null, tint = CardWarningText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = CardWarningText)
            }
        }

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
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("DMV Assistant", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinkedResponseText(
                    text = answer,
                    linkUrl = "https://www.dmv.ca.gov/portal/handbook/california-driver-handbook/"
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp), 
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = DMVGreen, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching handbook...", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }

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

    Spacer(modifier = Modifier.height(16.dp))

}



@Composable
private fun DMVQuizSection(
    viewModel: DMVViewModel,
    currentQuestion: com.hackathon.voicenavigator.data.model.DMVQuestion?,
    quizScore: Int,
    questionIndex: Int,
    selectedAnswer: Int?,
    showExplanation: Boolean,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit,
    onStopSpeech: () -> Unit,
    isSpeaking: Boolean,
    quizMode: QuizMode = QuizMode.MANUAL
) {
    if (currentQuestion == null) {
        // Quiz Complete or Not Started
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (questionIndex > 0) {
                    Icon(
                        if (quizScore >= viewModel.getTotalQuestions() * 0.7) Icons.Default.EmojiEvents else Icons.Default.School,
                        contentDescription = null,
                        tint = if (quizScore >= viewModel.getTotalQuestions() * 0.7) AccentGold else DMVGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quiz Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Score: $quizScore / ${viewModel.getTotalQuestions()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (quizScore >= viewModel.getTotalQuestions() * 0.7) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${(quizScore.toDouble() / viewModel.getTotalQuestions() * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (quizScore >= viewModel.getTotalQuestions() * 0.7) SuccessGreen else ErrorRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (quizScore >= viewModel.getTotalQuestions() * 0.7) "Great job! You passed!" else "Keep studying. You need 70% to pass.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DMV Practice Quiz", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${viewModel.getTotalQuestions()} questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.startQuiz() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DMVGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (questionIndex > 0) "Retry Quiz" else "Start Quiz", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    } else {
        // Question Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Question ${questionIndex + 1}/${viewModel.getTotalQuestions()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = DMVGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Score: $quizScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = (questionIndex + 1f) / viewModel.getTotalQuestions(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = DMVGreen,
                    trackColor = Color(0xFF404040)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Badge
                SuggestionChip(
                    onClick = {},
                    label = { Text(currentQuestion.category, style = MaterialTheme.typography.labelSmall, color = PrimaryBlue) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.15f),
                        labelColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Question
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Voice Controls for Question - Only show in Voice Mode
                if (!showExplanation && quizMode == QuizMode.VOICE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = { onReadAloud(currentQuestion.question) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Question", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }

                        ElevatedButton(
                            onClick = { onStartListening() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (isListening) ErrorRed else Color(0xFF43A047),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isListening) "Stop" else "Answer", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show recognized voice answer
                    if (recognizedText.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = CardInfoBackground),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = CardInfoText, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Your answer:", style = MaterialTheme.typography.labelSmall, color = CardInfoText, fontWeight = FontWeight.Medium)
                                    Text("\"$recognizedText\"", style = MaterialTheme.typography.bodySmall, color = CardInfoText)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Auto-detect and auto-select answer in Voice Mode
                        LaunchedEffect(recognizedText) {
                            val detectedAnswer = parseVoiceAnswer(recognizedText)
                            if (detectedAnswer != null && !isListening && selectedAnswer == null) {
                                // Auto-select the detected answer
                                viewModel.selectAnswer(detectedAnswer)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val isCorrect = index == currentQuestion.correctAnswer
                    val bgColor = when {
                        !showExplanation -> if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color.Transparent
                        isCorrect -> SuccessGreen.copy(alpha = 0.18f)
                        isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.18f)
                        else -> Color.Transparent
                    }
                    val borderColor = when {
                        !showExplanation -> if (isSelected) PrimaryBlue else Color(0xFF555555)
                        isCorrect -> SuccessGreen
                        isSelected && !isCorrect -> ErrorRed
                        else -> Color(0xFF555555)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !showExplanation) { viewModel.selectAnswer(index) },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${'A' + index})",
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(text = option, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            if (showExplanation && isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            }
                            if (showExplanation && isSelected && !isCorrect) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Explanation
                if (showExplanation) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Explanation:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(currentQuestion.explanation, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { 
                            viewModel.nextQuestion()
                            // Clear recognized text when moving to next question in Voice Mode
                            if (quizMode == QuizMode.VOICE) {
                                onStopListening()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DMVGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (questionIndex + 1 < viewModel.getTotalQuestions()) "Next Question" else "See Results", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Parse voice input to detect spoken answer (A, B, C, or D)
 */
fun parseVoiceAnswer(text: String): Int? {
    val normalized = text.trim().lowercase()
    return when {
        normalized.contains("a") || normalized == "a" -> 0
        normalized.contains("b") || normalized == "b" -> 1
        normalized.contains("c") || normalized == "c" -> 2
        normalized.contains("d") || normalized == "d" -> 3
        else -> null
    }
}

@Composable
private fun VoiceControlButtons(
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start/Stop Listening Toggle
        ElevatedButton(
            onClick = {
                if (isListening) onStopListening() else onStartListening()
            },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (isListening) ErrorRed else DMVGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isListening) "Stop Listening" else "🎤 Listen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }

        // Read Aloud Button (only show when response available and not speaking)
        if (!currentResponse.isNullOrBlank() && !isSpeaking) {
            ElevatedButton(
                onClick = { onReadAloud(currentResponse!!) },
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
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read Aloud", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        // Stop Speech Button (show when speaking)
        if (isSpeaking) {
            ElevatedButton(
                onClick = onStopSpeech,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DMVVoiceQuizFinalScoreScreen(
    score: Int,
    total: Int,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Quiz Complete!",
                style = MaterialTheme.typography.displaySmall,
                color = DMVGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Your Score",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "$score / $total",
                style = MaterialTheme.typography.displayLarge,
                color = DMVGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            ElevatedButton(
                onClick = onRestart,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = DMVGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text("Restart Quiz", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DMVVoiceQuizContainerScreen(
    viewModel: DMVViewModel,
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReadAloud: (String) -> Unit,
    onSelectedSectionChange: (Int) -> Unit
) {
    val currentQuestion = viewModel.currentQuestion.collectAsState().value
    val quizScore = viewModel.quizScore.collectAsState().value
    val questionIndex = viewModel.questionIndex.collectAsState().value
    val showExplanation = viewModel.showExplanation.collectAsState().value
    val feedback = if (showExplanation) {
        val selected = viewModel.selectedAnswer.collectAsState().value
        if (selected == currentQuestion?.correctAnswer) "Correct!" else "Incorrect"
    } else null

    // If quiz is complete (no more questions), show final score screen
    if (currentQuestion == null) {
        DMVVoiceQuizFinalScoreScreen(
            score = quizScore,
            total = 10,
            onRestart = { viewModel.startQuiz() }
        )
    } else {
        // Monitor recognized text and match to options
        LaunchedEffect(recognizedText, currentQuestion, showExplanation) {
            if (recognizedText.isNotEmpty() && !showExplanation && currentQuestion != null) {
                val matchedIndex = currentQuestion.options.indexOfFirst { option ->
                    option.contains(recognizedText, ignoreCase = true) ||
                    recognizedText.contains(option, ignoreCase = true)
                }
                if (matchedIndex != -1) {
                    viewModel.selectAnswer(matchedIndex)
                }
            }
        }

        DMVVoiceQuizScreen(
            question = currentQuestion.question,
            options = currentQuestion.options,
            progress = questionIndex + 1,
            total = viewModel.getTotalQuestions(),
            isListening = isListening,
            recognizedText = recognizedText,
            feedback = feedback,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onReadAloud = onReadAloud,
            onNextQuestion = { viewModel.nextQuestion() },
            onCancel = {
                // Cancel quiz, reset to practice quiz tab
                viewModel.startQuiz()
                onSelectedSectionChange(1)
            }
        )
    }
}

