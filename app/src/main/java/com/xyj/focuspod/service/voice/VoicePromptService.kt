package com.xyj.focuspod.service.voice

interface VoicePromptService {
    fun speak(text: String)

    fun release() = Unit
}
