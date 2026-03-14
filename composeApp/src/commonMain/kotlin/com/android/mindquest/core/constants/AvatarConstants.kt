package com.android.mindquest.core.constants

/**
 * Shared avatar configuration for the entire app.
 *
 * Used by: LoginJourneyScreen (avatar picker), ProfileScreen (user avatar),
 * LeaderboardScreen (other users' avatars), and any future avatar display.
 *
 * Avatar mapping is stored as plain data here so it can later be replaced
 * with a remote config fetch for cross-device consistency.
 */
data class AvatarInfo(
    val id: Int,
    val name: String,
    val emoji: String,
    val bgFrom: Long,
    val bgTo: Long,
)

object AvatarConstants {

    val AVATARS: List<AvatarInfo> = listOf(
        AvatarInfo(1, "Sunny", "\uD83C\uDF1E", 0xFFFF6B9D, 0xFFFF8DC7),
        AvatarInfo(2, "Blaze", "\uD83D\uDD25", 0xFF4FACFE, 0xFF2B86E8),
        AvatarInfo(3, "Coco", "\uD83D\uDC35", 0xFFA78BFA, 0xFF7C3AED),
        AvatarInfo(4, "Rio", "\uD83E\uDD81", 0xFF34D399, 0xFF059669),
        AvatarInfo(5, "Luna", "\uD83C\uDF19", 0xFFF59E0B, 0xFFD97706),
        AvatarInfo(6, "Zack", "\u26A1", 0xFFF87171, 0xFFDC2626),
        AvatarInfo(7, "Nova", "\u2B50", 0xFF8B5CF6, 0xFF6D28D9),
        AvatarInfo(8, "Miko", "\uD83E\uDD13", 0xFF06B6D4, 0xFF0891B2),
        AvatarInfo(9, "Pixel", "\uD83D\uDC7E", 0xFF6366F1, 0xFF4F46E5),   // Alien – Indigo
        AvatarInfo(10, "Panda", "\uD83D\uDC3C", 0xFF64748B, 0xFF475569),  // Panda – Slate
        AvatarInfo(11, "Rocket", "\uD83D\uDE80", 0xFFE11D48, 0xFFBE123C), // Rocket – Rose
        AvatarInfo(12, "Frosty", "\u2744\uFE0F", 0xFF38BDF8, 0xFF0EA5E9), // Snowflake – Sky
    )

    fun getAvatar(id: Int): AvatarInfo = AVATARS.find { it.id == id } ?: AVATARS[3]
}
