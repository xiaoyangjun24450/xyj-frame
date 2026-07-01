package com.xyj.focuspod.screen

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun doneScreen(context: Context, state: StudySessionState): View {
    return appPage(context, state) {
        addView(card(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(TextView(context).apply {
                applyText("学习完成", 34f, AppColors.Success, bold = true)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(TextView(context).apply {
                applyText("考试得分 ${state.totalExamScore} 分 · 完成 ${state.completedQuestionCount} 题 · 错题已处理 ${state.mistakeCount} 题", 18f, AppColors.TextPrimary)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16.dp(this@apply)
            })
            addView(TextView(context).apply {
                val statusText = when (state.doorStatus) {
                    DoorStatus.OPENING -> "正在开门"
                    DoorStatus.OPENED -> "开门成功"
                    DoorStatus.FAILED -> "开门失败"
                }
                val statusColor = when (state.doorStatus) {
                    DoorStatus.OPENING -> AppColors.Primary
                    DoorStatus.OPENED -> AppColors.Success
                    DoorStatus.FAILED -> AppColors.Warning
                }
                applyText(statusText, 26f, statusColor, bold = true)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24.dp(this@apply)
            })
        })
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
