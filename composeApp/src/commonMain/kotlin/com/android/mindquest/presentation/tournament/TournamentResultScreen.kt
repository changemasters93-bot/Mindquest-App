package com.android.mindquest.presentation.tournament

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.presentation.components.LoadingView
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.components.SecondaryButton
import kotlinx.coroutines.delay

@Composable
fun TournamentResultScreen(
    viewModel: TournamentViewModel,
    onViewLeaderboard: () -> Unit,
    onExit: () -> Unit,
    userId: String = "current_user",
) {
    val entryState by viewModel.entryState.collectAsState()
    val playState by viewModel.playState.collectAsState()

    // If navigated to directly (not from quiz play), load result data
    LaunchedEffect(Unit) {
        if (entryState is UiState.Empty) {
            viewModel.loadTournamentResult(userId)
        }
    }

    when (val state = entryState) {
        is UiState.Loading -> LoadingView(message = "Submitting results...")
        is UiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = state.message, color = MindquestColors.Error)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(text = "Continue", onClick = onExit)
            }
        }
        is UiState.Success -> {
            val entry = state.data
            TournamentResultContent(
                rank = entry.rank ?: 0,
                score = entry.score,
                totalQuestions = playState.totalQuestions,
                timeTakenSeconds = entry.timeTakenSeconds ?: 0,
                participantCount = (viewModel.tournamentState.value as? UiState.Success)?.data?.participantCount ?: 0,
                onViewLeaderboard = onViewLeaderboard,
                onExit = onExit,
            )
        }
        else -> {}
    }
}

@Composable
private fun TournamentResultContent(
    rank: Int,
    score: Int,
    totalQuestions: Int,
    timeTakenSeconds: Int,
    participantCount: Int,
    onViewLeaderboard: () -> Unit,
    onExit: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "result")
    val medalBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "medal_bounce",
    )

    val medalEmoji = when (rank) {
        1 -> "\uD83E\uDD47"
        2 -> "\uD83E\uDD48"
        3 -> "\uD83E\uDD49"
        else -> "\uD83C\uDFC5"
    }

    val isTopRank = rank in 1..3
    val accuracyPct = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
    val timeMins = timeTakenSeconds / 60
    val timeSecs = timeTakenSeconds % 60
    val stars = when {
        accuracyPct >= 90 -> 5
        accuracyPct >= 75 -> 4
        accuracyPct >= 60 -> 3
        accuracyPct >= 40 -> 2
        else -> 1
    }
    val xpEarned = score * 10 + 30 + if (accuracyPct == 100) 50 else 0

    // Always use dark gradient for results (matches JSX)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF7C3AED),
                        Color(0xFF4F46E5),
                        Color(0xFF0F172A),
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 🎉 + Title
        AnimatedVisibility(
            visible = showContent,
            enter = scaleIn(tween(600)) + fadeIn(tween(600)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\uD83C\uDF89",
                    fontSize = 42.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tournament Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (rank) {
                        1 -> "Champion!"
                        2 -> "Runner Up!"
                        3 -> "Third Place!"
                        else -> "Great Effort!"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Star rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 1..5) {
                        Text(
                            text = "\u2B50",
                            fontSize = if (i <= stars) 28.sp else 22.sp,
                            modifier = Modifier.graphicsLayer {
                                alpha = if (i <= stars) 1f else 0.25f
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score/$totalQuestions",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Text(
                            text = "$accuracyPct% accuracy",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rank display
                if (rank > 0) {
                    Text(
                        text = "Rank #$rank",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (rank) {
                            1 -> MindquestColors.Gold
                            2 -> MindquestColors.Silver
                            3 -> MindquestColors.Bronze
                            else -> Color.White
                        },
                    )
                    if (participantCount > 0) {
                        Text(
                            text = "out of $participantCount participants",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row: XP · Time · Rank
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600, delayMillis = 300)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultStatPill(
                    emoji = "\u26A1",
                    value = "+$xpEarned",
                    label = "XP Earned",
                    modifier = Modifier.weight(1f),
                )
                ResultStatPill(
                    emoji = "\u23F1",
                    value = "${timeMins}m ${timeSecs}s",
                    label = "Time",
                    modifier = Modifier.weight(1f),
                )
                ResultStatPill(
                    emoji = "\uD83C\uDFC5",
                    value = if (rank > 0) "#$rank" else "-",
                    label = "Your Rank",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Trophy/Certificate banner for top 3
        if (isTopRank) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 450)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
                            ),
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "\uD83C\uDF93", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Certificate of ${when(rank) { 1 -> "Excellence"; 2 -> "Achievement"; else -> "Merit" }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Buttons
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600, delayMillis = 600)),
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                PrimaryButton(
                    text = "View Leaderboard",
                    onClick = onViewLeaderboard,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "Continue",
                    onClick = onExit,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ResultStatPill(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}
