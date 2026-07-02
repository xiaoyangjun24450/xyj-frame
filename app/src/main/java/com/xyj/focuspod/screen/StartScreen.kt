package com.xyj.focuspod.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusPrimarySoft

@Composable
fun StartScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(156.dp)) {
                drawRoundRect(color = FocusPrimarySoft, size = size)
                drawRoundRect(
                    color = FocusPrimary,
                    topLeft = Offset(size.width * 0.28f, size.height * 0.18f),
                    size = Size(size.width * 0.44f, size.height * 0.64f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = FocusPrimary,
                    radius = 6.dp.toPx(),
                    center = Offset(size.width / 2f, size.height * 0.70f)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "XYJ 学习仓", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在启动，请稍候",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "手机放入学习仓后，将通过语音和翻转动作完成学习闭环",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
