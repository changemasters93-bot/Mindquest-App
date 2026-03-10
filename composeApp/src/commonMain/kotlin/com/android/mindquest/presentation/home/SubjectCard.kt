package com.android.mindquest.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.domain.model.Module
import com.android.mindquest.presentation.components.MindquestProgressBar

// ── Colors aligned with Chapters_New.jsx SUBJECTS palette ─────────────────
private val subjectAccentColors = mapOf(
    "math" to Pair(Color(0xFF6366F1), Color(0xFF4F46E5)),
    "mathematics" to Pair(Color(0xFF6366F1), Color(0xFF4F46E5)),
    "science" to Pair(Color(0xFF10B981), Color(0xFF059669)),
    "english" to Pair(Color(0xFFF59E0B), Color(0xFFD97706)),
    "social" to Pair(Color(0xFF0EA5E9), Color(0xFF0284C7)),
    "social studies" to Pair(Color(0xFF0EA5E9), Color(0xFF0284C7)),
    "hindi" to Pair(Color(0xFFEF4444), Color(0xFFDC2626)),
    "computer" to Pair(Color(0xFF06B6D4), Color(0xFF0891B2)),
    "gk" to Pair(Color(0xFFEC4899), Color(0xFFDB2777)),
    "general knowledge" to Pair(Color(0xFFEC4899), Color(0xFFDB2777)),
    "reasoning" to Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
    "logical reasoning" to Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
)

private val subjectEmojis = mapOf(
    "math" to "\uD83D\uDCD0",           // 📐
    "mathematics" to "\uD83D\uDCD0",    // 📐
    "science" to "\uD83D\uDD2C",        // 🔬
    "english" to "\uD83D\uDCD6",        // 📖
    "social" to "\uD83C\uDF0D",         // 🌍
    "social studies" to "\uD83C\uDF0D", // 🌍
    "hindi" to "\uD83D\uDCDD",          // 📝
    "computer" to "\uD83D\uDCBB",       // 💻
    "gk" to "\uD83D\uDCA1",             // 💡
    "general knowledge" to "\uD83D\uDCA1", // 💡
    "reasoning" to "\uD83E\uDDE9",      // 🧩
    "logical reasoning" to "\uD83E\uDDE9", // 🧩
)

@Composable
fun SubjectCard(
    module: Module,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "card_press_scale",
    )

    val moduleKey = module.title.lowercase().trim()
    val (accentColor, accentDark) = subjectAccentColors[moduleKey]
        ?: Pair(Color(0xFF4F46E5), Color(0xFF4338CA))
    val emoji = subjectEmojis[moduleKey] ?: module.emoji

    val bestScore = module.progress?.bestScorePct ?: 0
    val isCompleted = module.progress?.isCompleted == true
    val isNotStarted = module.progress == null || bestScore == 0
    val progress = bestScore / 100f
    val percentage = if (isCompleted) 100 else bestScore

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.1f),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // Gradient top section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .background(
                        brush = Brush.linearGradient(
                            if (isNotStarted) {
                                listOf(accentColor.copy(alpha = 0.6f), accentDark.copy(alpha = 0.6f))
                            } else {
                                listOf(accentColor, accentDark)
                            },
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Completed checkmark badge (top-right)
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u2714",
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                }

                // Subject emoji
                Text(
                    text = emoji,
                    fontSize = 40.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isNotStarted) 0.7f else 1f
                    },
                )
            }

            // Bottom white section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    text = module.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.subtitle ?: "",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(10.dp))

                MindquestProgressBar(
                    progress = if (isCompleted) 1f else progress,
                    color = when {
                        isCompleted -> Color(0xFF22C55E)
                        isNotStarted -> Color(0xFFD1D5DB)
                        else -> accentColor
                    },
                    height = 5.dp,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            isCompleted -> "\u2705 Completed"
                            isNotStarted -> "Not Started"
                            else -> "In Progress"
                        },
                        fontSize = 11.sp,
                        color = when {
                            isCompleted -> Color(0xFF16A34A)
                            isNotStarted -> Color(0xFFD1D5DB)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isCompleted -> Color(0xFF22C55E).copy(alpha = 0.1f)
                                    isNotStarted -> Color(0xFFF3F4F6)
                                    else -> accentColor.copy(alpha = 0.1f)
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isCompleted -> Color(0xFF22C55E)
                                isNotStarted -> Color(0xFFD1D5DB)
                                else -> accentColor
                            },
                        )
                    }
                }
            }
        }
    }
}
