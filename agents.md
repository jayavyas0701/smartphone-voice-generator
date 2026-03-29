# VoiceNavigator – Developer Guide & Project Requirements

## Overview

This project is part of the **CMPE 277 Hackathon (Spring 2026)** focused on building:

> Scalable, voice-driven mobile applications for **regulatory compliance** and **ESG impact** :contentReference[oaicite:0]{index=0}

The goal is to design a **voice-first Android application** that:
- Ensures compliance with regulatory content (DMV rules)
- Leverages ESG datasets (food security, sustainability)
- Provides an intelligent, touchless user experience

---

## Core Problem

Organizations need:
- Up-to-date regulatory training (DMV rules, compliance)
- Easy access to ESG insights (food security, sustainability)
- Hands-free interaction (voice-first UX)

Traditional apps are:
- manual (too many clicks)
- not accessible
- not scalable

---

## Solution Vision

Build a **voice-controlled mobile app** that acts as:

### 1. Compliance Assistant
- DMV knowledge test preparation
- Rule explanations via voice
- Quiz + evaluation system

### 2. ESG Intelligence Assistant
- Answer questions on food security and sustainability
- Use real datasets (FAO reports)
- Provide summaries and insights

### 3. Voice Navigation System
- Users interact using speech only
- Minimal UI dependency
- Real-time responses

---

## Data Sources

### ESG Dataset
- FAO Food Security Report :contentReference[oaicite:1]{index=1}  
- Provides:
  - global hunger statistics
  - food affordability data
  - urban vs rural trends

Example insight:
- ~2.4 billion people faced food insecurity globally :contentReference[oaicite:2]{index=2}

### DMV Dataset
- California Driver’s Handbook :contentReference[oaicite:3]{index=3}  
- Used for:
  - rules of the road
  - licensing requirements
  - quiz generation

Example:
- Users must pass knowledge + driving tests to get a license :contentReference[oaicite:4]{index=4}

---

## Functional Requirements

### Voice Features
- Speech-to-text input
- Text-to-speech output
- Continuous listening mode
- Command recognition:
  - "Start quiz"
  - "Explain speed limits"
  - "Show GDP trend"

### ESG Module
- Query ESG knowledge
- Summarize reports
- Compare data (e.g., 2023 vs 2024)
- Generate insights

### DMV Module
- Answer rule-based questions
- Provide explanations
- Run quizzes
- Track scores

### Quiz System
- Multiple choice questions
- Score tracking
- Feedback + explanation

### Navigation
- Bottom tabs or voice-only navigation
- Modules:
  - ESG
  - DMV
  - Quiz

---

## Non-Functional Requirements

- Scalable architecture
- Clean UI (Jetpack Compose)
- Fast response time
- Offline fallback (basic data)
- Secure API usage

---

## Tech Stack

### Mobile
- Kotlin
- Jetpack Compose
- Android SDK

### AI / Backend
- OpenAI API (for NLP + responses)
- Local parsing for PDFs (optional)

### Features
- SpeechRecognizer (Android)
- TextToSpeech (Android)

---

## Developer Agent Behavior

The agent (developer) must behave like:

### 1. System Designer
- Break features into modules
- Maintain clean architecture
- Avoid tightly coupled code

### 2. Problem Solver
- Handle missing data gracefully
- Provide fallback responses
- Debug runtime issues quickly

### 3. Product Thinker
- Prioritize usability
- Keep interaction simple
- Optimize for voice-first UX

### 4. Compliance-Oriented Engineer
- Ensure DMV accuracy
- Avoid hallucinated rules
- Base answers on real sources

### 5. Data Interpreter
- Convert ESG data into insights
- Avoid raw data dumps
- Provide meaningful summaries

---

## Expected App Flow

1. User opens app
2. App listens for voice
3. User speaks command:
   - “Start DMV quiz”
   - “Explain food security issues”
4. System:
   - Processes speech
   - Routes to correct module
   - Generates response
5. App responds via:
   - Voice (TTS)
   - Text UI

---

## Key Challenges

- Voice accuracy (speech recognition errors)
- API latency
- Data grounding (avoid incorrect answers)
- UI simplicity vs functionality

---

## Quick Start: Build & Run

### Prerequisites
- Android emulator running or device connected via USB
- OpenAI API key (set in `local.properties` or environment variable)
- Gradle sync successful

### Build & Install (Concise)
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install to device/emulator
./gradlew installDebug

# Launch app
adb shell am start -n com.hackathon.voicenavigator/com.hackathon.voicenavigator.MainActivity
```

### One-Liner (Full Build + Run)
```bash
./gradlew clean assembleDebug installDebug && adb shell am start -n com.hackathon.voicenavigator/com.hackathon.voicenavigator.MainActivity
```

### Set OpenAI API Key
Add to `local.properties`:
```properties
OPENAI_API_KEY=sk-your-key-here
```

---

## Testing Strategy

### Manual Testing
- Voice commands
- Navigation
- Quiz flow
- API responses

### Edge Cases
- No internet
- Invalid speech
- API failure

---

## Success Criteria

The app is successful if:

- Voice interaction works smoothly
- Users can:
  - learn DMV rules
  - take quizzes
  - ask ESG questions
- Responses are:
  - accurate
  - fast
  - relevant

---

## Common Errors & Fixes

### ❌ "NoSuchMethodError: No virtual method at(...KeyframesSpec...)" on ESG Tab
**Cause:** Compose BOM version `2024.01.00` incompatible with Material3 progress indicators.

**Fix:** Update `app/build.gradle.kts`:
```kotlin
// OLD (crashes):
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.compose.material3:material3")

// NEW (works):
implementation(platform("androidx.compose:compose-bom:2024.06.00"))
implementation("androidx.compose.material3:material3:1.2.1")
```
Then run: `./gradlew clean assembleDebug`

### ❌ "Activity class does not exist" on app launch
**Cause:** APK not installed or incorrect package name in adb command.

**Fix:**
```bash
./gradlew uninstallDebug          # Remove old APK
./gradlew installDebug            # Fresh install
adb shell am start -n com.hackathon.voicenavigator/com.hackathon.voicenavigator.MainActivity
```

### ❌ "RECORD_AUDIO permission denied"
**Cause:** Runtime permissions not granted.

**Fix:** Grant permission in emulator/device settings or restart app to trigger permission prompt.

---

## Future Improvements

- Offline AI models
- Better RAG (retrieval-augmented generation)
- Personalization
- Analytics dashboard

---

## Bottom Line

This is not just an app.

It is a **voice-first intelligent assistant** that combines:
- regulatory compliance
- ESG intelligence
- real-time AI interaction

Build it like a **product**, not just a demo.