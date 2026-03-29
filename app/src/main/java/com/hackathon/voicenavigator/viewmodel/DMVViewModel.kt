package com.hackathon.voicenavigator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.voicenavigator.data.api.GeminiApiService
import com.hackathon.voicenavigator.data.model.ChatMessage
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * DMV ViewModel — California Driver's Handbook
 *
 * Strategy: Try Gemini API first → fall back to hardcoded expert responses.
 * This guarantees the app ALWAYS works for demos, even with zero API quota.
 */
class DMVViewModel(application: Application) : AndroidViewModel(application) {

    private val _ragResponse = MutableStateFlow<String?>(null)
    val ragResponse: StateFlow<String?> = _ragResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    private val _initStatus = MutableStateFlow("✓ Ready (direct RAG)")
    val initStatus: StateFlow<String> = _initStatus

    // ── Response Cache ────────────────────────────────────────
    private val responseCache = mutableMapOf<String, String>()

    companion object {
        const val SOURCE_DMV = "dmv_handbook"
    }

    fun initializeRAG() {
        _initStatus.value = "✓ Ready (direct RAG"
    }

    private fun normalizeQuery(q: String): String =
        q.trim().lowercase().replace(Regex("\\s+"), " ")

    fun queryDMVHandbook(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cacheKey = normalizeQuery(question)

                // Check cache first
                val cached = responseCache[cacheKey]
                if (cached != null) {
                    _ragResponse.value = cached
                    _chatHistory.value = _chatHistory.value + Pair(question, cached)
                    _isLoading.value = false
                    return@launch
                }

                // Try API first
                val messages = listOf(
                    ChatMessage("system", "You are a California DMV test prep assistant. Answer ONLY from the handbook data provided. Include specific rules, distances, speed limits, and legal requirements. Be concise."),
                    ChatMessage("user", """Handbook:

$DMV_HANDBOOK_TEXT

---
Q: $question
Answer from the handbook above. Include specific numbers and rules.""")
                )

                val result = GeminiApiService.chatCompletion(messages, maxTokens = 768)
                result.onSuccess { response ->
                    responseCache[cacheKey] = response
                    _ragResponse.value = response
                    _chatHistory.value = _chatHistory.value + Pair(question, response)
                }.onFailure { error ->
                    // API failed — use fallback response
                    val fallback = getFallbackResponse(question)
                    if (fallback != null) {
                        responseCache[cacheKey] = fallback
                        _ragResponse.value = fallback
                        _chatHistory.value = _chatHistory.value + Pair(question, fallback)
                    } else {
                        val errMsg = formatError(error)
                        _ragResponse.value = errMsg
                        _chatHistory.value = _chatHistory.value + Pair(question, errMsg)
                    }
                }
            } catch (e: Exception) {
                val fallback = getFallbackResponse(question)
                if (fallback != null) {
                    _ragResponse.value = fallback
                    _chatHistory.value = _chatHistory.value + Pair(question, fallback)
                } else {
                    val errMsg = formatError(e)
                    _ragResponse.value = errMsg
                    _chatHistory.value = _chatHistory.value + Pair(question, errMsg)
                }
            }
            _isLoading.value = false
        }
    }

    private fun formatError(error: Throwable): String {
        val msg = error.message ?: "Unknown error"
        return when {
            msg.contains("quota", ignoreCase = true) || msg.contains("429") || msg.contains("limit", ignoreCase = true) ->
                "⚠️ API quota exceeded. Please wait a minute and try again, or use a new Gemini API key."
            msg.contains("403") ->
                "⚠️ API key invalid or expired. Please check your Gemini API key."
            msg.contains("timeout", ignoreCase = true) || msg.contains("connect", ignoreCase = true) ->
                "⚠️ Network timeout. Please check your internet connection."
            else -> "Error: $msg"
        }
    }

    fun querySignalingSigns() = queryDMVHandbook("What are the signaling signs for driving? Include all hand-and-arm signals for left turn, right turn, and slow/stop.")
    fun queryBACLimits() = queryDMVHandbook("What are all the Blood Alcohol Concentration (BAC) limits in California? Include limits for all driver categories and the BAC table.")
    fun clearResponse() { _ragResponse.value = null }

    // ================================================================
    // FALLBACK RESPONSES — pre-computed from DMV Handbook
    // Guarantees the app works even with zero API quota
    // ================================================================
    private fun getFallbackResponse(question: String): String? {
        val q = question.lowercase()
        return when {
            q.contains("signal") && (q.contains("sign") || q.contains("hand") || q.contains("arm")) -> FALLBACK_SIGNALING
            q.contains("bac") || q.contains("blood alcohol") || q.contains("alcohol concentration") -> FALLBACK_BAC
            q.contains("speed limit") -> FALLBACK_SPEED_LIMITS
            q.contains("parking") && (q.contains("hill") || q.contains("uphill") || q.contains("downhill")) -> FALLBACK_PARKING_HILLS
            q.contains("right-of-way") || q.contains("right of way") -> FALLBACK_RIGHT_OF_WAY
            q.contains("headlight") -> FALLBACK_HEADLIGHTS
            q.contains("following distance") || q.contains("3-second") || q.contains("three second") -> FALLBACK_FOLLOWING_DISTANCE
            q.contains("curb") && q.contains("color") -> FALLBACK_COLORED_CURBS
            q.contains("dui") || q.contains("penalty") || q.contains("penalties") -> FALLBACK_DUI
            q.contains("seat belt") || q.contains("child") && q.contains("seat") -> FALLBACK_SEATBELTS
            q.contains("cell phone") || q.contains("phone") && q.contains("driv") -> FALLBACK_CELLPHONE
            q.contains("school bus") -> FALLBACK_SCHOOL_BUS
            q.contains("lane") && (q.contains("hov") || q.contains("carpool")) -> FALLBACK_HOV
            q.contains("turn") && (q.contains("u-turn") || q.contains("u turn")) -> FALLBACK_UTURN
            q.contains("insurance") || q.contains("financial responsibility") -> FALLBACK_INSURANCE
            q.contains("traffic sign") || q.contains("octagonal") || q.contains("stop sign") -> FALLBACK_TRAFFIC_SIGNS
            q.contains("traffic signal") || q.contains("red light") || q.contains("green light") -> FALLBACK_TRAFFIC_SIGNALS
            else -> FALLBACK_GENERAL // Generic helpful response for unknown DMV questions
        }
    }

    private val FALLBACK_SIGNALING = """
**Signaling Signs for Driving (California Driver's Handbook):**

Always signal when turning, changing lanes, slowing, or stopping.

**Hand-and-Arm Signals:**
• **Left Turn:** Left arm extended straight out horizontally.
• **Right Turn:** Left arm extended upward, bent at elbow (forming an L-shape pointing up).
• **Slow/Stop:** Left arm extended downward, bent at elbow (forming an L-shape pointing down).

**Signaling Distance Requirements:**
• Signal at least **100 feet** before making a turn.
• On freeways, signal at least **5 seconds** before changing lanes.

These hand-and-arm signals are used when your vehicle's signal lights are not functioning, or as additional visibility in bright sunlight.
""".trimIndent()

    private val FALLBACK_BAC = """
**Blood Alcohol Concentration (BAC) Limits in California:**

**Legal Limits by Driver Category:**
• **Drivers over 21:** 0.08% or higher is illegal.
• **Drivers under 21:** 0.01% or higher (zero-tolerance policy).
• **On DUI probation (any age):** 0.01% or higher.
• **Commercial driver's license (CDL):** 0.04% or higher.
• **Driving passenger for hire:** 0.04% or higher.

**BAC Table (Male) — After 1 Drink:**
• 100 lb = 0.06% | 120 lb = 0.05% | 140 lb = 0.04%
• 160 lb = 0.04% | 180 lb = 0.03% | 200 lb = 0.03%

**BAC Table (Male) — After 2 Drinks:**
• 100 lb = 0.12% | 120 lb = 0.10% | 140 lb = 0.09%
• 160 lb = 0.07% | 180 lb = 0.07% | 200 lb = 0.06%

**BAC Table (Female) — After 1 Drink:**
• Slightly higher than male at same weight.
• 100 lb = 0.07% | 120 lb = 0.06% | 140 lb = 0.05% | 160 lb = 0.04%

**What counts as 1 drink:**
• 1.5 oz of 80-proof liquor
• 12 oz of 5% beer
• 5 oz of 12% wine
""".trimIndent()

    private val FALLBACK_SPEED_LIMITS = """
**California Speed Limits:**

• **Residential areas:** 25 mph
• **Near schools (children present):** 25 mph (some zones 15 mph)
• **Business districts:** 25 mph
• **Blind intersections:** 15 mph
• **Alleys:** 15 mph
• **Near railroad tracks** (cannot see 400 feet both directions): 15 mph
• **Highways:** 65 mph (some posted at 70 mph)
• **Undivided two-lane highways:** 55 mph
""".trimIndent()

    private val FALLBACK_PARKING_HILLS = """
**Parking on Hills (California Driver's Handbook):**

• **Headed downhill** (with or without curb): Turn wheels **toward the curb** (right).
• **Headed uphill with curb:** Turn wheels **away from curb** (left) — so the car rolls back into the curb.
• **Headed uphill without curb:** Turn wheels **right** toward the edge of road.
• Always park within **18 inches** of the curb.
""".trimIndent()

    private val FALLBACK_RIGHT_OF_WAY = """
**Right-of-Way Rules (California Driver's Handbook):**

• **At intersections without signs:** First to arrive goes first. If same time, yield to vehicle on your **right**.
• **T-intersections:** Through road traffic has right-of-way.
• **Turning left:** Yield to oncoming vehicles and pedestrians.
• **Pedestrians:** Always have right-of-way in crosswalks (marked or unmarked).
• **Emergency vehicles** (sirens/lights): Pull to right edge and stop. Do not follow within **300 feet**.
• **Mountain roads:** Vehicle facing **uphill** has right-of-way.
• **Roundabouts:** Yield to traffic already in the circle. Travel counterclockwise.
""".trimIndent()

    private val FALLBACK_HEADLIGHTS = """
**Headlight Rules (California Driver's Handbook):**

• Use headlights from **30 minutes after sunset** to **30 minutes before sunrise**.
• Use when visibility is under **1,000 feet**.
• In fog, rain, snow — use **low beams**.
• On mountain roads even on sunny days.
• Dim high beams within **500 feet** of oncoming vehicle.
• Dim high beams within **300 feet** when following another vehicle.
""".trimIndent()

    private val FALLBACK_FOLLOWING_DISTANCE = """
**Following Distance (California Driver's Handbook):**

• Use the **3-second rule** to maintain a safe following distance.
• In rain or fog: **double the following distance** (6 seconds).
• Large trucks need **400 feet** to stop at 55 mph.
""".trimIndent()

    private val FALLBACK_COLORED_CURBS = """
**Colored Curbs (California Driver's Handbook):**

• **White:** Stop only to pick up/drop off passengers.
• **Green:** Limited time parking.
• **Yellow:** Load/unload passengers and freight only.
• **Red:** No stopping, standing, or parking.
• **Blue:** Disabled persons only with placard.
""".trimIndent()

    private val FALLBACK_DUI = """
**DUI Penalties (California Driver's Handbook):**

• License suspension/revocation for **one year**.
• DUI program required.
• SR 22/SR 1P insurance filing required.
• Possible ignition interlock device.
• Up to **6 months jail**.
• Fines.
• Vehicle impoundment.
• Conviction on record for **10 years**.
""".trimIndent()

    private val FALLBACK_SEATBELTS = """
**Seat Belt & Child Restraint Laws:**

• **All occupants** must wear seat belts.
• **Under 2 years / under 40 lbs / under 3'4":** Rear-facing car seat.
• **Under 8 years or under 4'9":** Child restraint system in the rear seat.
• **8+ years or 4'9"+:** Standard safety belt.
""".trimIndent()

    private val FALLBACK_CELLPHONE = """
**Cell Phone Laws (California Driver's Handbook):**

• **Handheld** cell phone use while driving is **illegal**.
• **Adults:** Hands-free devices only.
• **Minors (under 18):** No electronic device use while driving at all (except emergency 911 calls).
""".trimIndent()

    private val FALLBACK_SCHOOL_BUS = """
**School Bus Laws (California Driver's Handbook):**

• **Stop** when red lights flash on a school bus.
• Fine up to **${'$'}1,000** and one-year license suspension for failure to stop.
• **Exception:** On a divided highway with 2+ lanes each direction — you may pass on your side.
""".trimIndent()

    private val FALLBACK_HOV = """
**HOV/Carpool Lane Rules (California Driver's Handbook):**

• Requires **2+ occupants**, buses, motorcycles, or low-emission vehicles with decal.
• Marked with a **diamond** symbol.
• Do **not** cross double solid lines to enter or exit.
""".trimIndent()

    private val FALLBACK_UTURN = """
**U-Turn Rules (California Driver's Handbook):**

**Legal U-Turns:**
• Across double yellow lines in a **residential district** if no vehicles within 200 feet.

**Illegal U-Turns:**
• At railroad crossings
• On divided highways
• On one-way streets
• Near fire stations
• Where a NO U-TURN sign is posted
""".trimIndent()

    private val FALLBACK_INSURANCE = """
**Financial Responsibility / Insurance Minimums (California):**

• **${'$'}30,000** for single death or injury.
• **${'$'}60,000** for death or injury to multiple persons.
• **${'$'}15,000** for property damage.
""".trimIndent()

    private val FALLBACK_TRAFFIC_SIGNS = """
**Traffic Signs (California Driver's Handbook):**

• **Red octagon (STOP):** Full stop before crosswalk or limit line.
• **Red inverted triangle (YIELD):** Slow down, yield to traffic.
• **5-sided sign:** School zone.
• **Diamond shape:** Warning of road conditions ahead.
• **Orange diamond:** Construction zone.
• **Yellow/black circle or X:** Railroad crossing.
""".trimIndent()

    private val FALLBACK_TRAFFIC_SIGNALS = """
**Traffic Signals (California Driver's Handbook):**

• **Solid Red:** STOP. May turn right after complete stop unless NO TURN ON RED sign posted.
• **Red Arrow:** STOP. Do NOT turn in the arrow direction.
• **Flashing Red:** Treat as a stop sign — stop then go when safe.
• **Solid Yellow:** Caution — stop if safe to do so.
• **Flashing Yellow:** Proceed with caution, no stop required.
• **Solid Green:** Go, yield to pedestrians and vehicles already in intersection.
• **Green Arrow:** Protected turn in arrow direction.
• **Traffic light not working:** Treat as an all-way stop.
""".trimIndent()

    private val FALLBACK_GENERAL = """
**California Driver's Handbook — Key Facts:**

Based on the official California Driver's Handbook, here are essential rules:

• Speed limit in residential areas: **25 mph**. Near schools: **25 mph** (15 mph in some zones).
• BAC limit over 21: **0.08%**. Under 21: **0.01%** (zero tolerance).
• Signal at least **100 feet** before turning. On freeways: **5 seconds** before lane change.
• Follow the **3-second rule** for safe following distance. Double in rain/fog.
• Park within **18 inches** of curb. On hills: turn wheels to prevent rolling.
• All occupants must wear seat belts. Children under 8 or under 4'9": child restraint required.
• Handheld phone use while driving is **illegal**. Minors: no electronic devices at all.
• Insurance minimums: ${'$'}30,000/${'$'}60,000/${'$'}15,000 (injury/multiple/property).

For a more specific answer, try asking about a particular topic (e.g., "BAC limits", "parking on hills", "right of way").
""".trimIndent()

    // ── Quiz ──────────────────────────────────────────────────
    private val questions = listOf(
        DMVQuestion(1, "What is the maximum speed limit in a residential area?",
            listOf("15 mph", "20 mph", "25 mph", "30 mph"), 2,
            "The speed limit in residential areas is 25 mph unless otherwise posted.", "Speed Limits"),
        DMVQuestion(2, "What BAC level is illegal for drivers over 21?",
            listOf("0.01%", "0.04%", "0.06%", "0.08%"), 3,
            "For drivers over 21, a BAC of 0.08% or higher is illegal.", "DUI Laws"),
        DMVQuestion(3, "When parking uphill with a curb, which way should you turn your wheels?",
            listOf("Toward the curb", "Away from the curb", "Straight ahead", "It doesn't matter"), 1,
            "When parking uphill with a curb, turn wheels away from curb so it rolls back into the curb.", "Parking"),
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
            "Zero-tolerance: BAC limit of 0.01% for drivers under 21.", "DUI Laws"),
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

    private val DMV_HANDBOOK_TEXT = """
CALIFORNIA DRIVER'S HANDBOOK — Official DMV Publication

SIGNALING:
Always signal when turning, changing lanes, slowing, or stopping.
Hand-and-arm signals: Left Turn = left arm extended straight out. Right Turn = left arm extended upward bent at elbow. Slow/Stop = left arm extended downward bent at elbow.
Signal at least 100 feet before turning. On freeways, signal at least 5 seconds before changing lanes.

SPEED LIMITS:
Residential areas: 25 mph. Near schools when children present: 25 mph (some zones 15 mph). Business districts: 25 mph. Blind intersections: 15 mph. Alleys: 15 mph. Near railroad tracks (cannot see 400 feet both directions): 15 mph. Highways: 65 mph (some 70 mph). Undivided two-lane highways: 55 mph.

BLOOD ALCOHOL CONCENTRATION (BAC) LIMITS:
Over 21: 0.08% or higher is illegal. Under 21: 0.01% or higher (zero tolerance). On DUI probation (any age): 0.01% or higher. Commercial driver's license: 0.04% or higher. Driving passenger for hire: 0.04% or higher.
BAC Table (Male): 100lb=0.06%, 120lb=0.05%, 140lb=0.04%, 160lb=0.04%, 180lb=0.03%, 200lb=0.03% (1 drink). 100lb=0.12%, 120lb=0.10%, 140lb=0.09%, 160lb=0.07%, 180lb=0.07%, 200lb=0.06% (2 drinks).
BAC Table (Female): Slightly higher than male at same weight. 100lb=0.07%, 120lb=0.06%, 140lb=0.05%, 160lb=0.04% (1 drink).
1 drink = 1.5 oz 80-proof liquor, 12 oz 5% beer, or 5 oz 12% wine.

TRAFFIC SIGNALS:
Solid Red: STOP. May turn right after complete stop unless NO TURN ON RED sign posted.
Red Arrow: STOP. Do NOT turn in arrow direction.
Flashing Red: Treat as stop sign — stop then go when safe.
Solid Yellow: Caution — stop if safe to do so.
Flashing Yellow: Proceed with caution, no stop required.
Solid Green: Go, yield to pedestrians and vehicles already in intersection.
Green Arrow: Protected turn in arrow direction.
Traffic light not working: Treat as all-way stop.

TRAFFIC SIGNS:
Red octagon (STOP): Full stop before crosswalk or limit line.
Red inverted triangle (YIELD): Slow down, yield to traffic.
5-sided sign: School zone.
Diamond shape: Warning of road conditions ahead.
Orange diamond: Construction zone.
Yellow/black circle or X: Railroad crossing.

RIGHT-OF-WAY:
At intersections without signs: First to arrive goes first. If same time, yield to vehicle on your right.
T-intersections: Through road traffic has right-of-way.
Turning left: Yield to oncoming vehicles and pedestrians.
Pedestrians always have right-of-way in crosswalks (marked or unmarked).
Emergency vehicles with sirens/lights: Pull to right edge and stop. Do not follow within 300 feet.
Mountain roads: Vehicle facing uphill has right-of-way.
Roundabouts: Yield to traffic already in the circle. Travel counterclockwise.

PARKING ON HILLS:
Headed downhill with or without curb: Turn wheels toward curb (right).
Headed uphill with curb: Turn wheels away from curb (left) — rolls back into curb.
Headed uphill without curb: Turn wheels right toward edge of road.
Park within 18 inches of curb.

COLORED CURBS:
White: Stop only to pick up/drop off passengers.
Green: Limited time parking.
Yellow: Load/unload passengers and freight only.
Red: No stopping, standing, or parking.
Blue: Disabled persons only with placard.

LANE MARKINGS:
Double solid yellow: Do not cross. Center of road.
Single solid yellow: Do not pass if on your side.
Broken yellow: May pass when safe.
Double solid white: Lane barrier — never cross (e.g., HOV lane).
Single solid white: Same-direction traffic lanes.
Broken white: May change lanes.

HOV/CARPOOL LANES:
Requires 2+ occupants, buses, motorcycles, or low-emission vehicles with decal.
Marked with diamond. Do not cross double solid lines.

TURNS:
Right turn: Stay close to right edge. Signal 100 feet before. Stop behind limit line. Look left-right-left.
Left turn: Stay close to center. Signal 100 feet before. Keep wheels straight until turning.
U-turn: Legal across double yellow lines in residential district if no vehicles within 200 feet. Illegal at railroad crossings, divided highways, one-way streets, fire stations, where NO U-TURN sign posted.

FOLLOWING DISTANCE: Use 3-second rule. In rain/fog: double it.

HEADLIGHTS: Use from 30 minutes after sunset to 30 minutes before sunrise. When visibility under 1,000 feet. In fog, rain, snow — use low beams. On mountain roads even on sunny days. Dim high beams within 500 feet of oncoming vehicle or 300 feet when following.

SEAT BELTS: All occupants must wear. Child under 2 years/under 40 lbs/under 3'4": rear-facing seat. Under 8 years or under 4'9": child restraint in rear seat. 8+ years or 4'9"+: safety belt.

CELL PHONES: Handheld use while driving illegal. Adults: hands-free only. Minors: no electronic device while driving (except emergency calls).

SCHOOL BUSES: Stop when red lights flash. Fine up to ${'$'}1,000 and one-year suspension for failure to stop. Exception: divided highway with 2+ lanes each direction — may pass on your side.

SHARING THE ROAD:
Motorcycles: Full lane width. 3-second following distance. Lane splitting is legal in California.
Bicyclists: At least 3 feet when passing. Enter bike lane no more than 200 feet before turning.
Large trucks: Large blind spots (No Zones). If you cannot see truck mirrors, driver cannot see you. Trucks need 400 feet to stop at 55 mph.

DUI PENALTIES:
License suspension/revocation for one year. DUI program required. SR 22/SR 1P insurance filing. Possible ignition interlock device. Up to 6 months jail. Fines. Vehicle impoundment. Conviction on record 10 years.

FINANCIAL RESPONSIBILITY (Insurance minimums):
${'$'}30,000 single death/injury. ${'$'}60,000 death/injury to multiple persons. ${'$'}15,000 property damage.

POINTS ON RECORD:
License may be suspended: 4 points in 12 months, 6 points in 24 months, 8 points in 36 months.

WORK ZONES: Fines ${'$'}1,000+. Doubled in Safety Enhanced-Double Fine Zones.
""".trimIndent()
}