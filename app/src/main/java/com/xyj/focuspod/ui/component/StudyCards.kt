package com.xyj.focuspod.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.DeviceCheckItem
import com.xyj.focuspod.model.DeviceCheckStatus
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.model.TutoringMessage
import com.xyj.focuspod.model.TutoringSpeaker
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusPrimarySoft
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun PlanCard(
    plan: StudyPlan,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) FocusPrimary else MaterialTheme.colorScheme.surfaceVariant
    val containerColor = if (selected) FocusPrimarySoft else MaterialTheme.colorScheme.surface

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = plan.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selected) {
                    StatusChip(text = "已选择", color = FocusPrimary, textColor = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "约 ${plan.estimatedMinutes} 分钟 · ${plan.exampleQuestions.size + plan.examQuestions.size} 题 · ${plan.passScore} 分达标",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun QuestionCard(
    question: Question?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = question?.title ?: "等待题目",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            MarkdownText(
                markdown = question?.questionMarkdown.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AiGuidePanel(
    messages: List<TutoringMessage>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "AI 引导",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (messages.isEmpty()) {
                Text(text = "正在连接语音辅导。", style = MaterialTheme.typography.bodyLarge)
            } else {
                messages.forEach { message ->
                    val speakerText = when (message.speaker) {
                        TutoringSpeaker.AI -> "AI"
                        TutoringSpeaker.STUDENT -> "学生"
                    }
                    val textColor = when (message.speaker) {
                        TutoringSpeaker.AI -> MaterialTheme.colorScheme.onSurface
                        TutoringSpeaker.STUDENT -> MaterialTheme.colorScheme.primary
                    }

                    Text(
                        text = "$speakerText：${message.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DeviceCheckList(
    checks: List<DeviceCheckItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            checks.forEach { item ->
                val statusText = when (item.status) {
                    DeviceCheckStatus.CHECKING -> "检查中"
                    DeviceCheckStatus.PASSED -> "已通过"
                    DeviceCheckStatus.FAILED -> "未通过"
                }
                val statusColor = when (item.status) {
                    DeviceCheckStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
                    DeviceCheckStatus.PASSED -> FocusSuccess
                    DeviceCheckStatus.FAILED -> FocusWarning
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
