package com.xyj.focuspod.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FocusBackground = Color(0xFFF3F7F6)
val FocusSurface = Color(0xFFFBFEFD)
val FocusSurfaceLift = Color(0xFFEAF2F1)
val FocusPrimary = Color(0xFF0B665F)
val FocusPrimarySoft = Color(0xFFD6EBE8)
val FocusText = Color(0xFF152423)
val FocusTextMuted = Color(0xFF657978)
val FocusWarning = Color(0xFFE46645)
val FocusSuccess = Color(0xFF168E69)
val FocusCamera = Color(0xFF122020)

private val FocusLightColors = lightColorScheme(
    primary = FocusPrimary,
    onPrimary = Color.White,
    primaryContainer = FocusPrimarySoft,
    onPrimaryContainer = FocusText,
    background = FocusBackground,
    onBackground = FocusText,
    surface = FocusSurface,
    onSurface = FocusText,
    surfaceVariant = FocusSurfaceLift,
    onSurfaceVariant = FocusTextMuted,
    error = FocusWarning,
    onError = Color.White
)

private val FocusTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = FocusText
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = FocusTextMuted
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val FocusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FocusPodTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocusLightColors,
        typography = FocusTypography,
        shapes = FocusShapes,
        content = content
    )
}
