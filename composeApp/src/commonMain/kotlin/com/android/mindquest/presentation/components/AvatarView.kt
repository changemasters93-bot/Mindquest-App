package com.android.mindquest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AvatarColors(
    val primary: Color,
    val secondary: Color,
    val emoji: String,
)

private val avatarColorMap = mapOf(
    1 to AvatarColors(Color(0xFFEC4899), Color(0xFFF472B6), "\uD83E\uDDD1"),    // Pink
    2 to AvatarColors(Color(0xFF3B82F6), Color(0xFF60A5FA), "\uD83E\uDDD2"),    // Blue
    3 to AvatarColors(Color(0xFF8B5CF6), Color(0xFFA78BFA), "\uD83E\uDDD3"),    // Purple
    4 to AvatarColors(Color(0xFF10B981), Color(0xFF34D399), "\uD83E\uDDD4"),    // Green
    5 to AvatarColors(Color(0xFFF59E0B), Color(0xFFFBBF24), "\uD83E\uDDD5"),    // Amber
    6 to AvatarColors(Color(0xFFEF4444), Color(0xFFF87171), "\uD83E\uDDD6"),    // Red
    7 to AvatarColors(Color(0xFF7C3AED), Color(0xFF8B5CF6), "\uD83E\uDDD7"),    // Violet
    8 to AvatarColors(Color(0xFF06B6D4), Color(0xFF22D3EE), "\uD83E\uDDD8"),    // Cyan
)

@Composable
fun AvatarView(
    avatarId: Int,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    showOnlineIndicator: Boolean = false,
) {
    val colors = avatarColorMap[avatarId.coerceIn(1, 8)]
        ?: avatarColorMap[1]!!

    val emojiFontSize = (size.value * 0.5f).sp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(colors.primary, colors.secondary),
                    ),
                )
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = colors.emoji,
                fontSize = emojiFontSize,
            )
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
