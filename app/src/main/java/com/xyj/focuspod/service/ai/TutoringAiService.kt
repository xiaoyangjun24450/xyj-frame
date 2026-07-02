package com.xyj.focuspod.service.ai

import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage

interface TutoringAiService {
    fun startSession(
        stage: StudyStage,
        question: Question,
        listener: TutoringAiListener
    )

    fun stopSession()

    fun release() {
        stopSession()
    }
}

interface TutoringAiListener {
    fun onSessionStarted()
    fun onStudentSpeech(text: String)
    fun onCaption(text: String)
    fun onError(message: String)
}
