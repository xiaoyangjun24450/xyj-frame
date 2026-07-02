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
import com.xyj.focuspod.ui.component.PoseHintBar
import com.xyj.focuspod.ui.component.QuestionCard
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun ExamScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = "考试", remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(20.dp))
            QuestionCard(question = state.currentQuestion)
            Spacer(modifier = Modifier.height(18.dp))
            PoseHintBar(text = "写完后请翻转手机提交本题。")
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
