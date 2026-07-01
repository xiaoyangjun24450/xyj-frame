package com.xyj.focuspod.screen

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.topStageBar
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun gradeResultScreen(context: Context, state: StudySessionState): View {
    val result = state.lastGradeResult
    val passed = result?.passed == true
    return appPage(context, state) {
        addView(topStageBar(this, if (state.stage == StudyStage.EXAMPLE) "例题讲解" else "考试", "剩余 ${state.remainingCount} 题"))

        addView(card(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(TextView(context).apply {
                applyText(if (passed) "本题通过" else "本题未通过", 32f, if (passed) AppColors.Success else AppColors.Warning, bold = true)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(TextView(context).apply {
                applyText("${result?.score ?: 0} 分", 42f, AppColors.TextPrimary, bold = true)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 14.dp(this@apply)
            })
            addView(TextView(context).apply {
                applyText(result?.feedback.orEmpty(), 18f, AppColors.TextPrimary)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 14.dp(this@apply)
            })
        })

        addView(TextView(context).apply {
            applyText("${state.resultCountdownSeconds} 秒后进入下一步", 20f, AppColors.TextSecondary, bold = true)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16.dp(this@apply)
        })
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
