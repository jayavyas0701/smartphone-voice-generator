# VoiceNavigator - CMPE 277 Hackathon
## Scalable Voice-Driven Mobile App for Regulatory Compliance and ESG Impact

**Course:** CMPE 277 - Smartphone Application Development, Spring 2026  
**Instructor:** Professor Chandrasekar Vuppalapati  
**Hackathon:** March 28-29, 2026  

---

## Overview

VoiceNavigator is a next-generation, touchless mobile application powered by **voice-first navigation** that delivers two high-impact solutions:

1. **ESG Market Research Navigator** – Real-time ESG data visualization using World Bank APIs with voice-driven graph navigation and AI-powered analysis
2. **California DMV Knowledge Test Preparation** – RAG-based (Retrieval Augmented Generation) compliance learning platform with voice interaction

The app targets enterprise compliance needs for a consulting firm whose tours, travel, and on-demand charter services require employees to stay current with evolving DMV regulations, while also providing ESG/food security analytics capabilities.

---

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Market   │  │    ESG       │  │   DMV Knowledge  │  │
│  │ Research  │  │  Dashboard   │  │   Test Prep      │  │
│  │  Screen   │  │  (Food Sec.) │  │   Screen         │  │
│  └─────┬─────┘  └──────┬───────┘  └────────┬─────────┘  │
│        │               │                    │            │
│  ┌─────┴───────────────┴────────────────────┴─────────┐  │
│  │              VOICE NAVIGATION LAYER                 │  │
│  │  Speech-to-Text (STT) ←→ Command Parser ←→ TTS     │  │
│  └─────────────────────┬───────────────────────────────┘  │
└────────────────────────┼──────────────────────────────────┘
                         │
┌────────────────────────┼──────────────────────────────────┐
│                   VIEWMODEL LAYER                         │
│  ┌────────────────┐ ┌────────────┐ ┌──────────────────┐  │
│  │MarketResearchVM│ │   ESGVM    │ │     DMVVM        │  │
│  └───────┬────────┘ └─────┬──────┘ └────────┬─────────┘  │
└──────────┼────────────────┼─────────────────┼─────────────┘
           │                │                 │
┌──────────┼────────────────┼─────────────────┼─────────────┐
│                      DATA LAYER                           │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ World Bank   │  │  OpenAI API  │  │   BLS API      │  │
│  │ API Service  │  │  (ChatGPT)   │  │   Service      │  │
│  │              │  │  RAG Engine  │  │                 │  │
│  └──────┬───────┘  └──────┬───────┘  └───────┬────────┘  │
└─────────┼─────────────────┼──────────────────┼────────────┘
          │                 │                  │
   ┌──────┴──────┐  ┌──────┴───────┐  ┌───────┴──────┐
   │World Bank   │  │ OpenAI API   │  │  BLS.gov     │
   │REST API     │  │ GPT-3.5/4    │  │  REST API    │
   └─────────────┘  └──────────────┘  └──────────────┘
```

### RAG (Retrieval Augmented Generation) Architecture

```
┌────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Mobile    │     │   Orchestrator   │     │  Data Sources   │
│  App UX    │────→│  (REST/ViewModel)│     │  (PDF Reports)  │
│            │     │                  │     │                 │
│  Voice     │     │  Query →         │     │ • SOFI 2024     │
│  Input     │     │  Knowledge       │     │ • SOFI 2025     │
└────────────┘     │                  │     │ • CA Driver's   │
                   │         ┌────────┤     │   Handbook      │
                   │         │ IR     │     └────────┬────────┘
                   │         │Search  │←────────────→│
                   │         └────────┤   Transform into
                   │                  │   Embeddings (NLP)
                   │  Prompt +        │   [-2,-1,0,1]
                   │  Knowledge →     │   [2,3,4,5]
                   │  Response        │   [6,7,8,9]
                   │         ┌────────┤
                   │         │  LLM   │
                   │         │(GPT)   │
                   │         └────────┤
                   └──────────────────┘
```

### RAG Implementation Details

The app implements a **full vector-based RAG pipeline** matching the professor's architecture:

1. **Data Sources**: PDF content from SOFI 2024, SOFI 2025, and CA Driver's Handbook bundled in-app
2. **Text Extraction**: PdfBox Android extracts raw text from PDF assets
3. **Chunking**: Text is split into ~500-word overlapping chunks (50-word overlap for continuity)
4. **Embedding Generation**: Each chunk is vectorized using OpenAI `text-embedding-ada-002` (1536-dim vectors)
5. **Vector Store**: Embeddings stored in-memory with source metadata
6. **IR Search**: User query is embedded → cosine similarity computed against all chunks → top-5 retrieved
7. **LLM Response**: Retrieved passages + user query sent to GPT-3.5/4 with strict grounding instructions
8. **No Internet Data**: System prompts enforce that responses come ONLY from retrieved document passages

Key file: `RAGEngine.kt` — implements the complete pipeline as a reusable component.

---

### Track 1: ESG Market Research Voice Navigator

| Feature | Voice Command | API/Source |
|---------|--------------|------------|
| GDP Growth Chart | "Show GDP graph" | World Bank: NY.GDP.MKTP.KD.ZG |
| CO2 Emissions Chart | "Show CO2 graph" | World Bank: EN.GHG.CO2.AG.MT.CE.AR5 |
| Agricultural Land Chart | "Show Agri Land graph" | World Bank: AG.LND.AGRI.ZS |
| CO2 Description (AI) | "Describe CO2 emissions" | OpenAI ChatGPT + World Bank metadata |
| DOW Top 10 Stocks | "Top 10 stocks of DOW" | OpenAI ChatGPT (Pie Chart) |
| Milk Price Time Series | "Show milk price" | BLS: APU0000709112 |
| Gas Price Time Series | "Show gas price" | BLS: APU000074714 |
| Food Security Analysis | "Major food security issues" | RAG (SOFI 2024/2025 PDFs) |
| Food Insecurity Comparison | "Compare 2023 vs 2024" | RAG (SOFI Reports) |
| Economic Sustainability | "Economic sustainability" | RAG (SOFI Reports) |
| Social Sustainability | "Social sustainability" | RAG (SOFI Reports) |

### Track 2: California DMV Knowledge Test

| Feature | Voice Command | Source |
|---------|--------------|--------|
| Signaling Signs | "What signaling signs" | CA Driver's Handbook (RAG) |
| BAC Limits | "Blood alcohol concentration limits" | CA Driver's Handbook (RAG) |
| Speed Limits | "Speed limits" | CA Driver's Handbook (RAG) |
| Right-of-Way Rules | "Right of way" | CA Driver's Handbook (RAG) |
| Practice Quiz (10 questions) | UI Button | Built-in question bank |
| Any DMV question | Free-form voice | CA Driver's Handbook (RAG) |

### Voice Navigation Commands

| Command Type | Examples |
|-------------|----------|
| Chart Display | "Show GDP graph", "Show CO2 chart", "Show Agri Land" |
| AI Analysis | "Describe CO2 emissions", "Top 10 DOW stocks" |
| Food Security | "Major food security issues", "Malnutrition in war zones" |
| DMV Queries | "What are signaling signs", "BAC limits", "Speed limits" |
| Navigation | "Go to market research", "Go to DMV", "Go to ESG" |

---

## APIs Used

### 1. World Bank API (ESG Indicators)
- **GDP Growth**: `https://api.worldbank.org/v2/country/WLD/indicator/NY.GDP.MKTP.KD.ZG?format=json`
- **CO2 Emissions**: `https://api.worldbank.org/v2/country/WLD/indicator/EN.GHG.CO2.AG.MT.CE.AR5?format=json`
- **Agricultural Land**: `https://api.worldbank.org/v2/country/WLD/indicator/AG.LND.AGRI.ZS?format=json`
- **CO2 Per Capita**: `https://api.worldbank.org/v2/country/WLD/indicator/EN.ATM.CO2E.PC?format=json`

### 2. OpenAI ChatGPT API (RAG + Analysis)
- Model: GPT-3.5-turbo / GPT-4
- Used for: Food Security RAG, DMV Handbook RAG, Stock Analysis, Indicator Descriptions

### 3. BLS (Bureau of Labor Statistics) API
- **Gas Price**: Series ID `APU000074714`
- **Milk Price**: Series ID `APU0000709112`

### 4. Android Speech APIs
- **Speech-to-Text**: `android.speech.SpeechRecognizer`
- **Text-to-Speech**: `android.speech.tts.TextToSpeech`

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM (Model-View-ViewModel) |
| Networking | OkHttp + Retrofit |
| JSON Parsing | Gson |
| Async | Kotlin Coroutines + Flow |
| Charts | Custom Canvas-based (Compose) |
| Voice | Android Speech Recognition + TTS |
| AI/RAG | OpenAI ChatGPT API |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Project Structure

```
VoiceNavigator/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/hackathon/voicenavigator/
│   │   │   ├── MainActivity.kt                    # Entry point + Navigation + Voice routing
│   │   │   ├── VoiceNavigatorApp.kt               # Application class (PdfBox init)
│   │   │   ├── data/
│   │   │   │   ├── api/
│   │   │   │   │   ├── WorldBankApiService.kt      # World Bank REST client
│   │   │   │   │   ├── OpenAIApiService.kt         # ChatGPT + Embeddings API
│   │   │   │   │   ├── BLSApiService.kt            # BLS price data client
│   │   │   │   │   └── RAGEngine.kt                # ★ Full RAG pipeline (chunk→embed→search→LLM)
│   │   │   │   └── model/
│   │   │   │       └── Models.kt                   # All data models
│   │   │   ├── viewmodel/
│   │   │   │   ├── MarketResearchViewModel.kt      # ESG charts + market data
│   │   │   │   ├── ESGViewModel.kt                 # Food security RAG (vector search)
│   │   │   │   └── DMVViewModel.kt                 # DMV RAG (vector search) + quiz
│   │   │   ├── voice/
│   │   │   │   └── VoiceRecognitionManager.kt      # STT/TTS + command parser
│   │   │   └── ui/
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt                    # SJSU brand colors
│   │   │       │   └── Theme.kt                    # Material 3 theme
│   │   │       ├── components/
│   │   │       │   ├── VoiceButton.kt              # Animated voice FAB
│   │   │       │   ├── Charts.kt                   # LineChart + PieChart (Canvas)
│   │   │       │   └── BottomNavBar.kt             # Navigation + TopBar
│   │   │       └── screens/
│   │   │           ├── MarketResearchScreen.kt     # API tab (GDP/CO2/Agri)
│   │   │           ├── ESGDashboardScreen.kt       # ESG tab (Food Security RAG)
│   │   │           └── DMVScreen.kt                # DMV tab (Handbook RAG + Quiz)
│   │   ├── assets/
│   │   │   ├── food_security_reports.pdf           # ← Place SOFI PDFs here
│   │   │   └── california_driver_handbook.pdf      # ← Place CA handbook here
│   │   └── res/
│   │       └── values/
│   │           ├── strings.xml
│   │           ├── colors.xml
│   │           └── themes.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Physical Android device or emulator (API 26+)

### Steps

1. **Clone/Import the project** into Android Studio

2. **Set your OpenAI API Key** in `MainActivity.kt`:
   ```kotlin
   OpenAIApiService.setApiKey("sk-your-actual-api-key")
   ```

3. **(Optional) Place PDF files** in `app/src/main/assets/`:
   - `food_security_reports.pdf` — SOFI 2024/2025 reports
   - `california_driver_handbook.pdf` — CA DMV handbook
   - If PDFs are not present, the app falls back to pre-extracted text content

4. **(Optional) Set BLS API Key** for higher rate limits:
   ```kotlin
   BLSApiService.setApiKey("your-bls-registration-key")
   ```

5. **Sync Gradle** and **Build** the project

6. **Run** on a physical device (recommended for voice features) or emulator

7. **Grant Permissions** when prompted:
   - Microphone access (for voice navigation)
   - Internet access (automatic)

---

## Voice Commands Quick Reference

Tap the blue microphone button on any screen and say:

**Market Research Tab:**
- "Show GDP graph"
- "Show CO2 graph"  
- "Show agricultural land graph"
- "Describe CO2 emissions"
- "Top 10 stocks of DOW"

**ESG/Food Security Tab:**
- "Major food security issues"
- "Explain malnutrition in war zones"
- "Compare food insecurity 2023 and 2024"

**DMV Tab:**
- "What are signaling signs"
- "Blood alcohol concentration limits"
- "What are speed limits in California"
- "Right of way rules"

---

## Data Sources

1. **World Bank Open Data** - ESG indicators (GDP, CO2, Agricultural Land)
2. **FAO/WHO/UNICEF** - The State of Food Security and Nutrition in the World 2024 & 2025
3. **California DMV** - Driver's Handbook (https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/)
4. **Bureau of Labor Statistics** - Consumer price data (gas, milk)

---

## Key Design Decisions

- **Voice-First UX**: Every screen has a prominent voice button; all major actions are voice-accessible
- **Full RAG Pipeline**: Implements the professor's architecture — PDF → chunk → embed (OpenAI ada-002) → cosine similarity IR search → LLM grounded response. No internet data used for report Q&A.
- **In-Memory Vector Store**: Embeddings stored in memory for fast cosine similarity search at query time
- **Canvas-based Charts**: Custom Compose Canvas charts for zero external chart library dependencies
- **MVVM Architecture**: Clean separation of concerns for scalability and testability
- **Material 3**: Modern Android design language with SJSU brand colors
- **Offline Quiz**: DMV practice quiz works without internet connectivity
- **PdfBox Android**: Used for PDF text extraction from bundled assets

---

## References

1. C. Vuppalapati, *Machine Learning and Artificial Intelligence for Agricultural Economics*. Springer, 2021
2. C. Vuppalapati, *Building Next-Gen ESG Platforms with IoT and AI for Sustainable Development Goals*. CRC Press, 2026
