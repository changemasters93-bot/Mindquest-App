package com.android.mindquest.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock

private val IqDark1 = Color(0xFF0F172A)
private val IqDark2 = Color(0xFF1E293B)
private val IqDark3 = Color(0xFF0F2744)
private val IqGreen1 = Color(0xFF34D399)
private val IqGreen2 = Color(0xFF10B981)
private val IqGreenText = Color(0xFF064E3B)

@Composable
fun IqTestCard(
    onStartIqTest: () -> Unit,
    lastAttemptDateMillis: Long? = null,
    cooldownHours: Int = 168,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "iq_card")

    val medalBob by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "medal_bob",
    )

    // Calculate availability using configurable cooldown
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val cooldownMs = cooldownHours.toLong() * 3600 * 1000
    val isAvailable = lastAttemptDateMillis == null ||
        (nowMillis - lastAttemptDateMillis) >= cooldownMs
    val daysUntilAvailable = if (!isAvailable) {
        val elapsed = nowMillis - (lastAttemptDateMillis ?: nowMillis)
        val remaining = cooldownMs - elapsed
        ((remaining / (24 * 3600 * 1000)) + 1).toInt().coerceAtLeast(1)
    } else 0

    Box(
        modifier = modifier
            .semantics { contentDescription = "Take IQ Test" }
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(IqDark1, IqDark2, IqDark3),
                ),
            )
            .then(
                if (isAvailable) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStartIqTest,
                ) else Modifier,
            ),
    ) {
        // Glow blob
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: text content
            Column(modifier = Modifier.weight(1f)) {
                // Label
                Text(
                    text = "Official Assessment",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.2.sp,
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Title
                Text(
                    text = "\uD83E\uDDE0 IQ Test",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = if (isAvailable) "Measure your true potential"
                    else "Available in $daysUntilAvailable day${if (daysUntilAvailable != 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.45f),
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isAvailable) {
                    // Green CTA button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(
                                    listOf(IqGreen1, IqGreen2),
                                ),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onStartIqTest,
                            )
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Take Test \u2192",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IqGreenText,
                            letterSpacing = (-0.2).sp,
                        )
                    }
                } else {
                    // Cooldown indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "\u23F3 Cooldown \u00B7 $daysUntilAvailable day${if (daysUntilAvailable != 1) "s" else ""} left",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: medal emoji
            Text(
                text = "\uD83C\uDFC5",
                fontSize = 50.sp,
                modifier = Modifier
                    .offset(y = medalBob.dp)
                    .graphicsLayer {
                        shadowElevation = 10f
                    },
            )
        }
    }
}
