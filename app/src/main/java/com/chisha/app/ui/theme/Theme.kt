package com.chisha.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- 暖色食欲底 ----
val WarmBg = Color(0xFFFFF8F0)
val WarmSurface = Color(0xFFFFFFFF)
val WarmSurfaceAlt = Color(0xFFFDF1E4)
val WarmOutline = Color(0xFFEADAC8)
val InkPrimary = Color(0xFF2E2016)
val InkSecondary = Color(0xFF7A6A5C)
val InkTertiary = Color(0xFFA69584)

val Ochre = Color(0xFFC8551F)
val OchreDark = Color(0xFF8F3A10)
val OchreLight = Color(0xFFFCE6D6)
val Leaf = Color(0xFF4E7A4A)

// ---- 稀有度 ----
val RarityCommon = Color(0xFF98A2AE)
val RaritySignature = Color(0xFF8E6BBF)
val RarityRare = Color(0xFFD9A21B)

// ---- 成就分档 ----
val TierBronze = Color(0xFFB0793F)
val TierSilver = Color(0xFF9BA6B2)
val TierGold = Color(0xFFD9A21B)

private val ChishaScheme = lightColorScheme(
    primary = Ochre,
    onPrimary = Color.White,
    primaryContainer = OchreLight,
    onPrimaryContainer = OchreDark,
    secondary = Color(0xFF8C6A4F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E6D8),
    onSecondaryContainer = Color(0xFF4A3627),
    tertiary = Leaf,
    onTertiary = Color.White,
    background = WarmBg,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceAlt,
    onSurfaceVariant = InkSecondary,
    outline = WarmOutline,
    outlineVariant = Color(0xFFF2E6D8),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val ChishaTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun ChishaTheme(content: @Composable () -> Unit) {
    // 固定浅色：米白底是这套视觉的地基，跟随系统深色会让菜品卡发脏
    MaterialTheme(
        colorScheme = ChishaScheme,
        typography = ChishaTypography,
        content = content
    )
}
