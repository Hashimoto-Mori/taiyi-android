package com.jinggong.timer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 不随 Material 主题自动派生的图表/模式强调色。 */
@Immutable
data class ZenPalette(
    val quiet: Color,
    val standing: Color,
    val heatmapEmpty: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
    val onHeatmapHigh: Color,
)

val DarkZenPalette = ZenPalette(
    quiet = QuietAccent,
    standing = StandingAccent,
    heatmapEmpty = HeatmapEmpty,
    heatmapLevel1 = HeatmapLevel1,
    heatmapLevel2 = HeatmapLevel2,
    heatmapLevel3 = HeatmapLevel3,
    heatmapLevel4 = HeatmapLevel4,
    onHeatmapHigh = ZenBackground,
)

val LightZenPalette = ZenPalette(
    quiet = Color(0xFF3E6B58),
    standing = Color(0xFF9A7438),
    heatmapEmpty = Color(0xFFE7E1D4),
    heatmapLevel1 = Color(0xFFCBDED2),
    heatmapLevel2 = Color(0xFF9EC4AD),
    heatmapLevel3 = Color(0xFF6A9C82),
    heatmapLevel4 = Color(0xFF356B55),
    onHeatmapHigh = Color(0xFFF8F5EC),
)

val LocalZenPalette = staticCompositionLocalOf { DarkZenPalette }
