package com.hackathon.voicenavigator.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Manages voice input (Speech-to-Text) and output (Text-to-Speech)
 * Core component for the touchless voice-first navigation experience
 *
 * UPDATED BEHAVIOR:
 * - TTS is NOT enabled by default
 * - TTS only activates on explicit user requests
 * - Users can stop TTS at any time
 * - Allows switching tabs without interruption
 */
class VoiceRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecognitionManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── TTS Control States ─────────────────────────────────────
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _ttsEnabled = MutableStateFlow(false)  // NOT enabled by default
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled

    private var onResultCallback: ((String) -> Unit)? = null

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())
            Log.d(TAG, "✓ SpeechRecognizer initialized")
        }

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.95f)
                isTTSReady = true
                Log.d(TAG, "✓ TextToSpeech initialized (ready for manual activation)")
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status: $status")
                isTTSReady = false
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // VOICE INPUT (STT) METHODS
    // ════════════════════════════════════════════════════════════

    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        _error.value = null
        _recognizedText.value = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command...")
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            Log.d(TAG, "🎤 Started listening...")
        } catch (e: Exception) {
            _error.value = "Failed to start voice recognition: ${e.message}"
            _isListening.value = false
            Log.e(TAG, "Error starting listening: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        Log.d(TAG, "⏹ Stopped listening")
    }

    fun clearRecognizedText() {
        _recognizedText.value = ""
    }

    // ════════════════════════════════════════════════════════════
    // VOICE OUTPUT (TTS) METHODS
    // ════════════════════════════════════════════════════════════

    fun enableTTS() {
        _ttsEnabled.value = true
        Log.d(TAG, "✓ TTS enabled (manual activation)")
    }

    fun disableTTS() {
        _ttsEnabled.value = false
        stopSpeaking()
        Log.d(TAG, "⏹ TTS disabled")
    }

    fun toggleTTS(): Boolean {
        val newState = !_ttsEnabled.value
        if (newState) enableTTS() else disableTTS()
        return newState
    }

    fun speak(text: String): Boolean {
        if (!_ttsEnabled.value) {
            Log.d(TAG, "⊘ TTS disabled — skipping speech")
            return false
        }

        if (!isTTSReady) {
            Log.w(TAG, "⚠ TTS not ready yet")
            return false
        }

        if (text.isBlank()) {
            Log.w(TAG, "⚠ Empty text provided to speak()")
            return false
        }

        try {
            _isSpeaking.value = true
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_output")
            Log.d(TAG, "🔊 Speaking: ${text.take(50)}...")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking: ${e.message}")
            _isSpeaking.value = false
            return false
        }
    }

    fun stopSpeaking() {
        if (isTTSReady && _isSpeaking.value) {
            try {
                textToSpeech?.stop()
                _isSpeaking.value = false
                Log.d(TAG, "⏹ Stopped speaking")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech: ${e.message}")
            }
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            Log.d(TAG, "✓ Voice resources destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying voice resources: ${e.message}")
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _error.value = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            _recognizedText.value = text
            onResultCallback?.invoke(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            _recognizedText.value = matches?.firstOrNull() ?: ""
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

/**
 * Parses voice commands to determine the intended action
 */
object VoiceCommandParser {

    sealed class VoiceCommand {
        // Market Research Commands
        data object ShowGDPGraph : VoiceCommand()
        data object ShowCO2Graph : VoiceCommand()
        data object ShowAgriLandGraph : VoiceCommand()
        data object DescribeCO2 : VoiceCommand()
        data object ShowDOWStocks : VoiceCommand()
        data class ShowMilkPrice(val query: String) : VoiceCommand()
        data class ShowGasPrice(val query: String) : VoiceCommand()

        // Food Security Commands
        data object MajorFoodSecurityIssues : VoiceCommand()
        data class FoodSecurityQuery(val query: String) : VoiceCommand()

        // DMV Commands
        data object ShowSignalingSigns : VoiceCommand()
        data object ShowBACLimits : VoiceCommand()
        data class DMVQuery(val query: String) : VoiceCommand()

        // Navigation Commands
        data object GoToMarketResearch : VoiceCommand()
        data object GoToDMV : VoiceCommand()
        data object GoToESG : VoiceCommand()
        data object GoHome : VoiceCommand()

        // General
        data class GeneralQuery(val query: String) : VoiceCommand()
    }

    fun parse(input: String): VoiceCommand {
        val lower = input.lowercase().trim()

        return when {
            // Market Research - Charts
            lower.contains("gdp") && (lower.contains("graph") || lower.contains("chart") || lower.contains("show")) ->
                VoiceCommand.ShowGDPGraph

            lower.contains("co2") && (lower.contains("graph") || lower.contains("chart") || lower.contains("show")) && !lower.contains("describe") ->
                VoiceCommand.ShowCO2Graph

            lower.contains("agri") && (lower.contains("land") || lower.contains("graph") || lower.contains("chart")) ->
                VoiceCommand.ShowAgriLandGraph

            lower.contains("describe") && lower.contains("co2") ->
                VoiceCommand.DescribeCO2

            lower.contains("dow") || (lower.contains("top") && lower.contains("stock")) ->
                VoiceCommand.ShowDOWStocks

            lower.contains("milk") && lower.contains("price") ->
                VoiceCommand.ShowMilkPrice(input)

            lower.contains("gas") && lower.contains("price") ->
                VoiceCommand.ShowGasPrice(input)

            // Food Security
            lower.contains("food") && lower.contains("security") && lower.contains("issue") ->
                VoiceCommand.MajorFoodSecurityIssues

            lower.contains("food") && (lower.contains("insecurity") || lower.contains("hunger") || lower.contains("malnutrition") || lower.contains("nutrition")) ->
                VoiceCommand.FoodSecurityQuery(input)

            // DMV
            lower.contains("signal") && (lower.contains("sign") || lower.contains("turn")) ->
                VoiceCommand.ShowSignalingSigns

            lower.contains("blood") && lower.contains("alcohol") || lower.contains("bac") ->
                VoiceCommand.ShowBACLimits

            lower.contains("dmv") || lower.contains("driver") || lower.contains("driving") || lower.contains("license") ->
                VoiceCommand.DMVQuery(input)

            // Navigation
            lower.contains("market") && lower.contains("research") ->
                VoiceCommand.GoToMarketResearch

            lower.contains("go") && lower.contains("dmv") ->
                VoiceCommand.GoToDMV

            lower.contains("esg") || lower.contains("sustainability") ->
                VoiceCommand.GoToESG

            lower.contains("home") || lower.contains("main") ->
                VoiceCommand.GoHome

            // Speed limits, right of way, etc. - likely DMV
            lower.contains("speed") || lower.contains("right of way") || lower.contains("parking") ||
            lower.contains("intersection") || lower.contains("lane") || lower.contains("highway") ->
                VoiceCommand.DMVQuery(input)

            // Default: general query
            else -> VoiceCommand.GeneralQuery(input)
        }
    }
}
