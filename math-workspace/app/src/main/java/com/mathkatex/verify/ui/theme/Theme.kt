package com.mathkatex.verify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightAppColors.primary,
    onPrimary = Color.White,
    primaryContainer = LightAppColors.primarySoft,
    onPrimaryContainer = LightAppColors.primaryStrong,
    secondary = LightAppColors.text2,
    onSecondary = Color.White,
    secondaryContainer = LightAppColors.surface2,
    onSecondaryContainer = LightAppColors.text,
    tertiary = LightAppColors.success,
    onTertiary = Color.White,
    tertiaryContainer = LightAppColors.successSoft,
    onTertiaryContainer = LightAppColors.success,
    background = LightAppColors.background,
    onBackground = LightAppColors.text,
    surface = LightAppColors.surface,
    onSurface = LightAppColors.text,
    surfaceVariant = LightAppColors.surface2,
    onSurfaceVariant = LightAppColors.text2,
    outline = LightAppColors.line,
    outlineVariant = LightAppColors.line,
    error = LightAppColors.danger,
    onError = Color.White,
    errorContainer = LightAppColors.dangerSoft,
    onErrorContainer = LightAppColors.danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppColors.primary,
    onPrimary = Color(0xFF08111F),
    primaryContainer = DarkAppColors.primarySoft,
    onPrimaryContainer = DarkAppColors.primaryStrong,
    secondary = DarkAppColors.text2,
    onSecondary = Color(0xFF08111F),
    secondaryContainer = DarkAppColors.surface2,
    onSecondaryContainer = DarkAppColors.text,
    tertiary = DarkAppColors.success,
    onTertiary = Color(0xFF04140D),
    tertiaryContainer = DarkAppColors.successSoft,
    onTertiaryContainer = DarkAppColors.success,
    background = DarkAppColors.background,
    onBackground = DarkAppColors.text,
    surface = DarkAppColors.surface,
    onSurface = DarkAppColors.text,
    surfaceVariant = DarkAppColors.surface2,
    onSurfaceVariant = DarkAppColors.text2,
    outline = DarkAppColors.line,
    outlineVariant = DarkAppColors.line,
    error = DarkAppColors.danger,
    onError = Color(0xFF1A0A0A),
    errorContainer = DarkAppColors.dangerSoft,
    onErrorContainer = DarkAppColors.danger,
)

/**
 * 正式版主题：固定 Light/Dark 两套 token，关闭 dynamicColor。
 * 动态取色会跟壁纸变，品牌色和对比度都不受控，学习类 App 不需要。
 */
@Composable
fun KaTeXVerifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
