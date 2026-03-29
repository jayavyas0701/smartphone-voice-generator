# VoiceNavigator — Voice-Driven Mobile App for Regulatory Compliance & ESG Impact

> **CMPE 277 — Smartphone Application Development | Hackathon Project**
> San Jose State University | Spring 2026

A next-generation, touchless Android mobile application powered by voice navigation, built during an internal hackathon initiative at a high-profile consulting firm. The app delivers voice-first interaction across three verticals: **Market Research (Econometrics)**, **ESG Food Security Analysis**, and **California DMV Regulatory Compliance**.

---

## Table of Contents

- [Project Context](#project-context)
- [Key Features](#key-features)
- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Data Sources & APIs](#data-sources--apis)
- [RAG Pipeline](#rag-pipeline)
- [Voice Navigation System](#voice-navigation-system)
- [Freshness Checking System](#freshness-checking-system)
- [Accomplishments vs. Objectives](#accomplishments-vs-objectives)
- [Screenshots](#screenshots)
- [Setup & Installation](#setup--installation)
- [Configuration](#configuration)
- [Build & Run](#build--run)
- [Referenced Documents](#referenced-documents)
- [License](#license)

---

## Project Context

After graduation, the developer joined a high-profile consulting firm that partners with multiple industries, with strong focus areas in **ESG (Environmental, Social, and Governance)**, **econometrics**, and **regulatory compliance**. To accelerate innovation and respond to rising demand for scalable, enterprise-grade mobile solutions, the firm launched an internal hackathon-style initiative — aimed at building a next-generation, touchless mobile app experience powered by voice navigation.

The developer was selected as a core contributor and assigned **two high-impact challenge tracks**:

1. **An ESG-focused solution** leveraging real-world datasets (including food security insights from FAO/IFAD/UNICEF/WFP/WHO SOFI reports)
2. **A mission-critical California DMV Knowledge Test** mobile application designed for compliance-driven industries

The company's tours, travel, and on-demand charter services depend on ensuring that employees stay up to date with continuously evolving DMV rules — creating an urgent need for a smart, accessible, and continuously updated learning platform.

---

## Key Features

### Voice-First Interaction
- Full **voice command navigation** across all three screens (API, ESG, DMV)
- **Speech-to-Text** via Android `SpeechRecognizer` for hands-free queries
- **Text-to-Speech (TTS)** readback of AI responses for accessibility
- **Natural language voice command parser** that routes queries to the correct screen and action
- Listen/Stop toggle with visual feedback

### Market Research & Econometrics (API Screen)
- **Real-time World Bank API** integration for global ESG indicators:
   - GDP Growth Rate (`NY.GDP.MKTP.KD.ZG`)
   - CO₂ Emissions (`EN.ATM.CO2E.KT`)
   - Agricultural Land Use (`AG.LND.AGRI.ZS`)
   - CO₂ Per Capita (`EN.ATM.CO2E.PC`)
- **Bureau of Labor Statistics (BLS) API** for US consumer price data:
   - Regular Gasoline Prices (`APU000074714`)
   - Milk Prices (`APU0000709112`)
- **Interactive line charts** with Jetpack Compose Canvas rendering
- **DOW Jones pie chart** visualization of top 10 stock weightings
- **Gemini AI-powered analysis** with automatic descriptions for each indicator
- Static chart fallbacks for offline/timeout resilience

### ESG Food Security Analysis (ESG Screen)
- **RAG (Retrieval Augmented Generation)** pipeline powered by Gemini 2.5 Flash
- Ingests data from official **FAO SOFI 2023 & SOFI 2024** reports
- **7 preset quick-query buttons** mapping to key report topics:
   - Major food insecurity reasons in 2024
   - Malnutrition in war zones
   - Price impact on food security
   - 2023 vs 2024 comparison
   - Quantitative differences in hunger/malnutrition numbers
   - Economic sustainability statements
   - Social sustainability statements
- **Source citation system** — every AI response includes SOFI report section attribution
- **Hardcoded expert fallback responses** guarantee the app works even with zero API quota
- Voice queries routed to the ESG knowledge base

### California DMV Knowledge Test (DMV Screen)
- **Direct prompt-stuffing RAG** with the full California Driver's Handbook (DL-600, Rev. 6/2025)
- **Handbook Q&A mode** — ask any question about California driving rules
- **Practice Quiz mode** — 10-question multiple choice with:
   - Category badges (Speed Limits, DUI Laws, Parking, Traffic Signs, etc.)
   - Progress bar and running score
   - Correct/incorrect highlighting with explanations
   - Pass/fail result (70% threshold)
- **4 preset quick-query buttons**: Signaling Signs, BAC Limits, Speed Limits, Right-of-Way
- **15+ topic-specific fallback responses** covering every major handbook section
- Voice queries routed to the DMV knowledge base

### Freshness Checking & Source Attribution
- **HTTP HEAD freshness detection** — on app launch, fires zero-byte requests to official document URLs
- Reads `Last-Modified` headers and compares against stored fingerprints in `SharedPreferences`
- **Source attribution cards** on DMV and ESG screens showing:
   - Document source name and revision
   - Live last-updated timestamp from the server
   - Tappable link to the official source document
- **Update notification banner** (orange, dismissible) appears when a newer document version is detected
- DMV links to: `https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/`
- ESG links to: `https://openknowledge.fao.org/handle/20.500.14283/cd1254en`

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer                 │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────────┐  │
│  │ MarketRe-   │ │ ESGDashboard │ │  DMVScreen   │  │
│  │ searchScreen│ │ Screen       │ │  (Q&A+Quiz)  │  │
│  └──────┬──────┘ └──────┬───────┘ └──────┬───────┘  │
│         │               │                │           │
│  ┌──────┴──────┐ ┌──────┴───────┐ ┌──────┴───────┐  │
│  │ MarketRe-   │ │  ESGView-    │ │  DMVView-    │  │
│  │ searchVM    │ │  Model       │ │  Model       │  │
│  └──────┬──────┘ └──────┬───────┘ └──────┬───────┘  │
├─────────┴───────────────┴────────────────┴───────────┤
│                     Data Layer                        │
│  ┌────────────┐ ┌────────────┐ ┌──────────────────┐  │
│  │ WorldBank  │ │ BLS API    │ │ GeminiApiService │  │
│  │ ApiService │ │ Service    │ │ (LLM + Embed)    │  │
│  └────────────┘ └────────────┘ └──────────────────┘  │
│  ┌────────────┐ ┌────────────────────────────────┐   │
│  │ Freshness  │ │ RAGEngine (TF-IDF + Gemini     │   │
│  │ Checker    │ │ embeddings + cosine similarity) │   │
│  └────────────┘ └────────────────────────────────┘   │
├──────────────────────────────────────────────────────┤
│                    Voice Layer                        │
│  ┌─────────────────────┐ ┌─────────────────────────┐ │
│  │ VoiceRecognition    │ │ VoiceCommandParser      │ │
│  │ Manager (STT + TTS) │ │ (NLP intent routing)    │ │
│  └─────────────────────┘ └─────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

### Design Patterns
- **MVVM (Model-View-ViewModel)** — clean separation of UI, business logic, and data
- **Repository pattern** — API services abstract data sources from ViewModels
- **StateFlow + Compose** — reactive, unidirectional data flow
- **Strategy pattern** — voice commands parsed and routed via sealed class hierarchy
- **Fallback chain** — API call → cache check → hardcoded expert response (never fails)

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 1.9.22 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM + StateFlow |
| **Build System** | Gradle 8.13.2, AGP, Android SDK 34 |
| **Min SDK** | API 26 (Android 8.0) |
| **LLM** | Google Gemini 2.5 Flash Lite (via REST API) |
| **Embeddings** | Gemini text-embedding-004 + local TF-IDF fallback |
| **HTTP Client** | OkHttp 4.12 + HttpURLConnection |
| **JSON** | Gson 2.10.1 + org.json |
| **PDF Parsing** | PdfBox-Android 2.0.27.0 |
| **Voice** | Android SpeechRecognizer + TextToSpeech |
| **Image Loading** | Coil 2.5.0 |
| **Navigation** | Jetpack Navigation Compose 2.7.6 |
| **Coroutines** | kotlinx-coroutines-android 1.7.3 |

---

## Project Structure

```
com.hackathon.voicenavigator/
├── MainActivity.kt                    # Entry point, voice manager, navigation
├── VoiceNavigatorApp.kt               # Application class
│
├── data/
│   ├── api/
│   │   ├── GeminiApiService.kt        # Gemini LLM + embedding API client
│   │   ├── WorldBankApiService.kt     # World Bank ESG indicator API
│   │   ├── BLSApiService.kt          # Bureau of Labor Statistics API
│   │   ├── RAGEngine.kt              # RAG pipeline (chunk → embed → search → generate)
│   │   ├── FreshnessChecker.kt       # HTTP HEAD document freshness detection
│   │   └── OpenAIApiService.kt       # Deprecated — migrated to Gemini
│   └── model/
│       └── Models.kt                  # Data classes (ChatMessage, ChartData, DMVQuestion, etc.)
│
├── viewmodel/
│   ├── MarketResearchViewModel.kt     # Econometrics: World Bank + BLS + AI analysis
│   ├── ESGViewModel.kt               # Food security RAG + SOFI report analysis
│   └── DMVViewModel.kt               # DMV handbook RAG + quiz engine
│
├── ui/
│   ├── screens/
│   │   ├── MarketResearchScreen.kt    # API tab — charts, AI analysis, voice
│   │   ├── ESGDashboardScreen.kt      # ESG tab — food security Q&A
│   │   └── DMVScreen.kt              # DMV tab — handbook Q&A + practice quiz
│   ├── components/
│   │   ├── LineChart.kt              # Compose Canvas line chart
│   │   ├── PieChart.kt              # DOW Jones stock visualization
│   │   ├── VoiceButton.kt           # Animated microphone button
│   │   ├── SourceInfoCard.kt        # Source attribution + freshness banner
│   │   └── AppBottomNavBar.kt       # Bottom navigation (API/ESG/DMV)
│   └── theme/
│       └── Theme.kt                  # Material 3 color palette & typography
│
└── voice/
    ├── VoiceRecognitionManager.kt     # STT + TTS lifecycle management
    └── VoiceCommandParser.kt          # Natural language → intent routing
```

---

## Data Sources & APIs

### Live APIs (Real-Time Data)
| API | Endpoint | Data |
|-----|----------|------|
| **World Bank** | `api.worldbank.org/v2/country/WLD/indicator/` | GDP growth, CO₂, agricultural land, CO₂/capita |
| **BLS** | `api.bls.gov/publicAPI/v2/timeseries/data/` | US gasoline & milk prices (1992–2026) |
| **Gemini** | `generativelanguage.googleapis.com/v1beta/` | LLM chat completion + text embeddings |

### Reference Documents (Curated RAG Sources)
| Document | Source | Usage |
|----------|--------|-------|
| **DL-600-R6-2025** | California DMV | Driver's Handbook — full text for DMV Q&A and quiz |
| **cd1254en.pdf** | FAO/IFAD/UNICEF/WFP/WHO | SOFI 2024 — State of Food Security & Nutrition |
| **cc3017en.pdf** | FAO/IFAD/UNICEF/WFP/WHO | SOFI 2023 — State of Food Security & Nutrition |

---

## RAG Pipeline

The application implements a dual-strategy RAG system:

### Strategy 1: Direct Prompt-Stuffing (DMV & ESG screens)
- Full document text embedded as constants in ViewModels
- Sent directly to Gemini with each user query in one API call
- Gemini's 1M token context window handles the full documents comfortably
- Zero embedding API dependency — works reliably every time

### Strategy 2: Vector-Based RAG (RAGEngine.kt)
- **Chunking**: Text split into 500-word chunks with 50-word overlap
- **Embedding**: Gemini `text-embedding-004` (primary) or local TF-IDF (fallback)
- **Storage**: In-memory vector store per source
- **Retrieval**: Cosine similarity search, top-K=5 chunks
- **Generation**: Retrieved context + user query → Gemini LLM → response

### Resilience Chain
Every query follows a three-tier fallback:
1. **Live API** — Gemini generates a response from document context
2. **Response Cache** — normalized query → cached response (same session)
3. **Hardcoded Expert Fallback** — pre-written, data-accurate responses for all preset topics

This guarantees the app **always shows content** regardless of API availability or quota status.

---

## Voice Navigation System

### Supported Voice Commands

| Command Pattern | Action |
|----------------|--------|
| "Show GDP graph" | Navigate to API tab, display GDP chart |
| "Show CO2 graph" | Navigate to API tab, display CO₂ chart |
| "Show agricultural land" | Navigate to API tab, display agriculture chart |
| "Show DOW stocks" | Display DOW Jones top 10 pie chart |
| "Describe CO2 emissions" | AI analysis of CO₂ indicator |
| "What are the signaling signs?" | Navigate to DMV tab, query handbook |
| "Show BAC limits" | Navigate to DMV tab, query BAC rules |
| "Major food security issues" | Navigate to ESG tab, query SOFI data |
| "Go to DMV / ESG / Market Research" | Tab navigation |
| Any free-form question | Routed to current tab's knowledge base |

### Voice Architecture
```
User speaks → SpeechRecognizer → VoiceCommandParser.parse()
    → Sealed class VoiceCommand matched
    → Route to correct ViewModel function
    → Response displayed on screen
    → Optional TTS readback via "Read Aloud" button
```

---

## Freshness Checking System

The app proactively monitors source documents for updates using a zero-cost HTTP HEAD strategy:

1. **On app launch**: fires HEAD requests to DMV handbook and SOFI report URLs
2. **Reads `Last-Modified` header** (downloads zero bytes)
3. **Compares against stored fingerprint** in `SharedPreferences`
4. **Displays results**: source card shows last-updated timestamp; orange banner appears if newer version detected
5. **User acknowledgment**: dismissing the banner stores the new fingerprint

This demonstrates a **continuously updated learning platform** that can detect when regulatory documents change — directly addressing the project requirement for employees to stay current with evolving DMV rules.

---

## Accomplishments vs. Objectives

| Project Objective | Implementation | Status |
|-------------------|---------------|--------|
| **Voice-first, touchless interaction** | Full STT + TTS + voice command parser across all 3 screens | ✅ Delivered |
| **ESG-focused solution with real-world datasets** | SOFI 2023 & 2024 report data, World Bank API, BLS API | ✅ Delivered |
| **California DMV Knowledge Test application** | Full handbook Q&A + 10-question practice quiz with scoring | ✅ Delivered |
| **Regulatory compliance** | DMV handbook Rev. 6/2025 (DL-600), source verification links | ✅ Delivered |
| **Scalable, enterprise-grade mobile solution** | MVVM architecture, Compose, StateFlow, coroutines | ✅ Delivered |
| **Continuously updated learning platform** | HTTP HEAD freshness checking, update banners, live timestamps | ✅ Delivered |
| **Intelligent automation** | Gemini AI-powered RAG, automatic indicator analysis, fallback chains | ✅ Delivered |
| **Econometrics focus** | World Bank GDP/CO₂/agriculture charts, BLS price data, AI trend analysis | ✅ Delivered |
| **Food security insights** | FAO SOFI report RAG with 7 preset queries + free-form Q&A | ✅ Delivered |
| **Rapid prototyping (hackathon pace)** | End-to-end app delivered within hackathon timeline | ✅ Delivered |
| **Multiple data source integration** | World Bank + BLS + Gemini + SOFI PDFs + DMV Handbook | ✅ Delivered |
| **Offline resilience** | Response caching + hardcoded fallbacks + static chart data | ✅ Delivered |
| **Source transparency** | Citation system, source cards, tappable links to official documents | ✅ Delivered |

---

## Screenshots

| API (Market Research) | ESG (Food Security) | DMV (Knowledge Test) |
|:---------------------:|:-------------------:|:--------------------:|
| World Bank charts + AI analysis | SOFI report RAG Q&A | Handbook Q&A + Practice Quiz |
| GDP / CO₂ / Agri Land tabs | 7 preset query buttons | Signaling / BAC / Speed / ROW |
| DOW Jones pie chart | Source citations | Score tracking + explanations |

---

## Setup & Installation

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- An Android device or emulator (API 26+)

### Clone & Open
```bash
git clone <repository-url>
cd VoiceNavigator
```
Open the project in Android Studio.

---

## Configuration

### Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/apikey)
2. Click **"Create API key"** → select a project
3. Copy the key
4. Add to `local.properties`:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

The key is loaded at build time via `BuildConfig.GEMINI_API_KEY`.

> **Note**: The app works without a valid API key — all preset buttons have hardcoded fallback responses. The API key enables live Gemini AI responses for free-form queries.

### BLS API Key (Optional)

For higher BLS rate limits, register at [bls.gov](https://data.bls.gov/registrationEngine/) and set the key in `BLSApiService.kt`.

---

## Build & Run

```bash
# Clean build
./gradlew clean assembleDebug

# Or from Android Studio:
# Build → Clean Project → Rebuild Project → Run
```

### Permissions Required
- `INTERNET` — API calls to World Bank, BLS, Gemini
- `RECORD_AUDIO` — Voice recognition (requested at runtime)
- `ACCESS_NETWORK_STATE` — Network availability checks

---

## Referenced Documents

1. **DL-600-R6-2025** — California Driver's Handbook, Rev. 6/2025. California Department of Motor Vehicles. Available at: [dmv.ca.gov](https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/)

2. **cd1254en** — The State of Food Security and Nutrition in the World 2024 (SOFI 2024). FAO, IFAD, UNICEF, WFP and WHO. Rome, FAO. Available at: [openknowledge.fao.org](https://openknowledge.fao.org/handle/20.500.14283/cd1254en)

3. **cc3017en** — The State of Food Security and Nutrition in the World 2023 (SOFI 2023). FAO, IFAD, UNICEF, WFP and WHO. Rome, FAO. Available at: [openknowledge.fao.org](https://openknowledge.fao.org/handle/20.500.14283/cc3017en)

4. **CMPE277_VoiceNavigator_WireFrames_20260328** — Project wireframes and UI specifications.

5. **CMPE277_Hackathon_Innovation_Scalable_Voice-Driven_Mobile_Apps_for_Regulatory_Compliance_and_ESG_Impact** — Full project description and requirements document.

---

## License

```
MIT License

Copyright (c) 2026 VoiceNavigator

Permission is granted to use, modify, and distribute this software for any purpose, with or without attribution.

This software is provided "as is", without warranty of any kind. The authors are not liable for any damages arising from its use.
```

### Third-Party Data Attribution

- **World Bank Open Data** — Licensed under [Creative Commons Attribution 4.0 (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/)
- **Bureau of Labor Statistics** — U.S. Government public domain data
- **FAO SOFI Reports** — Licensed under [Creative Commons Attribution-NonCommercial-ShareAlike 3.0 IGO (CC BY-NC-SA 3.0 IGO)](https://creativecommons.org/licenses/by-nc-sa/3.0/igo/)
- **California DMV Driver's Handbook** — California Department of Motor Vehicles, State of California public document
- **Google Gemini API** — Subject to [Google AI Terms of Service](https://ai.google.dev/terms)

---

> Built with Kotlin, Jetpack Compose, and Gemini AI for CMPE 277 — San Jose State University, Spring 2026