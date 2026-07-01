package com.xyj.focuspod.screen

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.component.setRoundedBackground
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.theme.AppColors

fun startScreen(context: Context, state: StudySessionState): View {
    return appPage(context, state, scrollable = false) {
        gravity = Gravity.CENTER

        addView(TextView(context).apply {
            text = "▣"
            textSize = 96f
            setTextColor(AppColors.Primary)
            gravity = Gravity.CENTER
            setRoundedBackground(AppColors.Muted, radiusDp = 8)
        }, LinearLayout.LayoutParams(150.dp(this), 150.dp(this)).apply {
            bottomMargin = 24.dp(this@apply)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        addView(TextView(context).apply {
            applyText("XYJ 学习仓", 30f, AppColors.TextPrimary, bold = true)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addView(TextView(context).apply {
            applyText("正在启动，请稍候", 18f, AppColors.TextSecondary)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 12.dp(this@apply)
        })

        addView(TextView(context).apply {
            applyText("手机放入学习仓后，将通过语音和翻转动作完成学习闭环", 14f, AppColors.TextSecondary)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 42.dp(this@apply)
        })
    }
}
