package com.xyj.focuspod.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.xyj.focuspod.ui.component.parseLatexFormula
import com.xyj.focuspod.ui.component.parseInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {
    @Test
    fun parseInlineMarkdownRemovesBoldMarkers() {
        val text = parseInlineMarkdown("一根绳子长 **24 米**。")

        assertEquals("一根绳子长 24 米。", text.text)
        assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun parseInlineMarkdownRendersLatexFormula() {
        val text = parseInlineMarkdown("\$24 \\times 0.8 = 96\$")

        assertEquals("24 × 0.8 = 96", text.text)
    }

    @Test
    fun parseInlineMarkdownAppliesCodeStyle() {
        val text = parseInlineMarkdown("填空：`goes`")

        assertEquals("填空：goes", text.text)
        assertTrue(text.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun parseLatexFormulaRendersFractions() {
        assertEquals("⅓", parseLatexFormula("\\frac{1}{3}"))
        assertEquals("24 × ⅓ = 8", parseLatexFormula("24 \\times \\frac{1}{3} = 8"))
    }

    @Test
    fun parseLatexFormulaRendersScripts() {
        assertEquals("x² + y₁", parseLatexFormula("x^2 + y_1"))
    }
}
