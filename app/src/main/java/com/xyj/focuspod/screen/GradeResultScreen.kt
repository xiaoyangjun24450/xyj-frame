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
import com.xyj.focuspod.ui.component.GradeResultPanel
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar
import com.xyj.focuspod.ui.component.stageText

@Composable
fun GradeResultScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = stageText(state.stage), remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(22.dp))
            GradeResultPanel(
                result = state.lastGradeResult,
                countdownSeconds = state.resultCountdownSeconds
            )
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
