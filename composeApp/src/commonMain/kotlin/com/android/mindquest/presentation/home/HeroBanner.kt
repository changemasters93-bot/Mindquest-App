package com.android.mindquest.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.domain.model.DailyChallenge

private val GoldGradientStart = Color(0xFFFFE259)
private val GoldGradientEnd = Color(0xFFFFA751)
private val DarkNavy = Color(0xFF0F172A)
private val DarkPurple = Color(0xFF1E1B4B)
private val DeepIndigo = Color(0xFF312E81)

@Composable
fun HeroBanner(
    dailyChallenges: List<DailyChallenge> = emptyList(),
    completedMissions: Int = 0,
    totalMissions: Int = 0,
    onStartChallenge: (DailyChallenge?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_decorations")
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedChallenge by remember { mutableStateOf<DailyChallenge?>(null) }

    val defaultChallenge = dailyChallenges.firstOrNull { !it.isDone }
    val activeChallenge = selectedChallenge ?: defaultChallenge

    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "star_alpha",
    )

    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "star_scale",
    )

    val rocketBob by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rocket_bob",
    )

    val dotOffset by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_offset",
    )

    val missionProgress = if (totalMissions > 0) {
        completedMissions.toFloat() / totalMissions
    } else 0f

    // ── Mission state: initial / in_progress / completed ──────────
    val allCompleted = totalMissions > 0 && completedMissions >= totalMissions
    val inProgress = totalMissions > 0 && completedMissions > 0 && !allCompleted
    // initial = everything else (totalMissions==0 or completedMissions==0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = if (allCompleted) {
                        listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF059669))
                    } else {
                        listOf(DarkNavy, DarkPurple, DeepIndigo)
                    },
                ),
            ),
    ) {
        // Star field canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .alpha(starAlpha),
        ) {
            val starColor = Color.White.copy(alpha = 0.25f)
            val dotColor = Color.White.copy(alpha = 0.15f)

            drawStar(Offset(size.width * 0.85f, size.height * 0.15f), 8f * starScale, starColor)
            drawStar(Offset(size.width * 0.12f, size.height * 0.3f), 6f * starScale, starColor)
            drawStar(Offset(size.width * 0.72f, size.height * 0.55f), 5f * starScale, starColor)
            drawStar(Offset(size.width * 0.92f, size.height * 0.7f), 7f * starScale, starColor)

            drawCircle(dotColor, 4f, Offset(size.width * 0.08f, size.height * 0.6f + dotOffset))
            drawCircle(dotColor, 3f, Offset(size.width * 0.95f, size.height * 0.4f + dotOffset))
            drawCircle(dotColor, 5f, Offset(size.width * 0.6f, size.height * 0.12f - dotOffset))
        }

        // Golden glow orb
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-72).dp, y = 12.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (allCompleted) {
                            listOf(Color(0xFF6EE7B7), Color(0xFF34D399))
                        } else {
                            listOf(Color(0xFFFFE066), Color(0xFFFFA700))
                        },
                    ),
                )
                .graphicsLayer {
                    alpha = 0.7f
                    translationY = rocketBob * 0.5f
                },
        )

        // Floating decoration — rocket for initial/in-progress, trophy for completed
        Text(
            text = if (allCompleted) "\uD83C\uDFC6" else "\uD83D\uDE80",
            fontSize = 36.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = (10 + rocketBob).dp)
                .graphicsLayer { rotationZ = if (allCompleted) 0f else -15f },
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Mission label
            Text(
                text = when {
                    allCompleted -> "\uD83C\uDF89 Mission Complete"
                    inProgress -> "\uD83C\uDFAF Today's Mission"
                    else -> "\uD83C\uDFAF Today's Mission"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── Title — varies by state ──────────────────────────────
            Text(
                text = when {
                    allCompleted -> "All Missions\nCompleted!"
                    inProgress -> "Keep Going,\nYou're Doing Great!"
                    else -> "Complete Daily\nChallenges!"
                },
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 26.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Subtitle — varies by state ───────────────────────────
            Text(
                text = when {
                    allCompleted -> "\u2B50 $totalMissions of $totalMissions missions done \u2014 Great job!"
                    inProgress -> "$completedMissions of $totalMissions missions completed"
                    totalMissions > 0 -> "$totalMissions missions waiting for you"
                    else -> "Earn XP by finishing today's quizzes"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = if (allCompleted) 0.7f else 0.45f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mission progress bar (always visible, full green when completed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(missionProgress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (allCompleted) {
                                    listOf(Color(0xFF6EE7B7), Color(0xFF34D399))
                                } else {
                                    listOf(GoldGradientStart, GoldGradientEnd)
                                },
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── CTA row — varies by state ────────────────────────────
            when {
                allCompleted -> {
                    // Completed state: celebratory CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6EE7B7), Color(0xFF34D399)),
                                    ),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onStartChallenge(null) },
                                )
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "\uD83C\uDF1F Come Back Tomorrow",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                            )
                        }
                    }
                }

                else -> {
                    // Initial & In-Progress: Start Challenge + optional dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Golden Start Challenge button
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GoldGradientStart, GoldGradientEnd),
                                    ),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onStartChallenge(activeChallenge) },
                                )
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "\uD83D\uDE80 Start Challenge",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                            )
                        }

                        // Select quiz dropdown toggle
                        if (dailyChallenges.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.18f),
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { dropdownExpanded = !dropdownExpanded }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = if (dropdownExpanded) "Hide \u25B4" else "Quizzes \u25BE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }

                    // ── Dropdown: Mission quiz list ──────────────────
                    AnimatedVisibility(
                        visible = dropdownExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            dailyChallenges.forEach { challenge ->
                                val isSelected = activeChallenge?.quizId == challenge.quizId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.12f)
                                            else Color.White.copy(alpha = 0.05f),
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.dp,
                                                GoldGradientEnd.copy(alpha = 0.4f),
                                                RoundedCornerShape(14.dp),
                                            ) else Modifier.border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(14.dp),
                                            ),
                                        )
                                        .clickable {
                                            if (!challenge.isDone) {
                                                selectedChallenge = challenge
                                                dropdownExpanded = false
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Status indicator
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (challenge.isDone) Color(0xFF22C55E)
                                                else if (isSelected) GoldGradientEnd
                                                else Color.White.copy(alpha = 0.3f),
                                            ),
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = challenge.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (challenge.isDone) Color.White.copy(alpha = 0.4f)
                                            else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${challenge.questionCount} Qs \u00B7 ${challenge.timeInMinutes} min",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.35f),
                                        )
                                    }

                                    if (challenge.isDone) {
                                        Text(
                                            text = "\u2714 Done",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF22C55E).copy(alpha = 0.7f),
                                        )
                                    } else if (isSelected) {
                                        Text(
                                            text = "Selected",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GoldGradientEnd,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path().apply {
        val innerRadius = radius * 0.4f
        val points = 4
        val angleStep = 360f / points

        for (i in 0 until points) {
            val outerAngle = (angleStep * i - 90).toDouble() * kotlin.math.PI / 180.0
            val innerAngle = (angleStep * i - 90 + angleStep / 2).toDouble() * kotlin.math.PI / 180.0

            val outerX = center.x + radius * kotlin.math.cos(outerAngle).toFloat()
            val outerY = center.y + radius * kotlin.math.sin(outerAngle).toFloat()
            val innerX = center.x + innerRadius * kotlin.math.cos(innerAngle).toFloat()
            val innerY = center.y + innerRadius * kotlin.math.sin(innerAngle).toFloat()

            if (i == 0) {
                moveTo(outerX, outerY)
            } else {
                lineTo(outerX, outerY)
            }
            lineTo(innerX, innerY)
        }
        close()
    }
    drawPath(path = path, color = color)
}
