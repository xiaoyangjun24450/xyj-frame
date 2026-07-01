package com.xyj.focuspod.ui.component

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xyj.focuspod.ui.theme.AppColors

fun Int.dp(view: View): Int = (this * view.resources.displayMetrics.density).toInt()

fun Int.dp(layoutParams: ViewGroup.LayoutParams): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

fun View.setRoundedBackground(
    color: Int,
    radiusDp: Int = 8,
    strokeColor: Int? = null,
    strokeWidthDp: Int = 1
) {
    val radius = radiusDp * resources.displayMetrics.density
    background = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        strokeColor?.let {
            setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), it)
        }
    }
}

fun verticalLayout(
    view: View,
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    init: LinearLayout.() -> Unit = {}
): LinearLayout {
    return LinearLayout(view.context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(width, height)
        init()
    }
}

fun horizontalLayout(
    view: View,
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    init: LinearLayout.() -> Unit = {}
): LinearLayout {
    return LinearLayout(view.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(width, height)
        init()
    }
}

fun TextView.applyText(
    textValue: String,
    sizeSp: Float,
    color: Int = AppColors.TextPrimary,
    bold: Boolean = false
) {
    text = textValue
    textSize = sizeSp
    setTextColor(color)
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    includeFontPadding = true
}

fun labelView(parent: View, text: String, color: Int = AppColors.TextSecondary): TextView {
    return TextView(parent.context).apply {
        applyText(text, 15f, color)
    }
}

fun titleView(parent: View, text: String): TextView {
    return TextView(parent.context).apply {
        applyText(text, 26f, AppColors.TextPrimary, bold = true)
    }
}

fun card(parent: View): LinearLayout {
    return LinearLayout(parent.context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(18.dp(this), 18.dp(this), 18.dp(this), 18.dp(this))
        setRoundedBackground(AppColors.Surface, radiusDp = 8, strokeColor = AppColors.Border)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12.dp(this@apply)
        }
    }
}

fun badge(parent: View, text: String, color: Int = AppColors.Primary): TextView {
    return TextView(parent.context).apply {
        applyText(text, 13f, Color.WHITE, bold = true)
        gravity = Gravity.CENTER
        setPadding(10.dp(this), 5.dp(this), 10.dp(this), 5.dp(this))
        setRoundedBackground(color, radiusDp = 6)
    }
}
