package com.xyj.focuspod.screen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.topStageBar
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun examScreen(context: Context, state: StudySessionState): View {
    val question = state.currentQuestion
    return appPage(context, state) {
        addView(topStageBar(this, "考试", "剩余 ${state.remainingCount} 题"))

        addView(card(this).apply {
            addView(TextView(context).apply {
                applyText(question?.title ?: "等待题目", 22f, AppColors.TextPrimary, bold = true)
            })
            addView(TextView(context).apply {
                applyText(question?.prompt.orEmpty(), 28f, AppColors.TextPrimary)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 18.dp(this@apply)
            })
        })

        addView(TextView(context).apply {
            applyText("写完后请翻转手机提交本题。", 18f, AppColors.TextSecondary)
        })
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
