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
 * Dark-mode palette stub — mirrors light for now.
 */
object MindquestDarkColors {
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
