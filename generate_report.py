#!/usr/bin/env python3
"""Generate a PDF project report for VoiceNavigator."""

from fpdf import FPDF
from datetime import datetime


class ReportPDF(FPDF):
    def header(self):
        if self.page_no() > 1:
            self.set_font("Helvetica", "I", 8)
            self.set_text_color(100, 100, 100)
            self.cell(0, 10, "VoiceNavigator - CMPE 277 Hackathon Project Report", align="C")
            self.ln(5)
            self.set_draw_color(0, 84, 159)
            self.set_line_width(0.3)
            self.line(10, self.get_y(), 200, self.get_y())
            self.ln(5)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")

    def section_title(self, title):
        self.set_font("Helvetica", "B", 14)
        self.set_text_color(0, 84, 159)
        self.ln(4)
        self.cell(0, 10, title)
        self.ln(8)
        self.set_draw_color(0, 84, 159)
        self.set_line_width(0.4)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(6)

    def subsection_title(self, title):
        self.set_font("Helvetica", "B", 11)
        self.set_text_color(40, 40, 40)
        self.ln(2)
        self.cell(0, 8, title)
        self.ln(8)

    def body_text(self, text):
        self.set_font("Helvetica", "", 10)
        self.set_text_color(50, 50, 50)
        self.multi_cell(0, 5.5, text)
        self.ln(2)

    def bullet(self, text):
        self.set_font("Helvetica", "", 10)
        self.set_text_color(50, 50, 50)
        x = self.get_x()
        self.cell(8, 5.5, "-")
        self.multi_cell(0, 5.5, text)
        self.ln(1)

    def bold_bullet(self, bold_part, rest):
        x = self.get_x()
        self.cell(8, 5.5, "-")
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(50, 50, 50)
        w = self.get_string_width(bold_part)
        self.cell(w, 5.5, bold_part)
        self.set_font("Helvetica", "", 10)
        self.multi_cell(0, 5.5, rest)
        self.ln(1)

    def table_row(self, col1, col2, bold=False):
        self.set_font("Helvetica", "B" if bold else "", 9)
        self.set_text_color(50, 50, 50)
        if bold:
            self.set_fill_color(0, 84, 159)
            self.set_text_color(255, 255, 255)
        else:
            self.set_fill_color(245, 245, 245) if self.table_alt else self.set_fill_color(255, 255, 255)
        self.cell(60, 7, col1, border=1, fill=True)
        self.cell(0, 7, col2, border=1, fill=True)
        self.ln()
        if not bold:
            self.table_alt = not self.table_alt

    table_alt = False


def generate():
    pdf = ReportPDF()
    pdf.alias_nb_pages()
    pdf.set_auto_page_break(auto=True, margin=20)

    # ── Cover Page ──────────────────────────────────────────────
    pdf.add_page()
    pdf.ln(40)
    pdf.set_font("Helvetica", "B", 28)
    pdf.set_text_color(0, 84, 159)
    pdf.cell(0, 15, "VoiceNavigator", align="C")
    pdf.ln(18)
    pdf.set_font("Helvetica", "", 16)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(0, 10, "Scalable Voice-Driven Mobile App for", align="C")
    pdf.ln(10)
    pdf.cell(0, 10, "Regulatory Compliance and ESG Impact", align="C")
    pdf.ln(20)
    pdf.set_draw_color(0, 84, 159)
    pdf.set_line_width(0.8)
    pdf.line(60, pdf.get_y(), 150, pdf.get_y())
    pdf.ln(20)
    pdf.set_font("Helvetica", "", 12)
    pdf.set_text_color(60, 60, 60)
    pdf.cell(0, 8, "CMPE 277 - Smartphone Application Development", align="C")
    pdf.ln(8)
    pdf.cell(0, 8, "Spring 2026 Hackathon (March 28-29, 2026)", align="C")
    pdf.ln(8)
    pdf.cell(0, 8, "Professor Chandrasekar Vuppalapati", align="C")
    pdf.ln(20)
    pdf.set_font("Helvetica", "I", 10)
    pdf.set_text_color(120, 120, 120)
    pdf.cell(0, 8, f"Report generated: {datetime.now().strftime('%B %d, %Y at %I:%M %p')}", align="C")

    # ── 1. Executive Summary ────────────────────────────────────
    pdf.add_page()
    pdf.section_title("1. Executive Summary")
    pdf.body_text(
        "VoiceNavigator is a next-generation, touchless Android mobile application powered by "
        "voice-first navigation. Built with Kotlin and Jetpack Compose, it delivers two high-impact "
        "solutions: (1) an ESG Market Research Navigator with real-time data visualization from "
        "World Bank and BLS APIs, and (2) a California DMV Knowledge Test Preparation platform "
        "using RAG (Retrieval Augmented Generation) architecture."
    )
    pdf.body_text(
        "The app targets enterprise compliance needs for a consulting firm whose tours, travel, "
        "and on-demand charter services require employees to stay current with evolving DMV "
        "regulations, while also providing ESG and food security analytics capabilities."
    )
    pdf.body_text(
        "Key achievements include a fully functional voice navigation system, real-time API data "
        "visualization with custom Canvas-based charts, a complete RAG pipeline with both neural "
        "and TF-IDF embeddings, and robust offline fallback for all features."
    )

    # ── 2. Tech Stack ───────────────────────────────────────────
    pdf.section_title("2. Technology Stack")
    pdf.table_alt = False
    pdf.table_row("Component", "Technology", bold=True)
    rows = [
        ("Language", "Kotlin"),
        ("UI Framework", "Jetpack Compose + Material 3"),
        ("Architecture", "MVVM (Model-View-ViewModel)"),
        ("Networking", "OkHttp + Retrofit + HttpURLConnection"),
        ("JSON Parsing", "Gson + org.json"),
        ("Async", "Kotlin Coroutines + StateFlow"),
        ("Charts", "Custom Canvas-based (Compose)"),
        ("Voice", "Android SpeechRecognizer + TextToSpeech"),
        ("AI / LLM", "Google Gemini 2.0 Flash"),
        ("Embeddings", "Gemini text-embedding-004 + TF-IDF fallback"),
        ("PDF Extraction", "PdfBox Android 2.0.27"),
        ("Min SDK", "26 (Android 8.0)"),
        ("Target SDK", "34 (Android 14)"),
    ]
    for c1, c2 in rows:
        pdf.table_row(c1, c2)

    # ── 3. Architecture ─────────────────────────────────────────
    pdf.section_title("3. System Architecture")
    pdf.subsection_title("3.1 MVVM Architecture")
    pdf.body_text(
        "The app follows the Model-View-ViewModel pattern with clear separation of concerns:"
    )
    pdf.bold_bullet("Presentation Layer: ", "Jetpack Compose screens (MarketResearchScreen, ESGDashboardScreen, DMVScreen) with reusable components (VoiceButton, Charts, BottomNavBar).")
    pdf.bold_bullet("ViewModel Layer: ", "MarketResearchViewModel, ESGViewModel, DMVViewModel manage UI state via Kotlin StateFlow, handle API calls, caching, and fallback logic.")
    pdf.bold_bullet("Data Layer: ", "API services (GeminiApiService, WorldBankApiService, BLSApiService) and the RAGEngine handle all external data fetching and processing.")
    pdf.bold_bullet("Voice Layer: ", "VoiceRecognitionManager handles STT/TTS, VoiceCommandParser maps speech to sealed-class commands routed through MainActivity.")

    pdf.subsection_title("3.2 RAG (Retrieval Augmented Generation) Pipeline")
    pdf.body_text(
        "The RAGEngine implements a full vector-based retrieval pipeline matching the professor's architecture:"
    )
    pdf.bullet("Data Sources: SOFI 2024/2025 food security reports and CA Driver's Handbook content bundled in-app.")
    pdf.bullet("Text Chunking: ~500-word chunks with 50-word overlap for context continuity.")
    pdf.bullet("Embedding Generation: Primary strategy uses Gemini text-embedding-004 (768-dim vectors). Falls back to local TF-IDF sparse vectors (512-dim) when API is unavailable.")
    pdf.bullet("Vector Store: In-memory storage with source metadata for fast retrieval.")
    pdf.bullet("IR Search: Query embedding compared via cosine similarity against all chunks; top-5 retrieved.")
    pdf.bullet("LLM Response: Retrieved passages + user query sent to Gemini 2.0 Flash with strict grounding instructions to prevent hallucination.")

    pdf.subsection_title("3.3 Voice Navigation Architecture")
    pdf.body_text(
        "The voice-first UX is implemented through a centralized command routing system:"
    )
    pdf.bullet("Speech-to-Text (STT): Android SpeechRecognizer with partial results for live feedback.")
    pdf.bullet("Command Parser: VoiceCommandParser uses keyword matching to map speech into a sealed class hierarchy (25+ distinct commands).")
    pdf.bullet("Text-to-Speech (TTS): AI responses are read aloud automatically (truncated at 400 chars for UX).")
    pdf.bullet("Command routing in MainActivity dispatches to the correct ViewModel and navigates to the appropriate tab.")

    # ── 4. Features ─────────────────────────────────────────────
    pdf.add_page()
    pdf.section_title("4. Features Implemented")

    pdf.subsection_title("4.1 Track 1: ESG Market Research Voice Navigator")
    pdf.body_text("Real-time ESG data visualization with voice-driven graph navigation and AI-powered analysis:")
    pdf.bullet("GDP Growth Chart - World Bank API (NY.GDP.MKTP.KD.ZG) with line chart visualization.")
    pdf.bullet("CO2 Emissions Chart - World Bank API (EN.ATM.CO2E.KT) with trend analysis.")
    pdf.bullet("Agricultural Land Chart - World Bank API (AG.LND.AGRI.ZS) showing global land-use trends.")
    pdf.bullet("AI-generated descriptions for each indicator via Gemini, with hardcoded fallback analysis.")
    pdf.bullet("DOW Top 10 Stocks - Pie chart visualization with Gemini-powered stock analysis.")
    pdf.bullet("Gas Price Time Series - BLS API (APU000074714) historical data from 1992-2026.")
    pdf.bullet("Milk Price Time Series - BLS API (APU0000709112) historical consumer price data.")
    pdf.bullet("Food Security Analysis - RAG-based Q&A from SOFI 2024/2025 reports with 7 preset query buttons.")

    pdf.subsection_title("4.2 Track 2: California DMV Knowledge Test Preparation")
    pdf.body_text("RAG-based compliance learning platform with voice interaction:")
    pdf.bullet("Handbook Q&A - Voice or button-driven queries against CA Driver's Handbook content.")
    pdf.bullet("4 preset quick-query buttons: Signaling Signs, BAC Limits, Speed Limits, Right-of-Way.")
    pdf.bullet("Free-form voice queries for any DMV topic (parking, headlights, DUI penalties, etc.).")
    pdf.bullet("15+ topic-specific fallback responses covering all major handbook sections.")
    pdf.bullet("Practice Quiz - 10 multiple-choice questions with scoring, explanations, and progress tracking.")
    pdf.bullet("Quiz covers: Speed Limits, DUI Laws, Parking, Traffic Signs, Signaling, Right-of-Way, Safe Driving.")

    pdf.subsection_title("4.3 Voice Navigation")
    pdf.body_text("25+ voice commands supported across all screens:")
    pdf.bullet("Chart commands: 'Show GDP graph', 'Show CO2 chart', 'Show agricultural land'")
    pdf.bullet("AI analysis: 'Describe CO2 emissions', 'Top 10 stocks of DOW'")
    pdf.bullet("Food security: 'Major food security issues', 'Compare 2023 vs 2024'")
    pdf.bullet("DMV queries: 'What are signaling signs', 'BAC limits', 'Speed limits'")
    pdf.bullet("Navigation: 'Go to market research', 'Go to DMV', 'Go to ESG', 'Go home'")

    # ── 5. APIs ─────────────────────────────────────────────────
    pdf.section_title("5. APIs and External Services")

    pdf.subsection_title("5.1 Google Gemini API")
    pdf.bullet("Model: gemini-2.0-flash for chat completions (v1beta endpoint).")
    pdf.bullet("Embeddings: text-embedding-004 for RAG vector generation.")
    pdf.bullet("Used for: ESG indicator analysis, food security RAG, DMV handbook RAG, stock analysis, market research queries.")
    pdf.bullet("Quota handling: Automatic retry with exponential backoff, quota exhaustion detection with cooldown timer.")

    pdf.subsection_title("5.2 World Bank API")
    pdf.bullet("GDP Growth: NY.GDP.MKTP.KD.ZG - Annual percentage growth rate.")
    pdf.bullet("CO2 Emissions: EN.ATM.CO2E.KT - Carbon dioxide emissions in kilotonnes.")
    pdf.bullet("Agricultural Land: AG.LND.AGRI.ZS - Percentage of land area.")
    pdf.bullet("10-second timeout with static fallback chart data for offline resilience.")

    pdf.subsection_title("5.3 Bureau of Labor Statistics (BLS) API")
    pdf.bullet("Gas Price: Series APU000074714 - US Regular All Formulations.")
    pdf.bullet("Milk Price: Series APU0000709112 - Whole milk per gallon.")
    pdf.bullet("Fetches in 20-year chunks to work within BLS API limits.")

    pdf.subsection_title("5.4 Android Speech APIs")
    pdf.bullet("SpeechRecognizer for voice-to-text with partial results.")
    pdf.bullet("TextToSpeech for reading AI responses aloud.")

    # ── 6. Project Structure ────────────────────────────────────
    pdf.add_page()
    pdf.section_title("6. Project Structure")
    pdf.set_font("Courier", "", 8.5)
    pdf.set_text_color(50, 50, 50)
    structure = [
        "app/src/main/",
        "  AndroidManifest.xml",
        "  java/com/hackathon/voicenavigator/",
        "    MainActivity.kt              - Entry point, navigation, voice routing",
        "    VoiceNavigatorApp.kt          - Application class (PdfBox init)",
        "    data/",
        "      api/",
        "        GeminiApiService.kt       - Gemini 2.0 Flash + embeddings API",
        "        WorldBankApiService.kt     - World Bank REST client",
        "        BLSApiService.kt           - Bureau of Labor Statistics client",
        "        OpenAIApiService.kt        - (Legacy, not currently used)",
        "        RAGEngine.kt              - Full RAG pipeline: chunk/embed/search/LLM",
        "      model/",
        "        Models.kt                 - All data models and enums",
        "    viewmodel/",
        "      MarketResearchViewModel.kt  - ESG charts, market data, caching",
        "      ESGViewModel.kt             - Food security RAG + fallbacks",
        "      DMVViewModel.kt             - DMV RAG + quiz + fallbacks",
        "    voice/",
        "      VoiceRecognitionManager.kt  - STT/TTS + VoiceCommandParser",
        "    ui/",
        "      theme/   Color.kt, Theme.kt - SJSU brand colors, Material 3",
        "      components/",
        "        VoiceButton.kt            - Animated voice FAB",
        "        Charts.kt                 - LineChart + PieChart (Canvas)",
        "        BottomNavBar.kt           - Navigation bar + TopBar",
        "      screens/",
        "        MarketResearchScreen.kt   - API tab (GDP/CO2/Agri/BLS)",
        "        ESGDashboardScreen.kt     - ESG tab (Food Security RAG)",
        "        DMVScreen.kt              - DMV tab (Handbook RAG + Quiz)",
    ]
    for line in structure:
        pdf.cell(0, 4.5, line)
        pdf.ln()
    pdf.ln(4)

    # ── 7. Design Decisions ─────────────────────────────────────
    pdf.section_title("7. Key Design Decisions")
    pdf.bold_bullet("Voice-First UX: ", "Every screen has a prominent voice button; all major actions are voice-accessible. TTS reads back AI responses for a touchless experience.")
    pdf.bold_bullet("Dual Embedding Strategy: ", "Gemini neural embeddings (768-dim) as primary, with automatic fallback to local TF-IDF (512-dim). Ensures RAG works even without API quota.")
    pdf.bold_bullet("Comprehensive Fallbacks: ", "Every API call and AI query has hardcoded fallback responses. The app is fully functional offline for demos.")
    pdf.bold_bullet("Response Caching: ", "In-memory caching in each ViewModel prevents redundant API calls and ensures instant responses on repeated queries.")
    pdf.bold_bullet("Custom Canvas Charts: ", "Zero external charting library dependencies. LineChart and PieChart rendered directly on Compose Canvas.")
    pdf.bold_bullet("Material 3 Theming: ", "Modern Android design with SJSU brand colors (blue/gold) and consistent card-based UI.")
    pdf.bold_bullet("Inline Knowledge Base: ", "ESG and DMV content embedded as string constants for reliable, instant access without PDF parsing at runtime.")

    # ── 8. Dependencies ─────────────────────────────────────────
    pdf.section_title("8. Dependencies")
    pdf.table_alt = False
    pdf.table_row("Library", "Purpose", bold=True)
    deps = [
        ("androidx.compose.*", "Jetpack Compose UI framework"),
        ("androidx.navigation", "Compose navigation"),
        ("androidx.lifecycle", "ViewModel + lifecycle management"),
        ("com.squareup.retrofit2", "REST API client (BLS)"),
        ("com.squareup.okhttp3", "HTTP client + logging"),
        ("com.google.code.gson", "JSON serialization"),
        ("kotlinx.coroutines", "Asynchronous programming"),
        ("com.tom-roush:pdfbox-android", "PDF text extraction"),
        ("io.coil-kt:coil-compose", "Image loading (available)"),
        ("androidx.webkit", "WebView support"),
    ]
    for c1, c2 in deps:
        pdf.table_row(c1, c2)

    # ── 9. How to Run ───────────────────────────────────────────
    pdf.add_page()
    pdf.section_title("9. Setup and Running")
    pdf.bullet("Prerequisites: Android Studio Hedgehog+, JDK 17, Android SDK 34, physical device or emulator (API 26+).")
    pdf.bullet("Clone/import the project into Android Studio.")
    pdf.bullet("Set Gemini API key in local.properties: GEMINI_API_KEY=your-key-here")
    pdf.bullet("(Optional) Place PDF files in app/src/main/assets/ for full RAG pipeline.")
    pdf.bullet("(Optional) Set BLS API key for higher rate limits.")
    pdf.bullet("Sync Gradle, build, and run on device. Grant microphone permission when prompted.")

    # ── 10. References ──────────────────────────────────────────
    pdf.section_title("10. References")
    pdf.bullet("C. Vuppalapati, Machine Learning and AI for Agricultural Economics. Springer, 2021.")
    pdf.bullet("C. Vuppalapati, Building Next-Gen ESG Platforms with IoT and AI for SDGs. CRC Press, 2026.")
    pdf.bullet("World Bank Open Data - https://data.worldbank.org")
    pdf.bullet("FAO/WHO/UNICEF - The State of Food Security and Nutrition in the World 2024 & 2025.")
    pdf.bullet("California DMV - Driver's Handbook (https://www.dmv.ca.gov)")
    pdf.bullet("Bureau of Labor Statistics - https://api.bls.gov")
    pdf.bullet("Google Gemini API - https://ai.google.dev")

    # ── Output ──────────────────────────────────────────────────
    output_path = "/Users/mminai/Desktop/smartphone-voice-generator/VoiceNavigator_Project_Report.pdf"
    pdf.output(output_path)
    print(f"Report generated: {output_path}")


if __name__ == "__main__":
    generate()
