package com.jinggong.timer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Bamboo,
    onPrimary = ZenBackground,
    primaryContainer = BambooContainer,
    onPrimaryContainer = Bamboo,
    secondary = Sand,
    onSecondary = ZenBackground,
    secondaryContainer = SandContainer,
    onSecondaryContainer = Sand,
    tertiary = Mist,
    onTertiary = ZenBackground,
    background = ZenBackground,
    onBackground = TextPrimary,
    surface = ZenSurface,
    onSurface = TextPrimary,
    surfaceVariant = ZenSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ZenSurface,
    surfaceContainerHigh = ZenSurfaceHigh,
    surfaceContainerHighest = ZenSurfaceHighest,
    outline = OutlineZen,
    outlineVariant = OutlineFaint,
    error = ErrorZen,
    onError = ZenBackground,
    errorContainer = ErrorContainerZen,
    onErrorContainer = ErrorZen,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF173A2D),
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Color(0xFF3D2F16),
    tertiary = Color(0xFF58645E),
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightSurface,
    errorContainer = LightErrorContainer,
    onErrorContainer = Color(0xFF5A1712),
)

private val ZenShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * 全应用主题。默认跟随调用方传入的深色/浅色选择，并同步状态栏、导航栏图标颜色。
 */
@Composable
fun JingGongTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalZenPalette provides if (darkTheme) DarkZenPalette else LightZenPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = JingGongTypography,
            shapes = ZenShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
