package com.hackathon.voicenavigator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.voicenavigator.data.api.RAGEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ESG ViewModel - Food Security Analysis using RAG Pipeline
 *
 * RAG Architecture (per professor's diagram):
 *   App UX → Orchestrator → IR Search (vector cosine similarity) → LLM → Response
 *                                 ↕
 *                    Data Sources (Food Security PDFs)
 *                    Transformed into Embeddings (NLP) via OpenAI text-embedding-ada-002
 */
class ESGViewModel(application: Application) : AndroidViewModel(application) {

    private val ragEngine = RAGEngine(application.applicationContext)

    private val _ragResponse = MutableStateFlow<String?>(null)
    val ragResponse: StateFlow<String?> = _ragResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory

    private val _initStatus = MutableStateFlow("Not initialized")
    val initStatus: StateFlow<String> = _initStatus

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    companion object {
        const val SOURCE_FOOD_SECURITY = "food_security"
    }

    private val systemPrompt = """You are an expert ESG analyst specializing in food security and nutrition.
You analyze data from the FAO's "The State of Food Security and Nutrition in the World" reports (2023, 2024, 2025).

CRITICAL: Answer ONLY from the retrieved document passages. Do NOT use external knowledge.
If the answer is not in the passages, say: "This information is not available in the provided reports."
Include specific statistics and data points. Cite the report year when possible."""

    /**
     * Initialize the RAG vector store.
     * Chunks text → generates embeddings via OpenAI → stores vectors in memory.
     */
    fun initializeRAG() {
        if (ragEngine.isInitialized(SOURCE_FOOD_SECURITY)) {
            _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_FOOD_SECURITY)} chunks)"
            return
        }
        viewModelScope.launch {
            _isInitializing.value = true
            _initStatus.value = "Chunking text & generating embeddings..."
            try {
                val success = ragEngine.initializeSource(SOURCE_FOOD_SECURITY, "food_security_reports.pdf", true)
                if (!success) {
                    // Fallback to extracted text
                    ragEngine.initializeFromText(SOURCE_FOOD_SECURITY, FOOD_SECURITY_TEXT)
                }
            } catch (_: Exception) {
                ragEngine.initializeFromText(SOURCE_FOOD_SECURITY, FOOD_SECURITY_TEXT)
            }
            _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_FOOD_SECURITY)} chunks)"
            _isInitializing.value = false
        }
    }

    /**
     * Full RAG query:  Query → Embed → Cosine Similarity Search → Top-K → LLM → Response
     */
    fun queryFoodSecurity(question: String) {
        viewModelScope.launch {
            _isLoading.value = true

            if (!ragEngine.isInitialized(SOURCE_FOOD_SECURITY)) {
                _initStatus.value = "Initializing RAG pipeline..."
                ragEngine.initializeFromText(SOURCE_FOOD_SECURITY, FOOD_SECURITY_TEXT)
                _initStatus.value = "Ready (${ragEngine.getChunkCount(SOURCE_FOOD_SECURITY)} chunks)"
            }

            val result = ragEngine.query(question, SOURCE_FOOD_SECURITY, systemPrompt)
            result.onSuccess { response ->
                _ragResponse.value = response
                _chatHistory.value = _chatHistory.value + Pair(question, response)
            }.onFailure { _ragResponse.value = "Error: ${it.message}" }

            _isLoading.value = false
        }
    }

    fun listFoodInsecurityReasons2024() = queryFoodSecurity("List the major food insecurity reasons in 2024 with specific statistics.")
    fun explainMalnutritionInWarZones() = queryFoodSecurity("Explain malnutrition in war zones and conflict areas.")
    fun explainPriceImpact() = queryFoodSecurity("Explain how increased prices impact food security with specific numbers.")
    fun compareFoodInsecurity2023vs2024() = queryFoodSecurity("Compare food insecurity between 2023 and 2024. Key quantitative differences?")
    fun explainQuantitativeDifferences() = queryFoodSecurity("Quantitative differences: percentage increase in global hunger, prevalence of undernourishment, low birthweight, and stunting.")
    fun listEconomicSustainability() = queryFoodSecurity("List economic sustainability statements including financing needs and policy recommendations.")
    fun listSocialSustainability() = queryFoodSecurity("List social sustainability statements including gender equality, social protection, and community approaches.")
    fun clearResponse() { _ragResponse.value = null }

    // Extracted content from SOFI 2024 & 2025 reports (data sources for vectorization)
    @Suppress("SpellCheckingInspection")
    private val FOOD_SECURITY_TEXT = """
THE STATE OF FOOD SECURITY AND NUTRITION IN THE WORLD 2024
FINANCING TO END HUNGER, FOOD INSECURITY AND MALNUTRITION IN ALL ITS FORMS

KEY FINDING: Between 713 and 757 million people faced hunger in 2023, equivalent to approximately one in eleven people globally, and one in five in Africa. This represents about 152 million more people than in 2019, before the global pandemic.

PREVALENCE OF UNDERNOURISHMENT:
The global prevalence of undernourishment (PoU) was 9.1 percent in 2023. Africa had the highest rate at 20.4 percent, followed by Asia at 8.1 percent and Latin America and the Caribbean at 6.2 percent. The world is far from achieving SDG Target 2.1 of ending hunger by 2030.

FOOD INSECURITY LEVELS:
Approximately 2.33 billion people in the world were moderately or severely food insecure in 2023. About 864 million people faced severe food insecurity, meaning they ran out of food, went hungry, or went without eating for an entire day. The gender gap in food insecurity persisted at 2.7 percentage points in 2023, with women more affected than men in every region of the world.

MALNUTRITION IN CHILDREN:
Child stunting: An estimated 148.1 million children under 5 years of age were stunted in 2022, representing 22.3 percent. While this represents a decline from 204.2 million in 2000, progress is insufficient to meet the 2030 targets. Child wasting: Approximately 45 million children under 5 (6.8 percent) suffered from wasting in 2022. Wasting is a life-threatening condition requiring urgent treatment. South Asia accounts for the highest burden of child wasting globally. Child overweight: About 37 million children under 5 (5.6 percent) were overweight in 2022. Low birthweight: An estimated 19.8 million babies (14.7 percent of all live births) were born with low birthweight in 2020. Southern Asia and sub-Saharan Africa bear the highest burden. Adult obesity: The prevalence of obesity among adults has nearly doubled since 2000, reaching 890 million adults (15.8 percent) in 2022.

COST OF A HEALTHY DIET:
More than 2.8 billion people in the world could not afford a healthy diet in 2022, over 35 percent of the global population. The cost of a healthy diet was estimated at USD 3.96 per person per day globally in 2021. The affordability gap is widest in low-income countries where 71.5 percent of the population cannot afford a healthy diet, compared to 6.3 percent in high-income countries.

KEY DRIVERS OF FOOD INSECURITY:
1. CONFLICT AND VIOLENCE: Armed conflict remains the primary driver of acute food insecurity. In 2023, conflict-affected countries accounted for the majority of people facing crisis-level food insecurity. The war in Ukraine disrupted global grain and fertilizer markets. Conflicts in Sudan, Gaza, Syria, Yemen, and the DRC have created severe humanitarian crises with millions facing famine-like conditions.
2. CLIMATE EXTREMES: Increasing frequency and severity of droughts, floods, heatwaves, and storms threaten agricultural production. El Nino events in 2023-2024 worsened conditions in East Africa, Central America, and South Asia. Climate change is projected to reduce crop yields by 2-6 percent per decade.
3. ECONOMIC SLOWDOWNS: Lingering effects of COVID-19 pandemic, rising inflation and debt levels have reduced household purchasing power. Many low- and middle-income countries face tight fiscal space.
4. HIGH FOOD PRICES: Food price inflation has been persistent and particularly harmful to the poorest. The FAO Food Price Index remains elevated above pre-pandemic levels. Domestic food prices in many developing countries remain high. The cost of food staples increased by 20-30 percent in many Sub-Saharan African countries since 2020.
5. RISING INEQUALITY: Income inequality between and within countries continues to widen. The poorest 20 percent of households spend 40-60 percent of their income on food.

FINANCING FOR FOOD SECURITY:
Current financing gaps are estimated at USD 176 billion per year to end hunger and malnutrition by 2030. Official Development Assistance (ODA) for food security averaged USD 13.7 billion annually in 2019-2021. The report recommends reforming harmful agricultural subsidies (estimated at USD 635 billion annually), increasing and better targeting ODA, leveraging innovative financing tools, and improving coordination between humanitarian and development financing.

REGIONAL ANALYSIS:
AFRICA: 20.4 percent undernourished in 2023. Number of hungry in Africa increased from 182 million in 2015 to 298 million in 2023. ASIA: Over 384 million hungry people. South Asia has highest child wasting rates globally. LATIN AMERICA: 6.2 percent undernourishment rate. Growing double burden of undernutrition and obesity.

THE STATE OF FOOD SECURITY AND NUTRITION IN THE WORLD 2025
ADDRESSING HIGH FOOD PRICE INFLATION FOR FOOD SECURITY AND NUTRITION

Six years from 2030, hunger and food insecurity trends are not yet moving in the right direction to end hunger (SDG Target 2.1) by 2030. The world is not on track to eliminate all forms of malnutrition (SDG Target 2.2). Billions of people still lack access to nutritious, safe and sufficient food.

HIGH FOOD PRICE INFLATION:
Food prices surged globally starting in late 2020, driven by pandemic disruptions, the Ukraine war, energy price increases, and climate shocks. While international commodity prices have moderated, domestic food prices remain stubbornly high. In Sub-Saharan Africa, food inflation exceeded 20 percent in multiple countries throughout 2023.

ECONOMIC SUSTAINABILITY STATEMENTS:
Implementing policies, investments and legislation to revert current trends requires proper financing. Despite broad agreement on increasing financing, there is no common understanding of how financing should be defined and tracked. The report provides a definition of financing for food security and nutrition. Recommendations include efficient use of innovative financing tools and reforms to the financing architecture. Domestic government spending must increase substantially. Harmful agricultural subsidies (USD 635 billion annually) should be reformed. Private sector investment needs alignment with food security goals. Innovative mechanisms like green bonds, blended finance, debt-for-food swaps should be expanded.

SOCIAL SUSTAINABILITY STATEMENTS:
Gender equality in food access is critical with a 2.7 percentage point gap globally. Social protection programs must reach the most vulnerable including displaced persons and refugees. Nutrition education programs are essential for addressing malnutrition. Community-based approaches including school feeding programs have proven effective. Indigenous and traditional food systems need protection. Inclusive governance requires participation of small-scale producers, women, youth, and indigenous peoples. Labor rights and fair wages in food systems are fundamental. Migration and urbanization require adaptive food security responses.

MALNUTRITION IN CONFLICT AREAS:
Armed conflict is the single largest driver of food crises worldwide. In 2023, approximately 135 million people in 20 countries faced acute food insecurity at crisis level due to conflict. Child malnutrition rates are 2-3 times higher than national averages in conflict areas. Famine risk is highest in Sudan, Gaza, Yemen, Somalia, and DRC.

COMPARISON 2023 vs 2024:
Global hunger: 691 million (2022) to 713-757 million (2023). Prevalence of undernourishment: 8.9% to 9.1%. Child stunting: 148.5 million to 148.1 million (slow progress). Child wasting: stable at 45 million (6.8%). Low birthweight: 19.8 million (14.7%). Adult obesity: continued increase to 890 million (15.8%). Cost of healthy diet: unaffordable for 2.8+ billion. Africa hunger: 282 million to 298 million.
    """.trimIndent()
}
