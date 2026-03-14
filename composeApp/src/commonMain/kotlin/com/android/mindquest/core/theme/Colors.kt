package com.android.mindquest.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Mindquest color system.
 *
 * Organized by role so every screen pulls from the same palette.
 * A single [MindquestColors] object keeps things easy to reference
 * while still allowing future light/dark switching.
 */
object MindquestColors {

    // ── Primary ────────────────────────────────────────────────────────
    val Primary = Color(0xFF4F46E5)
    val PrimaryDark = Color(0xFF4338CA)
    val PrimaryLight = Color(0xFF6366F1)
    val PrimaryContainer = Color(0xFFE0E7FF)
    val OnPrimary = Color.White

    // ── Secondary ──────────────────────────────────────────────────────
    val Secondary = Color(0xFF7C3AED)
    val SecondaryDark = Color(0xFF6D28D9)
    val SecondaryContainer = Color(0xFFEDE9FE)
    val OnSecondary = Color.White

    // ── Background / Surface ───────────────────────────────────────────
    val Background = Color(0xFFF8F9FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF3F4F6)
    val OnBackground = Color(0xFF111827)
    val OnSurface = Color(0xFF111827)

    // ── Hero gradient (dark blue → indigo) ─────────────────────────────
    val HeroGradientStart = Color(0xFF0F172A)
    val HeroGradientCenter = Color(0xFF1E1B4B)
    val HeroGradientEnd = Color(0xFF312E81)

    // ── Success ────────────────────────────────────────────────────────
    val Success = Color(0xFF22C55E)
    val SuccessDark = Color(0xFF16A34A)
    val SuccessAlt = Color(0xFF10B981)
    val SuccessContainer = Color(0xFFDCFCE7)
    val OnSuccess = Color.White

    // ── Warning ────────────────────────────────────────────────────────
    val Warning = Color(0xFFF59E0B)
    val WarningDark = Color(0xFFD97706)
    val WarningContainer = Color(0xFFFEF3C7)
    val OnWarning = Color.White

    // ── Error ──────────────────────────────────────────────────────────
    val Error = Color(0xFFEF4444)
    val ErrorDark = Color(0xFFDC2626)
    val ErrorContainer = Color(0xFFFEE2E2)
    val OnError = Color.White

    // ── Text ───────────────────────────────────────────────────────────
    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF374151)
    val TextTertiary = Color(0xFF6B7280)
    val TextMuted = Color(0xFF9CA3AF)
    val TextOnDark = Color.White

    // ── Module / Subject colors ────────────────────────────────────────
    val ModuleMath = Color(0xFF6366F1)
    val ModuleScience = Color(0xFF10B981)
    val ModuleEnglish = Color(0xFFF59E0B)
    val ModuleLogic = Color(0xFF8B5CF6)
    val ModuleSocial = Color(0xFF0EA5E9)

    // ── Leaderboard metals ─────────────────────────────────────────────
    val Gold = Color(0xFFFFB800)
    val Silver = Color(0xFF9BA8B8)
    val Bronze = Color(0xFFC0784A)

    // ── XP pill ────────────────────────────────────────────────────────
    val XpGradientStart = Color(0xFFFBBF24)
    val XpGradientEnd = Color(0xFFF59E0B)
    val XpBackground = Color(0xFFFFFBEB)
    val XpText = Color(0xFF92400E)

    // ── Tournament gradient ─────────────────────────────────────────────
    val TournamentGradientStart = Color(0xFF4F46E5)
    val TournamentGradientEnd = Color(0xFF7C3AED)

    // ── Borders & Dividers ─────────────────────────────────────────────
    val Border = Color(0xFFE5E7EB)
    val Divider = Color(0xFFE5E7EB)
    val BorderLight = Color(0xFFF3F4F6)

    // ── Gamification ─────────────────────────────────────────────────────
    val XpGold = Gold
    val StreakOrange = Color(0xFFF97316)
}

/**
 * Light-mode palette. A kids' app keeps things bright and cheerful.
 * Dark-mode can reuse the same values for now and diverge later.
 */
object MindquestLightColors {
    val primary = MindquestColors.Primary
    val primaryDark = MindquestColors.PrimaryDark
    val secondary = MindquestColors.Secondary
    val background = MindquestColors.Background
    val surface = MindquestColors.Surface
    val error = MindquestColors.Error
    val onPrimary = MindquestColors.OnPrimary
    val onSecondary = MindquestColors.OnSecondary
    val onBackground = MindquestColors.OnBackground
    val onSurface = MindquestColors.OnSurface
    val onError = MindquestColors.OnError
}

/**
 * Dark-mode palette with proper dark-bg-friendly colors.
 */
object MindquestDarkColors {
    val primary = Color(0xFF818CF8)       // Lighter indigo for dark bg
    val primaryDark = Color(0xFF6366F1)
    val secondary = Color(0xFFA78BFA)
    val background = Color(0xFF0F172A)    // Dark navy
    val surface = Color(0xFF1E293B)       // Dark slate
    val error = Color(0xFFFCA5A5)         // Lighter red for dark bg
    val onPrimary = Color.White
    val onSecondary = Color.White
    val onBackground = Color(0xFFF1F5F9)  // Light text on dark
    val onSurface = Color(0xFFF1F5F9)
    val onError = Color(0xFF7F1D1D)
}

/**
 * Full dark-mode palette — mirrors [MindquestColors] structure with
 * dark-background-friendly values for every role.
 */
object MindquestColorsDark {

    // ── Primary ────────────────────────────────────────────────────────
    val Primary = Color(0xFF818CF8)
    val PrimaryDark = Color(0xFF6366F1)
    val PrimaryLight = Color(0xFFA5B4FC)
    val PrimaryContainer = Color(0xFF312E81)
    val OnPrimary = Color.White

    // ── Secondary ──────────────────────────────────────────────────────
    val Secondary = Color(0xFFA78BFA)
    val SecondaryDark = Color(0xFF8B5CF6)
    val SecondaryContainer = Color(0xFF4C1D95)
    val OnSecondary = Color.White

    // ── Background / Surface ───────────────────────────────────────────
    val Background = Color(0xFF0F172A)
    val Surface = Color(0xFF1E293B)
    val SurfaceVariant = Color(0xFF334155)
    val OnBackground = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFFF1F5F9)

    // ── Hero gradient (dark blue → deep indigo) ─────────────────────
    val HeroGradientStart = Color(0xFF020617)
    val HeroGradientCenter = Color(0xFF0F172A)
    val HeroGradientEnd = Color(0xFF1E1B4B)

    // ── Success ────────────────────────────────────────────────────────
    val Success = Color(0xFF4ADE80)
    val SuccessDark = Color(0xFF22C55E)
    val SuccessAlt = Color(0xFF34D399)
    val SuccessContainer = Color(0xFF14532D)
    val OnSuccess = Color.White

    // ── Warning ────────────────────────────────────────────────────────
    val Warning = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFF59E0B)
    val WarningContainer = Color(0xFF78350F)
    val OnWarning = Color(0xFF1C1917)

    // ── Error ──────────────────────────────────────────────────────────
    val Error = Color(0xFFFCA5A5)
    val ErrorDark = Color(0xFFEF4444)
    val ErrorContainer = Color(0xFF7F1D1D)
    val OnError = Color(0xFF1C1917)

    // ── Text ───────────────────────────────────────────────────────────
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextTertiary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val TextOnDark = Color.White

    // ── Borders & Dividers ─────────────────────────────────────────────
    val Border = Color(0xFF334155)
    val Divider = Color(0xFF334155)
    val BorderLight = Color(0xFF475569)

    // ── XP pill ────────────────────────────────────────────────────────
    val XpBackground = Color(0xFF422006)
    val XpText = Color(0xFFFDE68A)
}
