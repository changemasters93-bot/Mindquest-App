package com.android.mindquest.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralised spacing, radius and sizing tokens for the Mindquest design system.
 *
 * Provided via [LocalDimensions] so any composable in the tree can access
 * values without passing them as parameters.
 */
@Immutable
data class Dimensions(

    // ── Spacing ────────────────────────────────────────────────────────
    val spacingXXS: Dp = 2.dp,
    val spacingXS: Dp = 4.dp,
    val spacingS: Dp = 8.dp,
    val spacingM: Dp = 12.dp,
    val spacingL: Dp = 16.dp,
    val spacingXL: Dp = 20.dp,
    val spacingXXL: Dp = 24.dp,
    val spacingHuge: Dp = 32.dp,

    // ── Radius ─────────────────────────────────────────────────────────
    val radiusSmall: Dp = 8.dp,
    val radiusMedium: Dp = 14.dp,
    val radiusLarge: Dp = 18.dp,
    val radiusXL: Dp = 20.dp,
    val radiusRound: Dp = 50.dp,

    // ── Component-specific radii ───────────────────────────────────────
    val cardRadius: Dp = 20.dp,
    val buttonRadius: Dp = 14.dp,
    val chipRadius: Dp = 20.dp,
    val bottomSheetRadius: Dp = 24.dp,

    // ── Avatar sizes ───────────────────────────────────────────────────
    val avatarSizeSmall: Dp = 34.dp,
    val avatarSizeMedium: Dp = 44.dp,
    val avatarSizeLarge: Dp = 72.dp,

    // ── Icon sizes ─────────────────────────────────────────────────────
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 22.dp,
    val iconSizeLarge: Dp = 44.dp,

    // ── Navigation ─────────────────────────────────────────────────────
    val bottomNavHeight: Dp = 80.dp,

    // ── Progress bars ──────────────────────────────────────────────────
    val progressBarHeight: Dp = 6.dp,
    val progressBarHeightLarge: Dp = 8.dp,

    // ── Button ─────────────────────────────────────────────────────────
    val buttonHeight: Dp = 50.dp,
)

/**
 * Default dimensions instance used across the app.
 */
val DefaultDimensions = Dimensions()

/**
 * CompositionLocal providing [Dimensions] down the tree.
 *
 * Access via `LocalDimensions.current` or the convenience extension
 * `MaterialTheme.dimens` defined in [AppTheme.kt].
 */
val LocalDimensions = staticCompositionLocalOf { DefaultDimensions }
