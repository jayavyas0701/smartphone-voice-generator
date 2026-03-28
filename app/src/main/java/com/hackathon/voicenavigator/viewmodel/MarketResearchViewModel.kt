package com.hackathon.voicenavigator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.voicenavigator.data.api.OpenAIApiService
import com.hackathon.voicenavigator.data.api.WorldBankApiService
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    fun selectTab(indicator: ESGIndicator) {
        _selectedTab.value = indicator
        loadDataForIndicator(indicator)
    }

    fun loadDataForIndicator(indicator: ESGIndicator) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = when (indicator) {
                ESGIndicator.GDP -> WorldBankApiService.fetchGDPData()
                ESGIndicator.CO2 -> WorldBankApiService.fetchCO2Data()
                ESGIndicator.AGRI_LAND -> WorldBankApiService.fetchAgriLandData()
                ESGIndicator.CO2_PER_CAPITA -> WorldBankApiService.fetchCO2PerCapitaData()
            }

            result.onSuccess { data ->
                when (indicator) {
                    ESGIndicator.GDP -> _gdpData.value = data
                    ESGIndicator.CO2 -> _co2Data.value = data
                    ESGIndicator.AGRI_LAND -> _agriLandData.value = data
                    ESGIndicator.CO2_PER_CAPITA -> _co2PerCapitaData.value = data
                }
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            _isLoading.value = false
        }
    }

    fun loadGDP() = loadDataForIndicator(ESGIndicator.GDP)
    fun loadCO2() = loadDataForIndicator(ESGIndicator.CO2)
    fun loadAgriLand() = loadDataForIndicator(ESGIndicator.AGRI_LAND)

    fun describeCO2Emissions() {
        viewModelScope.launch {
            _isChatLoading.value = true
            val result = OpenAIApiService.describeIndicator(
                "CO2 Emissions",
                "Carbon dioxide emissions, largely by-products of energy production and use, " +
                "account for the largest share of greenhouse gases, which are associated with global warming. " +
                "Anthropogenic carbon dioxide emissions result primarily from fossil fuel combustion and cement manufacturing."
            )
            result.onSuccess { _chatResponse.value = it }
                .onFailure { _chatResponse.value = "Error: ${it.message}" }
            _isChatLoading.value = false
        }
    }

    fun loadDOWStocks() {
        viewModelScope.launch {
            _isChatLoading.value = true
            val result = OpenAIApiService.queryStockAnalysis(
                "Top 10 Stocks of DOW with market percentages in JSON structure"
            )
            result.onSuccess { response ->
                _chatResponse.value = response
                // Parse stock data - use hardcoded fallback data for reliability
                _dowStocks.value = getDefaultDOWStocks()
            }.onFailure {
                _chatResponse.value = "Error: ${it.message}"
                _dowStocks.value = getDefaultDOWStocks()
            }
            _isChatLoading.value = false
        }
    }

    fun queryMarketResearch(prompt: String) {
        viewModelScope.launch {
            _isChatLoading.value = true
            val result = OpenAIApiService.queryMarketResearch(prompt)
            result.onSuccess { _chatResponse.value = it }
                .onFailure { _chatResponse.value = "Error: ${it.message}" }
            _isChatLoading.value = false
        }
    }

    fun clearChatResponse() {
        _chatResponse.value = null
    }

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
}
