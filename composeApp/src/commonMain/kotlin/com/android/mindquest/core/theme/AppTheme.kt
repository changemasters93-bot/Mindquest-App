package com.android.mindquest.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 light color scheme wired to the Mindquest palette.
 */
private val LightColorScheme = lightColorScheme(
    primary = MindquestColors.Primary,
    onPrimary = MindquestColors.OnPrimary,
    primaryContainer = MindquestColors.PrimaryContainer,
    onPrimaryContainer = MindquestColors.PrimaryDark,
    secondary = MindquestColors.Secondary,
    onSecondary = MindquestColors.OnSecondary,
    secondaryContainer = MindquestColors.SecondaryContainer,
    onSecondaryContainer = MindquestColors.SecondaryDark,
    background = MindquestColors.Background,
    onBackground = MindquestColors.OnBackground,
    surface = MindquestColors.Surface,
    onSurface = MindquestColors.OnSurface,
    surfaceVariant = MindquestColors.SurfaceVariant,
    onSurfaceVariant = MindquestColors.TextSecondary,
    error = MindquestColors.Error,
    onError = MindquestColors.OnError,
    errorContainer = MindquestColors.ErrorContainer,
    onErrorContainer = MindquestColors.ErrorDark,
    outline = MindquestColors.Border,
    outlineVariant = MindquestColors.BorderLight,
)

/**
 * Material 3 dark color scheme wired to the Mindquest dark palette.
 */
private val DarkColorScheme = darkColorScheme(
    primary = MindquestColorsDark.Primary,
    onPrimary = MindquestColorsDark.OnPrimary,
    primaryContainer = MindquestColorsDark.PrimaryContainer,
    onPrimaryContainer = MindquestColorsDark.PrimaryDark,
    secondary = MindquestColorsDark.Secondary,
    onSecondary = MindquestColorsDark.OnSecondary,
    secondaryContainer = MindquestColorsDark.SecondaryContainer,
    onSecondaryContainer = MindquestColorsDark.SecondaryDark,
    background = MindquestColorsDark.Background,
    onBackground = MindquestColorsDark.OnBackground,
    surface = MindquestColorsDark.Surface,
    onSurface = MindquestColorsDark.OnSurface,
    surfaceVariant = MindquestColorsDark.SurfaceVariant,
    onSurfaceVariant = MindquestColorsDark.TextSecondary,
    error = MindquestColorsDark.Error,
    onError = MindquestColorsDark.OnError,
    errorContainer = MindquestColorsDark.ErrorContainer,
    onErrorContainer = MindquestColorsDark.ErrorDark,
    outline = MindquestColorsDark.Border,
    outlineVariant = MindquestColorsDark.BorderLight,
)

// ── Extra colors not covered by Material 3 slots ──────────────────────

/**
 * Custom Mindquest colors that go beyond Material 3's built-in slots.
 * Screens can access these via [LocalMindquestColors].
 */
data class MindquestExtraColors(
    val heroGradientStart: Color,
    val heroGradientCenter: Color,
    val heroGradientEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val xpBackground: Color,
    val xpText: Color,
)

private val LightMindquestExtraColors = MindquestExtraColors(
    heroGradientStart = MindquestColors.HeroGradientStart,
    heroGradientCenter = MindquestColors.HeroGradientCenter,
    heroGradientEnd = MindquestColors.HeroGradientEnd,
    textPrimary = MindquestColors.TextPrimary,
    textSecondary = MindquestColors.TextSecondary,
    textTertiary = MindquestColors.TextTertiary,
    textMuted = MindquestColors.TextMuted,
    xpBackground = MindquestColors.XpBackground,
    xpText = MindquestColors.XpText,
)

private val DarkMindquestExtraColors = MindquestExtraColors(
    heroGradientStart = MindquestColorsDark.HeroGradientStart,
    heroGradientCenter = MindquestColorsDark.HeroGradientCenter,
    heroGradientEnd = MindquestColorsDark.HeroGradientEnd,
    textPrimary = MindquestColorsDark.TextPrimary,
    textSecondary = MindquestColorsDark.TextSecondary,
    textTertiary = MindquestColorsDark.TextTertiary,
    textMuted = MindquestColorsDark.TextMuted,
    xpBackground = MindquestColorsDark.XpBackground,
    xpText = MindquestColorsDark.XpText,
)

val LocalMindquestColors = staticCompositionLocalOf { LightMindquestExtraColors }

/**
 * Root theme composable for Mindquest.
 *
 * Wraps [MaterialTheme] with the app's color scheme, typography and
 * provides [Dimensions] via [LocalDimensions] and custom extra colors
 * via [LocalMindquestColors].
 *
 * Dark mode is detected automatically via [isSystemInDarkTheme] but can
 * be overridden with the [darkTheme] parameter.
 *
 * Usage:
 * ```
 * MindquestTheme {
 *     // screen content
 * }
 * ```
 */
@Composable
fun MindquestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkMindquestExtraColors else LightMindquestExtraColors

    CompositionLocalProvider(
        LocalDimensions provides DefaultDimensions,
        LocalMindquestColors provides extraColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MindquestTypography,
            content = content,
        )
    }
}

/**
 * Convenience accessor so screens can write `MaterialTheme.dimens.spacingL`
 * instead of `LocalDimensions.current.spacingL`.
 */
val MaterialTheme.dimens: Dimensions
    @Composable
    get() = LocalDimensions.current

/**
 * Convenience accessor so screens can write `MaterialTheme.mindquestColors.xpText`
 * instead of `LocalMindquestColors.current.xpText`.
 */
val MaterialTheme.mindquestColors: MindquestExtraColors
    @Composable
    get() = LocalMindquestColors.current
