package com.xyj.focuspod.ui.layout

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.component.setRoundedBackground
import com.xyj.focuspod.ui.theme.AppColors

fun appPage(
    context: Context,
    state: StudySessionState,
    scrollable: Boolean = true,
    contentBuilder: LinearLayout.(FrameLayout) -> Unit
): View {
    val root = FrameLayout(context).apply {
        setBackgroundColor(AppColors.Background)
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20.dp(this), 22.dp(this), 20.dp(this), 22.dp(this))
        contentBuilder(root)
    }

    if (scrollable) {
        root.addView(
            ScrollView(context).apply { addView(content) },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    } else {
        root.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    if (state.alertMessage != null) {
        root.addView(alertOverlay(context, state.alertMessage))
    }
    return root
}

fun topStageBar(parent: View, stageText: String, remainingText: String): LinearLayout {
    return LinearLayout(parent.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 18.dp(this@apply) }

        addView(TextView(context).apply {
            applyText(stageText, 15f, AppColors.Primary, bold = true)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        addView(TextView(context).apply {
            applyText(remainingText, 15f, AppColors.TextSecondary, bold = true)
            gravity = Gravity.END
        })
    }
}

fun voiceCaptionBar(parent: View, text: String): TextView {
    return TextView(parent.context).apply {
        applyText(text.ifBlank { "等待语音提示" }, 15f, AppColors.TextPrimary)
        setPadding(14.dp(this), 11.dp(this), 14.dp(this), 11.dp(this))
        setRoundedBackground(AppColors.Muted, radiusDp = 8)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 14.dp(this@apply) }
    }
}

private fun alertOverlay(context: Context, message: String): View {
    return FrameLayout(context).apply {
        setBackgroundColor(0x66000000)
        val alert = TextView(context).apply {
            applyText(message, 18f, AppColors.Warning, bold = true)
            gravity = Gravity.CENTER
            setPadding(18.dp(this), 16.dp(this), 18.dp(this), 16.dp(this))
            setRoundedBackground(AppColors.Surface, radiusDp = 8, strokeColor = AppColors.Warning)
        }
        addView(
            alert,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = 24.dp(this@apply)
                rightMargin = 24.dp(this@apply)
            }
        )
    }
}
