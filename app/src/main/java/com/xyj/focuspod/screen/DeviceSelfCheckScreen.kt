package com.xyj.focuspod.screen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.model.DeviceCheckStatus
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.component.titleView
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.layout.voiceCaptionBar
import com.xyj.focuspod.ui.theme.AppColors

fun deviceSelfCheckScreen(context: Context, state: StudySessionState): View {
    return appPage(context, state) {
        addView(titleView(this, "设备自检中，请勿触摸手机"))

        addView(card(this).apply {
            state.deviceChecks.forEach { item ->
                addView(TextView(context).apply {
                    val statusText = when (item.status) {
                        DeviceCheckStatus.CHECKING -> "检查中"
                        DeviceCheckStatus.PASSED -> "已通过"
                        DeviceCheckStatus.FAILED -> "未通过"
                    }
                    val color = when (item.status) {
                        DeviceCheckStatus.CHECKING -> AppColors.TextSecondary
                        DeviceCheckStatus.PASSED -> AppColors.Success
                        DeviceCheckStatus.FAILED -> AppColors.Warning
                    }
                    applyText("${item.name}    $statusText", 19f, color, bold = item.status == DeviceCheckStatus.PASSED)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12.dp(this@apply)
                })
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 20.dp(this@apply)
        })

        addView(TextView(context).apply {
            applyText("请确认手机仓 USB 已连接，系统会自动进入学习流程。", 16f, AppColors.TextSecondary)
        })
        addView(voiceCaptionBar(this, state.voiceCaption))
    }
}
