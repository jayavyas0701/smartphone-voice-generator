package com.hackathon.voicenavigator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.voicenavigator.data.api.BLSApiService
import com.hackathon.voicenavigator.data.api.GeminiApiService
import com.hackathon.voicenavigator.data.api.WorldBankApiService
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MarketResearchViewModel(application: Application) : AndroidViewModel(application) {

    // ── Chart Data States ──────────────────────────────────────
    private val _gdpData = MutableStateFlow<ChartData?>(null)
    val gdpData: StateFlow<ChartData?> = _gdpData

    private val _co2Data = MutableStateFlow<ChartData?>(null)
    val co2Data: StateFlow<ChartData?> = _co2Data

    private val _agriLandData = MutableStateFlow<ChartData?>(null)
    val agriLandData: StateFlow<ChartData?> = _agriLandData

    private val _co2PerCapitaData = MutableStateFlow<ChartData?>(null)
    val co2PerCapitaData: StateFlow<ChartData?> = _co2PerCapitaData

    // ── Selected Tab ───────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(ESGIndicator.GDP)
    val selectedTab: StateFlow<ESGIndicator> = _selectedTab

    // ── Loading & Error States ─────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ── ChatGPT Response ───────────────────────────────────────
    private val _chatResponse = MutableStateFlow<String?>(null)
    val chatResponse: StateFlow<String?> = _chatResponse

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

    // ── DOW Stocks Data ────────────────────────────────────────
    private val _dowStocks = MutableStateFlow<List<StockData>>(emptyList())
    val dowStocks: StateFlow<List<StockData>> = _dowStocks

    // ── Response Cache ────────────────────────────────────────
    private val responseCache = mutableMapOf<String, String>()

    fun selectTab(indicator: ESGIndicator) {
        _selectedTab.value = indicator
        // Clear cached AI response for this tab so Gemini always runs fresh
        responseCache.remove("describe_${indicator.name}")
        // Show hardcoded fallback instantly — no blank screen, no wait
        _chatResponse.value = getImmediateFallback(indicator)
        loadDataForIndicator(indicator)
        // Upgrade to live Gemini response in background
        describeIndicator(indicator)
    }

    private fun getImmediateFallback(indicator: ESGIndicator): String {
        // Check cache first — return instantly if already fetched
        val cached = responseCache["describe_${indicator.name}"]
        if (cached != null) return cached
        // Otherwise return the hardcoded fallback right away
        return when (indicator) {
            ESGIndicator.GDP -> FALLBACK_GDP
            ESGIndicator.CO2 -> FALLBACK_CO2_DESCRIBE
            ESGIndicator.AGRI_LAND -> FALLBACK_AGRI_LAND
            ESGIndicator.CO2_PER_CAPITA -> FALLBACK_CO2_DESCRIBE
        }
    }

    // Track which indicators were loaded from live API (not static fallback)
    private val liveDataLoaded = mutableSetOf<ESGIndicator>()

    fun loadDataForIndicator(indicator: ESGIndicator) {
        // Skip if already loaded from live API (tab switch back to same indicator)
        if (indicator in liveDataLoaded) return

        viewModelScope.launch {
            _isLoading.value = true

            // 10-second timeout — if World Bank is slow, give up gracefully
            val result = withTimeoutOrNull(10_000L) {
                when (indicator) {
                    ESGIndicator.GDP -> WorldBankApiService.fetchGDPData()
                    ESGIndicator.CO2 -> WorldBankApiService.fetchCO2Data()
                    ESGIndicator.AGRI_LAND -> WorldBankApiService.fetchAgriLandData()
                    ESGIndicator.CO2_PER_CAPITA -> WorldBankApiService.fetchCO2PerCapitaData()
                }
            }

            if (result == null) {
                android.util.Log.w("MarketResearchVM", "World Bank timed out for $indicator — using static fallback chart")
                val fallbackData = getStaticChartFallback(indicator)
                when (indicator) {
                    ESGIndicator.GDP -> _gdpData.value = fallbackData
                    ESGIndicator.CO2 -> _co2Data.value = fallbackData
                    ESGIndicator.AGRI_LAND -> _agriLandData.value = fallbackData
                    ESGIndicator.CO2_PER_CAPITA -> _co2PerCapitaData.value = fallbackData
                }
                // NOT added to liveDataLoaded — so next launch will retry live API
            } else {
                result.onSuccess { data ->
                    liveDataLoaded.add(indicator)  // mark as live — skip on tab switch back
                    when (indicator) {
                        ESGIndicator.GDP -> _gdpData.value = data
                        ESGIndicator.CO2 -> _co2Data.value = data
                        ESGIndicator.AGRI_LAND -> _agriLandData.value = data
                        ESGIndicator.CO2_PER_CAPITA -> _co2PerCapitaData.value = data
                    }
                }.onFailure { error ->
                    android.util.Log.w("MarketResearchVM",
                        "World Bank failed for $indicator: ${error.message ?: "no message"} — using static fallback")
                    val fallbackData = getStaticChartFallback(indicator)
                    when (indicator) {
                        ESGIndicator.GDP -> _gdpData.value = fallbackData
                        ESGIndicator.CO2 -> _co2Data.value = fallbackData
                        ESGIndicator.AGRI_LAND -> _agriLandData.value = fallbackData
                        ESGIndicator.CO2_PER_CAPITA -> _co2PerCapitaData.value = fallbackData
                    }
                    // NOT added to liveDataLoaded — retry next time
                }
            }

            _isLoading.value = false
        }
    }

    /** Static chart fallback data — shows a chart even if World Bank API is unreachable */
    private fun getStaticChartFallback(indicator: ESGIndicator): ChartData {
        return when (indicator) {
            ESGIndicator.GDP -> ChartData(
                title = "Global GDP Growth Rate (%)",
                unit = "% Annual Growth",
                dataPoints = listOf(
                    ChartDataPoint("2015", 2.9), ChartDataPoint("2016", 2.6),
                    ChartDataPoint("2017", 3.3), ChartDataPoint("2018", 3.1),
                    ChartDataPoint("2019", 2.6), ChartDataPoint("2020", -3.1),
                    ChartDataPoint("2021", 6.0), ChartDataPoint("2022", 3.1),
                    ChartDataPoint("2023", 2.7)
                )
            )
            ESGIndicator.CO2 -> ChartData(
                title = "Global CO₂ Emissions (kt)",
                unit = "kt CO₂",
                dataPoints = listOf(
                    ChartDataPoint("2015", 35400000.0), ChartDataPoint("2016", 35200000.0),
                    ChartDataPoint("2017", 35800000.0), ChartDataPoint("2018", 36600000.0),
                    ChartDataPoint("2019", 36400000.0), ChartDataPoint("2020", 34000000.0),
                    ChartDataPoint("2021", 36300000.0), ChartDataPoint("2022", 37400000.0)
                )
            )
            ESGIndicator.AGRI_LAND -> ChartData(
                title = "Agricultural Land (% of land area)",
                unit = "% of land",
                dataPoints = listOf(
                    ChartDataPoint("2015", 37.4), ChartDataPoint("2016", 37.3),
                    ChartDataPoint("2017", 37.2), ChartDataPoint("2018", 37.1),
                    ChartDataPoint("2019", 37.0), ChartDataPoint("2020", 36.9),
                    ChartDataPoint("2021", 36.8), ChartDataPoint("2022", 36.7)
                )
            )
            ESGIndicator.CO2_PER_CAPITA -> ChartData(
                title = "CO₂ Emissions Per Capita",
                unit = "t per person",
                dataPoints = listOf(
                    ChartDataPoint("2015", 4.8), ChartDataPoint("2016", 4.7),
                    ChartDataPoint("2017", 4.8), ChartDataPoint("2018", 4.9),
                    ChartDataPoint("2019", 4.8), ChartDataPoint("2020", 4.4),
                    ChartDataPoint("2021", 4.7), ChartDataPoint("2022", 4.8)
                )
            )
        }
    }

    fun loadGDP() = loadDataForIndicator(ESGIndicator.GDP)
    fun loadCO2() = loadDataForIndicator(ESGIndicator.CO2)
    fun loadAgriLand() = loadDataForIndicator(ESGIndicator.AGRI_LAND)

    /**
     * Auto-triggered when a tab is selected.
     * Fetches a Gemini AI description for the selected indicator.
     * Falls back to hardcoded analysis if Gemini is unavailable.
     */
    fun describeIndicator(indicator: ESGIndicator) {
        viewModelScope.launch {
            _isChatLoading.value = true
            val cacheKey = "describe_${indicator.name}"
            val cached = responseCache[cacheKey]
            if (cached != null) {
                _chatResponse.value = cached
                _isChatLoading.value = false
                return@launch
            }
            val (name, description) = when (indicator) {
                ESGIndicator.GDP -> Pair(
                    "Global GDP Growth Rate",
                    "Annual percentage growth of global GDP measured by the World Bank. Reflects global economic health, with sharp dips during the 2008 financial crisis and 2020 COVID-19 pandemic."
                )
                ESGIndicator.CO2 -> Pair(
                    "CO₂ Emissions",
                    "Carbon dioxide emissions from fossil fuel combustion and cement manufacturing. The largest driver of climate change and a key ESG environmental metric tracked globally."
                )
                ESGIndicator.AGRI_LAND -> Pair(
                    "Agricultural Land (% of land area)",
                    "Percentage of total land area used for agriculture globally. Tracked by the World Bank as a key food security and land-use sustainability indicator."
                )
                ESGIndicator.CO2_PER_CAPITA -> Pair(
                    "CO₂ Emissions Per Capita",
                    "Tonnes of CO₂ emitted per person per year. Highlights inequality in emissions between developed and developing nations."
                )
            }
            val result = GeminiApiService.describeIndicator(name, description)
            val response = result.getOrElse {
                // Gemini failed — just keep the fallback already on screen, don't overwrite
                null
            }
            if (response != null) {
                // Gemini succeeded — upgrade the displayed text
                responseCache[cacheKey] = response
                _chatResponse.value = response
            } else {
                // Cache the fallback so we don't retry on next tab switch
                val fallback = when (indicator) {
                    ESGIndicator.GDP -> FALLBACK_GDP
                    ESGIndicator.CO2 -> FALLBACK_CO2_DESCRIBE
                    ESGIndicator.AGRI_LAND -> FALLBACK_AGRI_LAND
                    ESGIndicator.CO2_PER_CAPITA -> FALLBACK_CO2_DESCRIBE
                }
                responseCache[cacheKey] = fallback
            }
            _isChatLoading.value = false
        }
    }

    fun describeCO2Emissions() {
        viewModelScope.launch {
            _isChatLoading.value = true

            val cacheKey = "describe_co2"
            val cached = responseCache[cacheKey]
            if (cached != null) {
                _chatResponse.value = cached
                _isChatLoading.value = false
                return@launch
            }

            val result = GeminiApiService.describeIndicator(
                "CO2 Emissions",
                "Carbon dioxide emissions, largely by-products of energy production and use, " +
                        "account for the largest share of greenhouse gases, which are associated with global warming. " +
                        "Anthropogenic carbon dioxide emissions result primarily from fossil fuel combustion and cement manufacturing."
            )
            result.onSuccess {
                responseCache[cacheKey] = it
                _chatResponse.value = it
            }.onFailure {
                val fallback = FALLBACK_CO2_DESCRIBE
                responseCache[cacheKey] = fallback
                _chatResponse.value = fallback
            }
            _isChatLoading.value = false
        }
    }

    fun loadDOWStocks() {
        viewModelScope.launch {
            _isChatLoading.value = true

            val cacheKey = "dow_stocks"
            val cached = responseCache[cacheKey]
            if (cached != null) {
                _chatResponse.value = cached
                _dowStocks.value = getDefaultDOWStocks()
                _isChatLoading.value = false
                return@launch
            }

            val result = GeminiApiService.queryStockAnalysis(
                "Top 10 Stocks of DOW with market percentages in JSON structure"
            )
            result.onSuccess { response ->
                responseCache[cacheKey] = response
                _chatResponse.value = response
                _dowStocks.value = getDefaultDOWStocks()
            }.onFailure {
                val fallback = FALLBACK_DOW_STOCKS
                responseCache[cacheKey] = fallback
                _chatResponse.value = fallback
                _dowStocks.value = getDefaultDOWStocks()
            }
            _isChatLoading.value = false
        }
    }

    fun queryMarketResearch(prompt: String) {
        viewModelScope.launch {
            _isChatLoading.value = true

            val cacheKey = prompt.trim().lowercase().replace(Regex("\\s+"), " ")
            val cached = responseCache[cacheKey]
            if (cached != null) {
                _chatResponse.value = cached
                _isChatLoading.value = false
                return@launch
            }

            val result = GeminiApiService.queryMarketResearch(prompt)
            result.onSuccess {
                responseCache[cacheKey] = it
                _chatResponse.value = it
            }.onFailure {
                // Try fallback
                val fallback = getMarketFallback(prompt)
                responseCache[cacheKey] = fallback
                _chatResponse.value = fallback
            }
            _isChatLoading.value = false
        }
    }

    fun clearChatResponse() {
        _chatResponse.value = null
    }

    // ================================================================
    // FALLBACK RESPONSES — guaranteed to work without API
    // ================================================================

    private fun getMarketFallback(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("gdp") && (q.contains("trend") || q.contains("growth") || q.contains("data")) -> FALLBACK_GDP
            q.contains("co2") || q.contains("carbon") || q.contains("emission") -> FALLBACK_CO2_DESCRIBE
            q.contains("agri") || q.contains("agriculture") || q.contains("land") || q.contains("farm") -> FALLBACK_AGRI_LAND
            q.contains("dow") || q.contains("stock") || q.contains("market") -> FALLBACK_DOW_STOCKS
            q.contains("gas") || q.contains("gasoline") || q.contains("fuel") -> FALLBACK_GAS_PRICE
            q.contains("milk") || q.contains("dairy") -> FALLBACK_MILK_PRICE
            q.contains("esg") || q.contains("environment") || q.contains("sustain") -> FALLBACK_ESG_OVERVIEW
            q.contains("difference") || q.contains("compare") || q.contains("vs") -> FALLBACK_COMPARISON
            q.contains("inflation") || q.contains("price") || q.contains("cost") -> FALLBACK_INFLATION
            else -> FALLBACK_GENERAL_MARKET
        }
    }

    private val FALLBACK_GDP = """
**Global GDP Growth Rate Analysis:**

Based on World Bank data (NY.GDP.MKTP.KD.ZG indicator):

• Global GDP growth has shown significant volatility in recent years.
• **2020:** Sharp contraction of approximately -3.1% due to the COVID-19 pandemic — the deepest global recession in decades.
• **2021:** Strong recovery at approximately +6.0% as economies reopened.
• **2022:** Growth moderated to approximately +3.1% amid inflation, supply chain disruptions, and the Ukraine war.
• **2023:** Further slowdown to approximately +2.7% as central banks raised interest rates to combat inflation.

**Long-term trends:** Global GDP growth averaged about 3.5% per year over the past two decades, with emerging economies generally growing faster than advanced economies. The data displayed in the chart comes from the World Bank API in real-time.
""".trimIndent()

    private val FALLBACK_CO2_DESCRIBE = """
**CO₂ Emissions — ESG Indicator Analysis:**

Carbon dioxide emissions are the largest contributor to greenhouse gases and global warming, primarily resulting from fossil fuel combustion and cement manufacturing. Global CO₂ emissions have been rising steadily, with energy production being the dominant source.

Key facts:
• Global CO₂ emissions reached approximately 37.4 billion tonnes in 2023.
• The energy sector accounts for about 73% of global greenhouse gas emissions.
• The Paris Agreement targets limiting warming to 1.5°C, requiring significant emissions reductions.
• Agricultural CO₂ emissions (shown in the chart) contribute to approximately 10-12% of global greenhouse gases.
• Developing nations face the dual challenge of economic growth while transitioning to cleaner energy sources.

The chart shows World Bank data on agricultural CO₂ emissions (EN.GHG.CO2.AG.MT.CE.AR5).
""".trimIndent()

    private val FALLBACK_AGRI_LAND = """
**Agricultural Land — ESG Indicator Analysis:**

Agricultural land as a percentage of total land area is a critical sustainability metric tracked by the World Bank (AG.LND.AGRI.ZS):

• Globally, approximately **37% of land area** is used for agriculture.
• This includes both cropland and permanent pastures.
• Agricultural land has been gradually **declining** in developed countries due to urbanization.
• In developing countries, agricultural land may be **expanding** at the cost of deforestation.
• Sustainable land management is crucial for food security and environmental health.

The chart displays the World Bank's global agricultural land data showing the trend over the past 20 years.
""".trimIndent()

    private val FALLBACK_DOW_STOCKS = """
**Top 10 Dow Jones Industrial Average (DJIA) Components by Weight:**

The DJIA consists of 30 major US companies. Top holdings include:

1. **Apple (AAPL)** — 9.2% weight
2. **UnitedHealth Group (UNH)** — 7.7%
3. **Home Depot (HD)** — 6.6%
4. **Goldman Sachs (GS)** — 5.4%
5. **Microsoft (MSFT)** — 5.2%
6. **Visa (V)** — 5.1%
7. **Boeing (BA)** — 5.1%
8. **McDonald's (MCD)** — 5.0%
9. **3M (MMM)** — 4.2%
10. **Johnson & Johnson (JNJ)** — 3.8%

The remaining 20 stocks account for approximately 42.9% of the index. The DJIA is price-weighted, meaning stocks with higher share prices have greater influence on the index.
""".trimIndent()

    private val FALLBACK_GAS_PRICE = """
**US Regular Gasoline Price Analysis:**

Based on Bureau of Labor Statistics data (Series: APU000074714):

• US regular gasoline prices have shown significant volatility over the past decades.
• **Historical low:** Around ${'$'}0.90/gallon in the late 1990s.
• **2008 peak:** Over ${'$'}4.00/gallon before the financial crisis.
• **2020 low:** Around ${'$'}2.00/gallon during COVID-19 demand collapse.
• **2022 peak:** Over ${'$'}5.00/gallon nationally due to post-pandemic demand surge and Ukraine war impacts.
• **Current trend:** Prices have moderated but remain above historical averages.

Key drivers include crude oil prices, refining capacity, seasonal demand, and geopolitical events.
""".trimIndent()

    private val FALLBACK_MILK_PRICE = """
**US Milk Price Analysis:**

Based on Bureau of Labor Statistics data (Series: APU0000709112):

• The average price of whole milk in the US has risen gradually over the past decades.
• **Current average:** Approximately ${'$'}4.00-${'$'}4.50 per gallon.
• Milk prices are influenced by feed costs, dairy farm economics, government programs, and consumer demand.
• Regional variation is significant — prices tend to be higher in urban areas and on the coasts.
• The USDA Federal Milk Marketing Orders help stabilize dairy prices through pricing formulas.
""".trimIndent()

    private val FALLBACK_ESG_OVERVIEW = """
**ESG (Environmental, Social, Governance) Overview:**

ESG indicators track sustainability and ethical impact across three dimensions:

**Environmental:**
• CO₂ emissions and carbon footprint
• Agricultural land use and deforestation
• Renewable energy adoption
• Water usage and pollution

**Social:**
• Food security and nutrition (see ESG tab for SOFI reports)
• Gender equality and labor rights
• Community development and health

**Governance:**
• Corporate transparency and ethics
• Regulatory compliance
• Anti-corruption measures

The Market Research dashboard displays World Bank environmental indicators (GDP growth, CO₂ emissions, agricultural land) with real-time API data visualization.
""".trimIndent()

    private val FALLBACK_COMPARISON = """
**ESG Indicator Comparison:**

The three key indicators shown in this dashboard from World Bank data:

| Indicator | What it measures | Trend |
|-----------|-----------------|-------|
| **GDP Growth** | Annual % change in global economic output | Volatile — pandemic shock + recovery |
| **CO₂ Emissions** | Agricultural greenhouse gas output in Mt CO₂ eq | Generally rising, slight recent moderation |
| **Agricultural Land** | % of total land area used for farming | Gradually declining globally |

**Key relationships:**
• GDP growth and CO₂ emissions tend to be positively correlated — economic growth often increases emissions.
• Agricultural land is declining as urbanization increases, but remaining farmland must be more productive.
• Sustainable development requires decoupling GDP growth from emissions — growing the economy while reducing environmental impact.

Switch between the GDP, CO2, and Agri. Land tabs to see each indicator's historical data.
""".trimIndent()

    private val FALLBACK_INFLATION = """
**Price and Inflation Analysis:**

Key price trends relevant to market research:

• **Global inflation** surged in 2021-2022, driven by supply chain disruptions, energy prices, and monetary policy.
• **Food prices:** The FAO Food Price Index remained elevated above pre-pandemic levels. Staples increased 20-30% in Sub-Saharan Africa since 2020.
• **Energy prices:** Crude oil and natural gas volatility impacted transportation and production costs globally.
• **US Consumer Prices:** The CPI peaked at ~9.1% year-over-year in June 2022, the highest since 1981.
• **Central bank response:** The Federal Reserve and other central banks raised interest rates aggressively to combat inflation.

The BLS provides detailed price tracking for individual commodities including gasoline (APU000074714) and milk (APU0000709112).
""".trimIndent()

    private val FALLBACK_GENERAL_MARKET = """
**Market Research Dashboard — Overview:**

This dashboard provides real-time data from multiple APIs:

**World Bank ESG Indicators** (displayed in charts):
• GDP Growth Rate — measures global economic performance
• CO₂ Emissions — tracks agricultural greenhouse gas output
• Agricultural Land — monitors farmland as % of total land

**Bureau of Labor Statistics (BLS):**
• US Regular Gasoline Prices (historical time series)
• US Milk Prices (consumer price data)

**AI-Powered Analysis:**
• Uses Gemini AI to analyze trends and provide insights
• Voice-activated queries for hands-free interaction

**Try these voice commands:**
• "Show GDP graph" — displays GDP growth chart
• "Show CO2 graph" — displays emissions chart
• "Show agricultural land" — displays land use chart
• "Describe CO2 emissions" — AI analysis of emissions
• "Show DOW stocks" — top DJIA components with pie chart
""".trimIndent()

    private fun getDefaultDOWStocks(): List<StockData> = listOf(
        StockData("Apple", "AAPL", 9.2),
        StockData("UnitedHealth", "UNH", 7.7),
        StockData("Home Depot", "HD", 6.6),
        StockData("Goldman Sachs", "GS", 5.4),
        StockData("Microsoft", "MSFT", 5.2),
        StockData("Visa", "V", 5.1),
        StockData("Boeing", "BA", 5.1),
        StockData("McDonald's", "MCD", 5.0),
        StockData("3M Co", "MMM", 4.2),
        StockData("J&J", "JNJ", 3.8),
        StockData("The Rest", "OTHER", 42.9)
    )

    private val _apiResponse = MutableStateFlow<String>("")
    val apiResponse: StateFlow<String> = _apiResponse

    fun fetchGasData() {
        viewModelScope.launch {
            val result = BLSApiService.fetchGasPrice()

            result.onSuccess {
                _apiResponse.value = "Gas Price Data Loaded: ${it.dataPoints.take(5)}"
            }.onFailure {
                _apiResponse.value = "Error: ${it.message}"
            }
        }
    }

    fun queryIndicatorAPI(indicator: ESGIndicator) {
        viewModelScope.launch {
            _isChatLoading.value = true

            val result = when (indicator) {
                ESGIndicator.GDP -> WorldBankApiService.fetchGDPData()
                ESGIndicator.CO2 -> WorldBankApiService.fetchCO2Data()
                ESGIndicator.AGRI_LAND -> WorldBankApiService.fetchAgriLandData()
                else -> null
            }

            result?.onSuccess { data ->
                _chatResponse.value = formatChartDataResponse(indicator, data)
            }?.onFailure {
                _chatResponse.value = "API Error: ${it.message ?: "Unknown error"}"
            }

            _isChatLoading.value = false
        }
    }

    private fun formatChartDataResponse(
        indicator: ESGIndicator,
        data: ChartData
    ): String {

        val title = when (indicator) {
            ESGIndicator.GDP -> "GDP Growth"
            ESGIndicator.CO2 -> "CO2 Emissions"
            ESGIndicator.AGRI_LAND -> "Agricultural Land"
            else -> "Data"
        }

        val topPoints = data.dataPoints.take(5)

        return buildString {
            append("**$title (Latest Data):**\n\n")

            topPoints.forEach {
                append("• ${it.year}: ${it.value}\n")
            }
        }
    }
}