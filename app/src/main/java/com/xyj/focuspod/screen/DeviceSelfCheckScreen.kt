package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.DeviceCheckList
import com.xyj.focuspod.ui.component.PoseHintBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun DeviceSelfCheckScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "设备自检中，请勿触摸手机",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(22.dp))
            DeviceCheckList(checks = state.deviceChecks)
            Spacer(modifier = Modifier.height(18.dp))
            PoseHintBar(text = "请确认手机仓 USB 已连接，系统会自动进入学习流程。")
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
