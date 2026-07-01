package com.xyj.focuspod.screen

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.component.setRoundedBackground
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.topStageBar
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun gradingScreen(context: Context, state: StudySessionState): View {
    return appPage(context, state, scrollable = false) {
        addView(topStageBar(this, if (state.stage == StudyStage.EXAMPLE) "例题讲解" else "考试", "剩余 ${state.remainingCount} 题"))

        addView(FrameLayout(context).apply {
            setRoundedBackground(AppColors.CameraDark, radiusDp = 8)
            addView(TextView(context).apply {
                val status = when (state.gradingStep) {
                    GradingStep.CAPTURING -> "正在拍摄"
                    GradingStep.UPLOADING -> "正在上传"
                    GradingStep.GRADING -> "正在批改"
                    GradingStep.WAITING_FLIP_BACK -> "批改完成\n请把手机翻回正面"
                }
                applyText(status, 26f, AppColors.Surface, bold = true)
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            bottomMargin = 18.dp(this@apply)
        })

        addView(TextView(context).apply {
            applyText("请保持手机稳定", 17f, AppColors.TextSecondary)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
