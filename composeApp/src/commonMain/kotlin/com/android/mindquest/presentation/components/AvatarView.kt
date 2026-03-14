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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.constants.AvatarConstants

@Composable
fun AvatarView(
    avatarId: Int,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    showOnlineIndicator: Boolean = false,
) {
    val avatarInfo = AvatarConstants.getAvatar(avatarId)
    val emojiFontSize = (size.value * 0.5f).sp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(avatarInfo.bgFrom), Color(avatarInfo.bgTo)),
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
                text = avatarInfo.emoji,
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
