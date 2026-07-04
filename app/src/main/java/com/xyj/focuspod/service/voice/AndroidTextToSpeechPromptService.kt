package com.xyj.focuspod.service.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTextToSpeechPromptService(
    context: Context
) : VoicePromptService, TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = textToSpeech ?: return
            val languageResult = engine.setLanguage(Locale.CHINESE)
            ready = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED
            pendingText?.let { speak(it) }
            pendingText = null
        } else {
            ready = false
        }
    }

    override fun speak(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val engine = textToSpeech
        if (engine == null || !ready) {
            pendingText = cleanText
            return
        }
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "focuspod-${System.currentTimeMillis()}")
    }

    override fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
        pendingText = null
    }

}
