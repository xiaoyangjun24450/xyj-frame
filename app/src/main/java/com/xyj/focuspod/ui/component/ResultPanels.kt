package com.xyj.focuspod.ui.component

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.ui.theme.FocusCamera
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun GradingCameraPanel(
    step: GradingStep,
    countdownSeconds: Int,
    onPreviewReady: (TextureView) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(FocusCamera),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                TextureView(context).also(onPreviewReady)
            },
            update = onPreviewReady,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.34f))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = gradingStepText(step, countdownSeconds),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "请保持手机稳定，不要遮挡试卷",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.86f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GradeResultPanel(
    result: GradeResult?,
    countdownSeconds: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = "本题得分",
                style = MaterialTheme.typography.headlineLarge,
                color = FocusPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = result?.let { "${scoreText(it.score)} / ${scoreText(it.maxScore)} 分" } ?: "0 分",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = result?.feedback.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (!result?.suggestion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result?.suggestion.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            StatusChip(
                text = "${countdownSeconds} 秒后进入下一步",
                color = FocusPrimary,
                textColor = Color.White
            )
        }
    }
}

@Composable
fun DoorStatusPanel(
    totalExamScore: Double,
    passScore: Int,
    examPassed: Boolean?,
    completedQuestionCount: Int,
    mistakeCount: Int,
    doorStatus: DoorStatus,
    modifier: Modifier = Modifier
) {
    val passed = examPassed == true
    val statusColor = if (!passed) {
        FocusWarning
    } else {
        when (doorStatus) {
            DoorStatus.OPENING -> FocusPrimary
            DoorStatus.OPENED -> FocusSuccess
            DoorStatus.FAILED -> FocusWarning
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = if (passed) "考试达标" else "考试不合格",
                style = MaterialTheme.typography.headlineLarge,
                color = if (passed) FocusSuccess else FocusWarning,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "考试成绩 ${scoreText(totalExamScore)} 分 · 达标分 ${passScore} 分 · 完成 ${completedQuestionCount} 题",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (passed) {
                    "正在处理开门指令"
                } else {
                    "错题 ${mistakeCount} 题，正在进入错题辅导"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = if (passed) doorStatusText(doorStatus) else "10 秒后开始复习",
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
