package com.hackathon.voicenavigator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.voicenavigator.data.api.RAGEngine
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * DMV ViewModel - California Driver's Handbook using RAG Pipeline
 *
 * RAG Architecture (per professor's diagram):
 *   App UX → Orchestrator → IR Search (vector cosine similarity) → LLM → Response
 *                                 ↕
 *                    Data Sources (CA Driver's Handbook PDF)
 *                    Transformed into Embeddings (NLP) via OpenAI text-embedding-ada-002
 */
class DMVViewModel(application: Application) : AndroidViewModel(application) {

    private val ragEngine = RAGEngine(application.applicationContext)

    // ── RAG Response ───────────────────────────────────────────
    private val _ragResponse = MutableStateFlow<String?>(null)
    val ragResponse: StateFlow<String?> = _ragResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── Quiz State ─────────────────────────────────────────────
    private val _currentQuestion = MutableStateFlow<DMVQuestion?>(null)
    val currentQuestion: StateFlow<DMVQuestion?> = _currentQuestion

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore

    private val _questionIndex = MutableStateFlow(0)
    val questionIndex: StateFlow<Int> = _questionIndex

    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer

    private val _showExplanation = MutableStateFlow(false)
    val showExplanation: StateFlow<Boolean> = _showExplanation

    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory

    private val _initStatus = MutableStateFlow("Not initialized")
    val initStatus: StateFlow<String> = _initStatus

    companion object {
        const val SOURCE_DMV = "dmv_handbook"
    }

    private val systemPrompt = """You are a California DMV Knowledge Test preparation assistant.
You help users prepare for the California Driver's License knowledge test.

CRITICAL: Answer ONLY from the retrieved handbook passages. Do NOT use external knowledge.
Include relevant rules, regulations, and safety information.
If the information is not in the provided passages, say so clearly."""

    /**
     * Initialize the RAG pipeline for DMV handbook.
     */
    fun initializeRAG() {
        if (ragEngine.isInitialized(SOURCE_DMV)) {
            _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_DMV)} chunks)"
            return
        }
        viewModelScope.launch {
            _initStatus.value = "Indexing DMV handbook..."
            try {
                val success = ragEngine.initializeSource(SOURCE_DMV, "california_driver_handbook.pdf", true)
                if (!success) ragEngine.initializeFromText(SOURCE_DMV, DMV_HANDBOOK_TEXT)
            } catch (_: Exception) {
                ragEngine.initializeFromText(SOURCE_DMV, DMV_HANDBOOK_TEXT)
            }
            _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_DMV)} chunks)"
        }
    }

    /**
     * RAG query: Query → Embed → IR Search → Top-K → LLM → Response
     */
    fun queryDMVHandbook(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (!ragEngine.isInitialized(SOURCE_DMV)) {
                _initStatus.value = "Initializing RAG..."
                ragEngine.initializeFromText(SOURCE_DMV, DMV_HANDBOOK_TEXT)
                _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_DMV)} chunks)"
            }
            val result = ragEngine.query(question, SOURCE_DMV, systemPrompt)
            result.onSuccess { response ->
                _ragResponse.value = response
                _chatHistory.value = _chatHistory.value + Pair(question, response)
            }.onFailure { _ragResponse.value = "Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun querySignalingSigns() = queryDMVHandbook("What are the signaling signs for driving? Include hand-and-arm signals for left turn, right turn, and slow/stop.")
    fun queryBACLimits() = queryDMVHandbook("What are the Blood Alcohol Concentration (BAC) limits in California? Include limits for all driver categories.")

    // ── Quiz ───────────────────────────────────────────────────
    private val questions = listOf(
        DMVQuestion(1, "What is the maximum speed limit in a residential area?",
            listOf("15 mph", "20 mph", "25 mph", "30 mph"), 2,
            "The speed limit in residential areas is 25 mph unless otherwise posted.", "Speed Limits"),
        DMVQuestion(2, "What BAC level is illegal for drivers over 21?",
            listOf("0.01%", "0.04%", "0.06%", "0.08%"), 3,
            "For drivers over 21, a BAC of 0.08% or higher is illegal.", "DUI Laws"),
        DMVQuestion(3, "When parking uphill with a curb, which way should you turn your wheels?",
            listOf("Toward the curb", "Away from the curb", "Straight ahead", "It doesn't matter"), 1,
            "When parking uphill with a curb, turn your wheels away from the curb.", "Parking"),
        DMVQuestion(4, "What does a red octagonal sign mean?",
            listOf("Yield", "Stop", "No entry", "Speed limit"), 1,
            "A red octagonal (8-sided) sign means STOP.", "Traffic Signs"),
        DMVQuestion(5, "How far should you signal before making a turn?",
            listOf("50 feet", "100 feet", "150 feet", "200 feet"), 1,
            "Signal at least 100 feet before making a turn.", "Signaling"),
        DMVQuestion(6, "What is the speed limit near schools when children are present?",
            listOf("15 mph", "20 mph", "25 mph", "30 mph"), 2,
            "25 mph when children are present near schools.", "Speed Limits"),
        DMVQuestion(7, "At an uncontrolled intersection, who has the right-of-way?",
            listOf("Vehicle on the left", "Vehicle on the right", "Larger vehicle", "Faster vehicle"), 1,
            "Yield to the vehicle on your right.", "Right-of-Way"),
        DMVQuestion(8, "What is the BAC limit for drivers under 21?",
            listOf("0.01%", "0.02%", "0.04%", "0.08%"), 0,
            "Zero-tolerance: BAC limit of 0.01% for under 21.", "DUI Laws"),
        DMVQuestion(9, "When should you use your headlights?",
            listOf("Only at night", "Sunset to sunrise", "Only in rain", "Only on highways"), 1,
            "Use headlights from sunset to sunrise and when visibility is less than 1,000 feet.", "Safe Driving"),
        DMVQuestion(10, "What is the safe following distance rule?",
            listOf("1-second rule", "2-second rule", "3-second rule", "5-second rule"), 2,
            "Maintain a 3-second following distance.", "Safe Driving")
    )

    fun startQuiz() {
        _questionIndex.value = 0; _quizScore.value = 0
        _selectedAnswer.value = null; _showExplanation.value = false
        _currentQuestion.value = questions.firstOrNull()
    }

    fun selectAnswer(index: Int) {
        _selectedAnswer.value = index; _showExplanation.value = true
        if (index == _currentQuestion.value?.correctAnswer) _quizScore.value += 1
    }

    fun nextQuestion() {
        val next = _questionIndex.value + 1
        if (next < questions.size) {
            _questionIndex.value = next; _currentQuestion.value = questions[next]
            _selectedAnswer.value = null; _showExplanation.value = false
        } else _currentQuestion.value = null
    }

    fun getTotalQuestions() = questions.size
    fun clearResponse() { _ragResponse.value = null }

    // Extracted content from California Driver's Handbook (data source for vectorization)
    @Suppress("SpellCheckingInspection")
    private val DMV_HANDBOOK_TEXT = """
CALIFORNIA DRIVER'S HANDBOOK

SIGNALING
Always signal when you turn, change lanes, slow down, or stop. You can signal using your vehicle's signal lights or using hand-and-arm positions. If your signal lights are not functioning, or bright sunlight makes your signal lights hard to see, use the hand-and-arm signals. Left Turn: Left arm extended straight out the window. Right Turn: Left arm extended upward, bent at the elbow. Slow or Stop: Left arm extended downward, bent at the elbow. Bicyclists may signal a turn with their arm held straight out, pointing in the direction they plan to turn. You should signal at least 100 feet before turning. On freeways, signal at least 5 seconds before changing lanes.

BLOOD ALCOHOL CONCENTRATION (BAC) LIMITS
When you consume alcohol, traces of it enter your bloodstream. Your BAC measures how much alcohol is present in your bloodstream. It is illegal for you to drive if you have a BAC of: 0.08% or higher if you are over 21 years old. 0.01% or higher if you are under 21 years old. 0.01% or higher at any age if you are on DUI probation. 0.04% or higher if you drive a vehicle that requires a commercial driver's license. 0.04% or higher if you are driving a passenger for hire. If you drive with an illegal BAC, law enforcement officers can charge you with DUI. Even if your BAC is below legal limits, that does not mean it is safe for you to drive. Alcohol affects everyone. Even at levels lower than the legal limit, driving ability can be impaired. Depending on how badly you are impaired, you may be arrested and convicted of a DUI even without a BAC measurement. The table below shows BAC estimates based on how many drinks are consumed, gender, and body weight. Remember, even one drink can affect your ability to drive safely. NOTE: It is illegal to drink alcohol or take drugs when you are operating a boat, jet ski, water skis, aquaplane, or similar vessels.

SPEED LIMITS
Maximum speed limits in California: Residential areas: 25 mph. Near schools and senior centers: 25 mph when children or seniors are present. Business districts: 25 mph. Highways: 65 mph, some posted at 70 mph. Undivided two-lane highways: 55 mph. Blind intersections: 15 mph. Alleys: 15 mph. Near railroad tracks: 15 mph within 100 feet if you cannot see 400 feet in both directions. You may drive faster than the posted speed limit only when passing another vehicle on a two-lane road, and only if it is safe and legal to do so. You must always drive at a speed that is safe for current road, weather, and traffic conditions, even if this means driving slower than the posted speed limit.

RIGHT-OF-WAY RULES
Right-of-way rules help people drive safely by determining who goes first at intersections and other driving scenarios. At intersections without signs or signals, yield to the vehicle that arrived first. If two vehicles arrive at the same time, yield to the vehicle on your right. At T-intersections, yield to traffic on the through road. Always yield to pedestrians in crosswalks, whether marked or unmarked. Yield to emergency vehicles with sirens and flashing lights by pulling over to the right edge of the road and stopping. At roundabouts, yield to traffic already in the circle. When entering a freeway, yield to traffic already on the freeway. At blind intersections, yield to any vehicle or pedestrian. Pedestrians always have the right-of-way in crosswalks.

LANE CHANGES AND MERGING
Before changing lanes: Check your mirrors (rearview and side mirrors). Check your blind spot by looking over your shoulder. Signal your intention for at least 5 seconds on a freeway. Make sure there is enough room in the lane you want to enter. Do not cross double yellow lines. Carpool/HOV lanes require 2 or more occupants during posted hours. Use designated entry and exit areas for HOV lanes.

PARKING RULES
When parking on a hill, curb your wheels: Uphill with curb: Turn wheels away from curb (left). Downhill with or without curb: Turn wheels toward curb (right). Uphill without curb: Turn wheels toward the edge of the road (right). Park within 18 inches of the curb. Do not park in front of driveways, fire hydrants (within 15 feet), on sidewalks, in crosswalks, or in spaces reserved for disabled persons without proper placard. When leaving your vehicle, set the parking brake, put automatic transmissions in Park, and manual transmissions in gear.

SAFE DRIVING PRACTICES
Use headlights from sunset to sunrise and when visibility is less than 1,000 feet. Keep a safe following distance using the 3-second rule. Use child safety seats for children under 2 years and under 40 pounds. All passengers must wear seat belts at all times. Do not use a handheld cell phone while driving. Drivers 18 and over may use hands-free devices only. Drivers under 18 cannot use any electronic device while driving, including hands-free devices. Do not drive while drowsy or fatigued. Avoid distractions such as eating, grooming, or adjusting the radio while driving.

TRAFFIC SIGNS AND SIGNALS
Red octagonal sign: STOP. Come to a complete stop at the limit line or before the crosswalk. Red inverted triangle: YIELD. Slow down, be prepared to stop, and yield to traffic and pedestrians. Yellow diamond: WARNING. Indicates hazards, changes in road conditions, or special situations ahead. Green rectangle: GUIDE signs. Provide directional information, distances, and services. Blue rectangle: SERVICE signs. Indicate nearby services such as gas stations, food, and hospitals. Brown rectangle: RECREATION signs. Indicate parks, historic sites, and recreational areas. Orange diamond: CONSTRUCTION signs. Warn of work zones and road construction. Yellow pennant shape: NO PASSING ZONE. A red circle with a line through it: indicates something is prohibited. Flashing red light: Treat as a stop sign. Flashing yellow light: Proceed with caution. Green arrow: You may turn in the direction of the arrow. Yellow arrow: The protected turning phase is ending.

SHARING THE ROAD
Motorcyclists have the same rights as other drivers. Give motorcycles a full lane width. Watch for bicyclists, especially when turning right or opening your car door. Yield to pedestrians at all crosswalks. Be cautious around large trucks; they have large blind spots. Do not cut in front of trucks. Allow extra following distance behind trucks.

DRIVING IN SPECIAL CONDITIONS
Rain: Reduce speed, increase following distance, turn on headlights, avoid hydroplaning. Fog: Use low-beam headlights, reduce speed, use the right edge of the road as a guide. Night: Use high-beam headlights on dark roads with no oncoming traffic, dim within 500 feet of oncoming vehicles. Mountains: Use lower gears on steep grades, watch for falling rocks, yield to uphill traffic on narrow mountain roads.

VEHICLE REGISTRATION AND INSURANCE
All vehicles must be registered with the California DMV. Maintain minimum liability insurance: $15,000 for injury/death of one person, $30,000 for injury/death of more than one person, $5,000 for property damage. Carry proof of insurance at all times while driving.

ORGAN DONATION
You can register as an organ donor through the DMV when applying for or renewing your driver's license.

FINANCIAL RESPONSIBILITY
California law requires all drivers to carry minimum liability insurance. Failure to maintain insurance can result in fines, suspension of vehicle registration, and impoundment of your vehicle.
    """.trimIndent()
}
