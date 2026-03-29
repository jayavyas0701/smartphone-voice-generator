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
import androidx.compose.material3.ExperimentalMaterial3Api

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
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0=Handbook, 1=Quiz
    val ragResponse by viewModel.ragResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val quizScore by viewModel.quizScore.collectAsState()
    val questionIndex by viewModel.questionIndex.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showExplanation by viewModel.showExplanation.collectAsState()
    val initStatus by viewModel.initStatus.collectAsState()

    // Auto-initialize RAG
   // LaunchedEffect(Unit) { viewModel.initializeRAG() }

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

        // Top Bar with Logo/Trade/Country Flag
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {}, border = BorderStroke(1.dp, Color(0xFF666666))) {
                    Text("Logo", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Text("Trade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedButton(
                    onClick = {},
                    border = BorderStroke(1.5.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Text("Country Flag", style = MaterialTheme.typography.labelSmall)
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // RAG Pipeline Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RAG Vector Store: $initStatus", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

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
            1 -> DMVQuizSection(
                viewModel = viewModel,
                currentQuestion = currentQuestion,
                quizScore = quizScore,
                questionIndex = questionIndex,
                selectedAnswer = selectedAnswer,
                showExplanation = showExplanation
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
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VoiceButton(isListening = isListening, onClick = onStartListening)
    }

    // Source Info
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSuccessBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Source:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = CardSuccessText)
            Spacer(modifier = Modifier.height(8.dp))
            Text("California Driver's Handbook", style = MaterialTheme.typography.bodyMedium, color = CardSuccessText)
            Spacer(modifier = Modifier.height(4.dp))
            Text("https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/",
                style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DMVQuizSection(
    viewModel: DMVViewModel,
    currentQuestion: com.hackathon.voicenavigator.data.model.DMVQuestion?,
    quizScore: Int,
    questionIndex: Int,
    selectedAnswer: Int?,
    showExplanation: Boolean
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
                        onClick = { viewModel.nextQuestion() },
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
