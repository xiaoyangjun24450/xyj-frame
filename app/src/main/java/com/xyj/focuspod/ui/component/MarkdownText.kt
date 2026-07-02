package com.xyj.focuspod.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = markdown
        .trim()
        .split(Regex("\\n{2,}"))
        .filter { it.isNotBlank() }

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            MarkdownBlock(
                block = block.trim(),
                style = style,
                color = color
            )
            if (index != blocks.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MarkdownBlock(
    block: String,
    style: TextStyle,
    color: Color
) {
    val headingLevel = block.takeWhile { it == '#' }.length
    val headingText = block.drop(headingLevel).trim()
    if (headingLevel in 1..3 && headingText.isNotEmpty()) {
        Text(
            text = parseInlineMarkdown(headingText),
            style = when (headingLevel) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.bodyLarge
            },
            color = color,
            fontWeight = FontWeight.Bold
        )
        return
    }

    val lines = block.lines().filter { it.isNotBlank() }
    val normalized = lines.joinToString("\n") { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ")}"
            trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ")}"
            else -> trimmed
        }
    }

    Text(
        text = parseInlineMarkdown(normalized),
        style = style,
        color = color
    )
}

internal fun parseInlineMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < markdown.length) {
            when {
                markdown.startsWith("**", index) -> {
                    val end = markdown.indexOf("**", startIndex = index + 2)
                    if (end == -1) {
                        append(markdown[index])
                        index += 1
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(markdown.substring(index + 2, end))
                        }
                        index = end + 2
                    }
                }

                markdown[index] == '`' -> {
                    val end = markdown.indexOf('`', startIndex = index + 1)
                    if (end == -1) {
                        append(markdown[index])
                        index += 1
                    } else {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(markdown.substring(index + 1, end))
                        }
                        index = end + 1
                    }
                }

                markdown[index] == '$' -> {
                    val end = markdown.indexOf('$', startIndex = index + 1)
                    if (end == -1) {
                        append(markdown[index])
                        index += 1
                    } else {
                        appendLatexFormula(markdown.substring(index + 1, end))
                        index = end + 1
                    }
                }

                else -> {
                    append(markdown[index])
                    index += 1
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendLatexFormula(latex: String) {
    withStyle(
        SpanStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
    ) {
        append(parseLatexFormula(latex))
    }
}

internal fun parseLatexFormula(latex: String): String {
    var text = latex.trim()
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\cdot", "·")
        .replace("\\le", "≤")
        .replace("\\ge", "≥")
        .replace("\\neq", "≠")
        .replace("\\pi", "π")
        .replace("\\%", "%")

    text = replaceFractions(text)
    text = replaceScript(text, '^', SUPERSCRIPT_DIGITS)
    text = replaceScript(text, '_', SUBSCRIPT_DIGITS)
    return text
        .replace("{", "")
        .replace("}", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun replaceFractions(input: String): String {
    var text = input
    val fractionRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
    while (true) {
        val updated = fractionRegex.replace(text) { match ->
            val numerator = match.groupValues[1]
            val denominator = match.groupValues[2]
            vulgarFractions[numerator to denominator] ?: "$numerator/$denominator"
        }
        if (updated == text) return updated
        text = updated
    }
}

private fun replaceScript(
    input: String,
    marker: Char,
    replacements: Map<Char, Char>
): String {
    val builder = StringBuilder()
    var index = 0
    while (index < input.length) {
        if (input[index] == marker && index + 1 < input.length) {
            val script = when (input[index + 1]) {
                '{' -> {
                    val end = input.indexOf('}', startIndex = index + 2)
                    if (end == -1) null else input.substring(index + 2, end) to end + 1
                }
                else -> input[index + 1].toString() to index + 2
            }
            if (script != null) {
                builder.append(script.first.map { replacements[it] ?: it }.joinToString(""))
                index = script.second
                continue
            }
        }
        builder.append(input[index])
        index += 1
    }
    return builder.toString()
}

private val vulgarFractions = mapOf(
    "1" to "2" to "½",
    "1" to "3" to "⅓",
    "2" to "3" to "⅔",
    "1" to "4" to "¼",
    "3" to "4" to "¾",
    "1" to "5" to "⅕",
    "2" to "5" to "⅖",
    "3" to "5" to "⅗",
    "4" to "5" to "⅘",
    "1" to "6" to "⅙",
    "5" to "6" to "⅚",
    "1" to "8" to "⅛",
    "3" to "8" to "⅜",
    "5" to "8" to "⅝",
    "7" to "8" to "⅞"
)

private val SUPERSCRIPT_DIGITS = mapOf(
    '0' to '⁰',
    '1' to '¹',
    '2' to '²',
    '3' to '³',
    '4' to '⁴',
    '5' to '⁵',
    '6' to '⁶',
    '7' to '⁷',
    '8' to '⁸',
    '9' to '⁹',
    '+' to '⁺',
    '-' to '⁻',
    '=' to '⁼',
    '(' to '⁽',
    ')' to '⁾'
)

private val SUBSCRIPT_DIGITS = mapOf(
    '0' to '₀',
    '1' to '₁',
    '2' to '₂',
    '3' to '₃',
    '4' to '₄',
    '5' to '₅',
    '6' to '₆',
    '7' to '₇',
    '8' to '₈',
    '9' to '₉',
    '+' to '₊',
    '-' to '₋',
    '=' to '₌',
    '(' to '₍',
    ')' to '₎'
)
