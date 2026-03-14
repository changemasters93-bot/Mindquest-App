package com.android.mindquest.presentation.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.presentation.components.PrimaryButton
import kotlinx.coroutines.delay

// IQ Level data for result classification
private data class IqResultLevel(
    val title: String,
    val emoji: String,
    val minScore: Int,
    val maxScore: Int,
    val message: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val IQ_RESULT_LEVELS = listOf(
    IqResultLevel(
        "Rising Star", "\uD83C\uDF31", 55, 84,
        "You're building a strong foundation! Keep challenging yourself.",
        Color(0xFF6EE7B7), Color(0xFF059669),
    ),
    IqResultLevel(
        "Sharp Thinker", "\u26A1", 85, 114,
        "Sharp analytical skills! You think clearly and methodically.",
        Color(0xFF818CF8), Color(0xFF4F46E5),
    ),
    IqResultLevel(
        "Brain Explorer", "\uD83D\uDD2D", 115, 124,
        "Impressive problem solving! Your mind works in creative ways.",
        Color(0xFF34D399), Color(0xFF059669),
    ),
    IqResultLevel(
        "Genius Zone", "\uD83D\uDE80", 125, 134,
        "Outstanding cognitive abilities! You're in the top tier.",
        Color(0xFFFBBF24), Color(0xFFD97706),
    ),
    IqResultLevel(
        "Mastermind", "\uD83C\uDFC6", 135, 160,
        "Extraordinary intellect! You're among the very best.",
        Color(0xFFF472B6), Color(0xFFDB2777),
    ),
)

private fun getIqLevel(score: Int): IqResultLevel =
    IQ_RESULT_LEVELS.find { score in it.minScore..it.maxScore }
        ?: IQ_RESULT_LEVELS.first()

@Composable
fun IqResultContent(
    iqScore: Int,
    score: Int,
    totalQuestions: Int,
    xpEarned: Int,
    onExit: () -> Unit,
) {
    val level = remember(iqScore) { getIqLevel(iqScore) }

    // Animated counter
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(iqScore) {
        delay(400) // let entrance animation settle
        animatedScore.animateTo(
            targetValue = iqScore.toFloat(),
            animationSpec = tween(1500, easing = EaseOutBack),
        )
    }

    // Staggered entrance animations
    var showScore by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showScore = true
        delay(500)
        showLevel = true
        delay(400)
        showStats = true
        delay(400)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF064E3B),
                        Color(0xFF065F46),
                        Color(0xFF0F172A),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // IQ Score section
            AnimatedVisibility(
                visible = showScore,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = tween(700, easing = EaseOutBack),
                ) + fadeIn(tween(500)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\uD83E\uDDE0",
                        fontSize = 60.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your IQ Score",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Big animated score number
                    Text(
                        text = "${animatedScore.value.toInt()}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Level badge
            AnimatedVisibility(
                visible = showLevel,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(500, easing = EaseOutBack),
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Level badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        level.gradientStart.copy(alpha = 0.25f),
                                        level.gradientEnd.copy(alpha = 0.25f),
                                    ),
                                ),
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = level.emoji,
                                fontSize = 24.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = level.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = level.gradientStart,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Motivational message
                    Text(
                        text = level.message,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stat pills
            AnimatedVisibility(
                visible = showStats,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(500, easing = EaseOutBack),
                ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Score + XP pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IqStatPill(
                            emoji = "\uD83C\uDFAF",
                            label = "Score",
                            value = "$score/$totalQuestions",
                            modifier = Modifier.weight(1f),
                        )
                        IqStatPill(
                            emoji = "\u26A1",
                            label = "XP Earned",
                            value = "+$xpEarned",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Cooldown nudge card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6EE7B7).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "\uD83D\uDD04", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Come Back Stronger",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Try again in 7 days to improve your score",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Continue button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(500, easing = EaseOutBack),
                ),
            ) {
                PrimaryButton(
                    text = "Continue",
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun IqStatPill(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
