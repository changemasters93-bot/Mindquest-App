package com.android.mindquest.presentation.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.domain.model.NudgeHintType
import com.android.mindquest.domain.model.NudgeState

/**
 * Animated nudge/hint card shown after a wrong answer in MODULE quiz mode.
 *
 * Provides contextual encouragement and optional hints to help the user
 * learn from mistakes. Animates in with a slide + fade for gentle attention.
 *
 * @param nudge      The nudge state with message and hint type
 * @param accentColor Module accent color for theming
 */
@Composable
fun NudgeCard(
    nudge: NudgeState,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val (emoji, title, bgColor, borderColor) = when (nudge.hintType) {
        NudgeHintType.ENCOURAGEMENT -> NudgeStyle(
            emoji = "\uD83D\uDCAA",
            title = "Keep Going!",
            bgColor = Color(0xFFFFF7ED), // warm orange tint
            borderColor = Color(0xFFFBBF24),
        )
        NudgeHintType.ELIMINATION_HINT -> NudgeStyle(
            emoji = "\uD83D\uDCA1",
            title = "Here's a Hint",
            bgColor = Color(0xFFF0F9FF), // light blue tint
            borderColor = Color(0xFF3B82F6),
        )
        NudgeHintType.CATEGORY_HINT -> NudgeStyle(
            emoji = "\uD83C\uDFAF",
            title = "Quick Tip",
            bgColor = Color(0xFFF0FDF4), // light green tint
            borderColor = Color(0xFF10B981),
        )
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = borderColor,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = nudge.message,
                    fontSize = 13.sp,
                    color = MindquestColors.TextSecondary,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

/** Internal styling helper for nudge card variants. */
private data class NudgeStyle(
    val emoji: String,
    val title: String,
    val bgColor: Color,
    val borderColor: Color,
)
