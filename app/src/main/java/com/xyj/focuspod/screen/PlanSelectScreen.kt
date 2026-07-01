package com.xyj.focuspod.screen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.applyText
import com.xyj.focuspod.ui.component.card
import com.xyj.focuspod.ui.component.dp
import com.xyj.focuspod.ui.component.labelView
import com.xyj.focuspod.ui.component.setRoundedBackground
import com.xyj.focuspod.ui.component.titleView
import com.xyj.focuspod.ui.layout.appPage
import com.xyj.focuspod.ui.theme.AppColors

fun planSelectScreen(context: Context, state: StudySessionState, flow: StudyFlow): View {
    var selectedPlan: StudyPlan? = state.availablePlans.firstOrNull()

    return appPage(context, state) {
        addView(titleView(this, "选择学习计划"))
        addView(labelView(this, "${state.studentName} · 网络${if (state.networkOnline) "在线" else "离线"}").apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 6.dp(this@apply); bottomMargin = 18.dp(this@apply) }
        })

        if (state.availablePlans.isEmpty()) {
            addView(labelView(this, "暂无学习计划，请联系老师。"))
        } else {
            state.availablePlans.forEach { plan ->
                addView(planCard(this, plan, selectedPlan == plan) {
                    selectedPlan = plan
                })
            }
        }

        addView(Button(context).apply {
            text = "开始本计划"
            isEnabled = selectedPlan != null
            setTextColor(if (isEnabled) AppColors.Surface else AppColors.TextSecondary)
            setRoundedBackground(if (isEnabled) AppColors.Primary else AppColors.Muted, radiusDp = 8)
            setOnClickListener {
                selectedPlan?.let(flow::selectPlan)
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 52.dp(this)).apply {
            topMargin = 12.dp(this@apply)
        })
    }
}

private fun planCard(parent: View, plan: StudyPlan, selected: Boolean, onSelect: () -> Unit): View {
    return card(parent).apply {
        if (selected) {
            setRoundedBackground(AppColors.Surface, radiusDp = 8, strokeColor = AppColors.Primary, strokeWidthDp = 2)
        }
        setOnClickListener { onSelect() }
        addView(TextView(context).apply {
            applyText(plan.title, 20f, AppColors.TextPrimary, bold = true)
        })
        addView(TextView(context).apply {
            applyText(
                "${plan.subject} · 约 ${plan.estimatedMinutes} 分钟 · ${plan.exampleQuestions.size + plan.examQuestions.size} 题 · ${plan.passScore} 分达标",
                15f,
                AppColors.TextSecondary
            )
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 8.dp(this@apply)
        })
    }
}
