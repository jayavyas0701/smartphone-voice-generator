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
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "California DMV License",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Knowledge Test Preparation Navigator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Top Bar with Logo/Trade/Country Flag
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {}, border = BorderStroke(1.dp, Color.Gray)) {
                    Text("Logo", color = TextSecondary)
                }
                Text("Trade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = {},
                    border = BorderStroke(1.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Text("Country Flag", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        // RAG Pipeline Status
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RAG Vector Store: $initStatus", style = MaterialTheme.typography.bodySmall, color = DMVGreen)
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
                onStartListening = onStartListening
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
    onStartListening: () -> Unit
) {
    // Quick DMV Query Buttons
    Text(
        "Quick Queries",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedButton(
            onClick = { viewModel.querySignalingSigns() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = DMVGreen
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            Text("Signaling Signs", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        ElevatedButton(
            onClick = { viewModel.queryBACLimits() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = DMVGreen
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            Text("BAC Limits", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedButton(
            onClick = { viewModel.queryDMVHandbook("What are the California speed limits for different areas?") },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = DMVGreen
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            Text("Speed Limits", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        ElevatedButton(
            onClick = { viewModel.queryDMVHandbook("What are the right-of-way rules in California?") },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = DMVGreen
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            Text("Right-of-Way", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }

    // Chat History
    chatHistory.forEach { (question, answer) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DMV Assistant", style = MaterialTheme.typography.labelMedium, color = DMVGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = answer, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = DMVGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Searching handbook...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }

    if (recognizedText.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("\"$recognizedText\"", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VoiceButton(isListening = isListening, onClick = onStartListening)
    }

    // Source Info
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Source:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = DMVGreen)
            Text("California Driver's Handbook", style = MaterialTheme.typography.bodySmall, color = DMVGreen)
            Text("https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/",
                style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (questionIndex > 0) {
                    Icon(
                        if (quizScore >= viewModel.getTotalQuestions() * 0.7) Icons.Default.EmojiEvents else Icons.Default.School,
                        contentDescription = null,
                        tint = if (quizScore >= viewModel.getTotalQuestions() * 0.7) AccentGold else DMVGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quiz Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Score: $quizScore / ${viewModel.getTotalQuestions()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (quizScore >= viewModel.getTotalQuestions() * 0.7) SuccessGreen else ErrorRed
                    )
                    Text(
                        "${(quizScore.toDouble() / viewModel.getTotalQuestions() * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (quizScore >= viewModel.getTotalQuestions() * 0.7) SuccessGreen else ErrorRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (quizScore >= viewModel.getTotalQuestions() * 0.7) "Great job! You passed!" else "Keep studying. You need 70% to pass.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = DMVGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DMV Practice Quiz", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${viewModel.getTotalQuestions()} questions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.startQuiz() },
                    colors = ButtonDefaults.buttonColors(containerColor = DMVGreen)
                ) {
                    Text(if (questionIndex > 0) "Retry Quiz" else "Start Quiz")
                }
            }
        }
    } else {
        // Question Display
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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

                LinearProgressIndicator(
                    progress = (questionIndex + 1f) / viewModel.getTotalQuestions(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = DMVGreen,
                    trackColor = Color(0xFFE0E0E0)
                )

                // Category Badge
                SuggestionChip(
                    onClick = {},
                    label = { Text(currentQuestion.category, style = MaterialTheme.typography.bodySmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f),
                        labelColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Question
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val isCorrect = index == currentQuestion.correctAnswer
                    val bgColor = when {
                        !showExplanation -> if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent
                        isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                        isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                    val borderColor = when {
                        !showExplanation -> if (isSelected) PrimaryBlue else Color.LightGray
                        isCorrect -> SuccessGreen
                        isSelected && !isCorrect -> ErrorRed
                        else -> Color.LightGray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !showExplanation) { viewModel.selectAnswer(index) },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${'A' + index})",
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                modifier = Modifier.width(30.dp)
                            )
                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Explanation:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(currentQuestion.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DMVGreen)
                    ) {
                        Text(if (questionIndex + 1 < viewModel.getTotalQuestions()) "Next Question" else "See Results")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
