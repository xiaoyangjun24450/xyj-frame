package com.xyj.focuspod.screen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.QuestionSource
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.topStageBar
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun tutoringScreen(context: Context, state: StudySessionState): View {
    val question = state.currentQuestion
    return appPage(context, state) {
        addView(topStageBar(this, "例题讲解", "剩余 ${state.remainingCount} 题"))

        addView(card(this).apply {
            addView(TextView(context).apply {
                applyText(question?.title ?: "等待题目", 22f, AppColors.TextPrimary, bold = true)
            })
            addView(TextView(context).apply {
                applyText(question?.prompt.orEmpty(), 24f, AppColors.TextPrimary)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 14.dp(this@apply)
            })
        })

        addView(card(this).apply {
            addView(TextView(context).apply {
                val intro = if (question?.source == QuestionSource.MISTAKE_REVIEW) {
                    "AI 引导：这是一道错题复盘例题。先回忆题目条件，再找出关系式，最后检查单位。"
                } else {
                    "AI 引导：先读题找已知条件，再把问题拆成两步。系统只做引导，不直接给最终答案。"
                }
                applyText(intro, 18f, AppColors.TextPrimary)
            })
        })

        addView(TextView(context).apply {
            applyText("姿态提示：请保持坐姿端正。完成后请翻转手机开始批卷。", 16f, AppColors.TextSecondary)
        })
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
