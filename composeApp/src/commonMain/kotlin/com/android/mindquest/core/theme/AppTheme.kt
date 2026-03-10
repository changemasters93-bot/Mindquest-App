package com.android.mindquest.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
 * Root theme composable for Mindquest.
 *
 * Wraps [MaterialTheme] with the app's color scheme, typography and
 * provides [Dimensions] via [LocalDimensions].
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
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDimensions provides DefaultDimensions,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
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
