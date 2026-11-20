package com.mathkatex.verify.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// 正式版色板（2026-11-20 锁定）
// 一个主色 + 中性底 + 三级文字灰；语义色只用于对错/提示。
// ---------------------------------------------------------------------------

/** 扩展语义色：Material3 的 ColorScheme 槽位不够表达「soft 底」这类 token。 */
data class AppColors(
    val primary: Color,
    val primaryStrong: Color,
    val primarySoft: Color,
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val line: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color,
)

val LightAppColors = AppColors(
    primary = Color(0xFF2563EB),
    primaryStrong = Color(0xFF1D4ED8),
    primarySoft = Color(0xFFEAF1FF),
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF3F9),
    text = Color(0xFF0F172A),
    text2 = Color(0xFF4A5A70),
    text3 = Color(0xFF8896A8),
    line = Color(0xFFE2E8F1),
    success = Color(0xFF15803D),
    successSoft = Color(0xFFE8F6EE),
    danger = Color(0xFFC24141),
    dangerSoft = Color(0xFFFBECEC),
    warning = Color(0xFFB45309),
    warningSoft = Color(0xFFFDF3E3),
)

val DarkAppColors = AppColors(
    primary = Color(0xFF6EA8FF),
    primaryStrong = Color(0xFF8BB9FF),
    primarySoft = Color(0xFF172A4A),
    background = Color(0xFF0B1220),
    surface = Color(0xFF121C2F),
    surface2 = Color(0xFF17233A),
    text = Color(0xFFE9EFF8),
    text2 = Color(0xFFADBACD),
    text3 = Color(0xFF7B8AA0),
    line = Color(0xFF22314A),
    success = Color(0xFF5FD39A),
    successSoft = Color(0xFF122C23),
    danger = Color(0xFFF08C8C),
    dangerSoft = Color(0xFF33191D),
    warning = Color(0xFFF0B46A),
    warningSoft = Color(0xFF332514),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 旧代码里引用过的颜色名，保留别名避免一次性改太多。 */
val KaTeXBlue = LightAppColors.primary
val KaTeXBackground = LightAppColors.background
