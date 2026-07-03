package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.QuestionCard
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun TutoringScreen(state: StudySessionState) {
    val question = state.currentQuestion

    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = "例题讲解", remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(18.dp))
            QuestionCard(question = question)
            Spacer(modifier = Modifier.height(16.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
