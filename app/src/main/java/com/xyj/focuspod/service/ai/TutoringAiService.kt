package com.xyj.focuspod.service.ai

import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.model.TutoringSpeaker

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
    fun onSubtitle(subtitle: TutoringSubtitle) {
        when (subtitle.speaker) {
            TutoringSpeaker.STUDENT -> onStudentSpeech(subtitle.text)
            TutoringSpeaker.AI -> onCaption(subtitle.text)
        }
    }
    fun onError(message: String)
}

data class TutoringSubtitle(
    val speaker: TutoringSpeaker,
    val text: String,
    val definite: Boolean,
    val paragraph: Boolean,
    val sequence: Int,
    val roundId: Int
)
