package com.android.mindquest.presentation.tournament

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors

// ═══════════════════════════════════════════════════════════
// TOURNAMENT PLAY — Quiz Screen (matches Tournament_Journey_New.jsx)
// ═══════════════════════════════════════════════════════════

private val AccentIndigo = Color(0xFF4F46E5)
private val AccentIndigoDark = Color(0xFF4338CA)
private val PlayCorrectGreen = Color(0xFF22C55E)
private val PlayCorrectGreenDark = Color(0xFF166534)
private val PlayCorrectGreenBg = Color(0xFFF0FDF4)
private val PlayIncorrectRed = Color(0xFFEF4444)
private val PlayIncorrectRedDark = Color(0xFF991B1B)
private val PlayIncorrectRedBg = Color(0xFFFEF2F2)
private val PlaySelectedBg = Color(0xFFEEF2FF)
private val PlayDefaultBg = Color(0xFFFAFAFA)
private val PlayDefaultBorder = Color(0xFFF0F0F5)
private val PlayTimerAmberBg = Color(0xFFFEF3C7)
private val PlayTimerAmberText = Color(0xFF92400E)
private val PlayTimerRedBg = Color(0xFFFEE2E2)
private val PlayTimerRedText = Color(0xFFDC2626)

@Composable
fun TournamentPlayScreen(
    viewModel: TournamentViewModel,
    onPause: () -> Unit,
    onFinish: () -> Unit,
) {
    val playState by viewModel.playState.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()

    if (playState.isFinished) {
        onFinish()
        return
    }

    val timerWarn = timeLeft < 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // ── Top Bar ──────────────────────────────────────────
        PlayTopBar(
            timeLeft = timeLeft,
            timerWarn = timerWarn,
            currentIndex = playState.currentQuestionIndex,
            totalQuestions = playState.totalQuestions,
            questionsAnswered = playState.questionsAnswered,
            onPause = onPause,
        )

        // ── Question Body ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            // Question label
            Text(
                text = "QUESTION ${playState.currentQuestionIndex + 1} OF ${playState.totalQuestions}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentIndigo,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question text
            Text(
                text = playState.questionText.ifEmpty { "Question ${playState.currentQuestionIndex + 1}" },
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MindquestColors.TextPrimary,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Options
            playState.options.forEachIndexed { index, option ->
                val optionState = when {
                    option.isRevealed && option.isCorrect == true -> PlayOptionState.CORRECT
                    option.isRevealed && option.isSelected -> PlayOptionState.INCORRECT
                    option.isSelected -> PlayOptionState.SELECTED
                    else -> PlayOptionState.DEFAULT
                }

                PlayOptionCard(
                    text = option.text,
                    letterIndex = index,
                    state = optionState,
                    onClick = { viewModel.selectOption(option.id) },
                    enabled = !playState.isConfirmed,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ── Bottom CTA ───────────────────────────────────────
        if (!playState.isConfirmed) {
            val hasSelection = playState.selectedOptionId != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
                    .shadow(
                        if (hasSelection) 6.dp else 0.dp,
                        RoundedCornerShape(14.dp),
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (hasSelection) Brush.linearGradient(
                            listOf(AccentIndigo, AccentIndigoDark),
                        )
                        else Brush.linearGradient(
                            listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)),
                        ),
                    )
                    .then(
                        if (hasSelection) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.confirmAnswer() },
                        ) else Modifier,
                    )
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Confirm Answer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasSelection) Color.White else Color(0xFF9CA3AF),
                    letterSpacing = (-0.3).sp,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TOP BAR — Pause | Center label + progress dots | Timer pill
// ═══════════════════════════════════════════════════════════

@Composable
private fun PlayTopBar(
    timeLeft: Int,
    timerWarn: Boolean,
    currentIndex: Int,
    totalQuestions: Int,
    questionsAnswered: Int,
    onPause: () -> Unit,
) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    // Pulse animation for timer warning
    val infiniteTransition = rememberInfiniteTransition(label = "play_timer_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "play_pulse",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pause button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { onPause() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "\u23F8 Pause",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280),
                )
            }

            // Center: label + segmented progress dots
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u2694\uFE0F Tournament",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9CA3AF),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until totalQuestions.coerceAtMost(15)) {
                        val dotColor = when {
                            i < questionsAnswered -> AccentIndigo
                            i == currentIndex -> AccentIndigo
                            else -> Color(0xFFE5E7EB)
                        }
                        val dotWidth = if (i == currentIndex) 18.dp else 10.dp
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(dotColor),
                        )
                    }
                }
            }

            // Timer pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (timerWarn) PlayTimerRedBg else PlayTimerAmberBg)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .graphicsLayer {
                        alpha = if (timerWarn) pulseAlpha else 1f
                    },
            ) {
                Text(
                    text = "\u23F1 ${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (timerWarn) PlayTimerRedText else PlayTimerAmberText,
                )
            }
        }

        // Bottom divider
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF3F4F6)),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// OPTION CARD — Letter badge (A/B/C/D) with animated states
// ═══════════════════════════════════════════════════════════

private enum class PlayOptionState { DEFAULT, SELECTED, CORRECT, INCORRECT }

@Composable
private fun PlayOptionCard(
    text: String,
    letterIndex: Int,
    state: PlayOptionState,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val letter = ('A' + letterIndex).toString()

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.SELECTED -> AccentIndigo
            PlayOptionState.CORRECT -> PlayCorrectGreen
            PlayOptionState.INCORRECT -> PlayIncorrectRed
            PlayOptionState.DEFAULT -> PlayDefaultBorder
        },
        animationSpec = tween(200),
        label = "play_opt_border",
    )

    val bgColor by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.SELECTED -> PlaySelectedBg
            PlayOptionState.CORRECT -> PlayCorrectGreenBg
            PlayOptionState.INCORRECT -> PlayIncorrectRedBg
            PlayOptionState.DEFAULT -> PlayDefaultBg
        },
        animationSpec = tween(200),
        label = "play_opt_bg",
    )

    val textColor by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.SELECTED -> AccentIndigo
            PlayOptionState.CORRECT -> PlayCorrectGreenDark
            PlayOptionState.INCORRECT -> PlayIncorrectRedDark
            PlayOptionState.DEFAULT -> Color(0xFF374151)
        },
        animationSpec = tween(200),
        label = "play_opt_text",
    )

    val badgeBg by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.SELECTED -> AccentIndigo
            PlayOptionState.CORRECT -> PlayCorrectGreen
            PlayOptionState.INCORRECT -> PlayIncorrectRed
            PlayOptionState.DEFAULT -> Color.White
        },
        animationSpec = tween(200),
        label = "play_badge_bg",
    )

    val badgeBorderColor by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.SELECTED -> AccentIndigo
            PlayOptionState.CORRECT -> PlayCorrectGreen
            PlayOptionState.INCORRECT -> PlayIncorrectRed
            PlayOptionState.DEFAULT -> Color(0xFFD1D5DB)
        },
        animationSpec = tween(200),
        label = "play_badge_border",
    )

    val badgeTextColor by animateColorAsState(
        targetValue = when (state) {
            PlayOptionState.DEFAULT -> Color(0xFF9CA3AF)
            else -> Color.White
        },
        animationSpec = tween(200),
        label = "play_badge_text",
    )

    val scale by animateFloatAsState(
        targetValue = if (state == PlayOptionState.SELECTED) 1.01f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "play_opt_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (state == PlayOptionState.DEFAULT) 1.dp else 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Letter badge circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(2.dp, badgeBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (state) {
                        PlayOptionState.CORRECT -> "\u2713"
                        PlayOptionState.INCORRECT -> "\u2717"
                        else -> letter
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }

            // Option text
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (state != PlayOptionState.DEFAULT) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
            )
        }
    }
}
