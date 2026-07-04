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
import com.xyj.focuspod.ui.component.DoorStatusPanel
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun DoneScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DoorStatusPanel(
                totalExamScore = state.totalExamScore,
                passScore = state.selectedPlan?.passScore ?: 0,
                examPassed = state.examPassed,
                completedQuestionCount = state.completedQuestionCount,
                mistakeCount = state.mistakeCount,
                doorStatus = state.doorStatus
            )
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
