package com.hackathon.voicenavigator

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class VoiceNavigatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)
    }
}
