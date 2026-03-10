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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.model.TournamentEntryStatus
import com.android.mindquest.domain.model.TournamentStatus
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

// ═══════════════════════════════════════════════════════════════════
// TOURNAMENT BANNER STATES
// ═══════════════════════════════════════════════════════════════════
private enum class TournamentDisplayState {
    UPCOMING,       // scheduled, not yet started
    PLAY,           // live, user hasn't started
    RESUME,         // live, user in-progress
    COMPLETED,      // live/closed, user completed
    ANONYMOUS,      // live but user is anonymous → needs auth
    ENDED,          // closed/finalized, user never played → "next tournament coming soon"
    HIDDEN,         // no active tournament at all
}

private val BannerDark1 = Color(0xFF1E1B4B)
private val BannerDark2 = Color(0xFF312E81)
private val BannerDark3 = Color(0xFF4C1D95)

private val LiveGreen = Color(0xFF22C55E)
private val UpcomingYellow = Color(0xFFF59E0B)
private val InProgressYellow = Color(0xFFF59E0B)
private val CompletedIndigo = Color(0xFF4F46E5)
private val PurpleCta = Color(0xFF7C3AED)
private val DisabledGray = Color(0xFF9CA3AF)

@Composable
fun TournamentHomeBanner(
    tournament: Tournament,
    entry: TournamentEntry? = null,
    isAnonymous: Boolean = false,
    onPlay: () -> Unit = {},
    onResume: () -> Unit = {},
    onViewResults: () -> Unit = {},
    onLinkAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tournament_banner")

    val trophyBob by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophy_bob",
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live_pulse",
    )

    // Countdown timer
    var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = Clock.System.now().toEpochMilliseconds()
        }
    }

    // Determine display state
    val entryStatus = entry?.status ?: tournament.userEntryStatus
    val timeToStart = tournament.startsAt - now
    val timeLeft = tournament.endsAt - now

    val displayState = when {
        tournament.status == TournamentStatus.SCHEDULED && timeToStart > 0 -> TournamentDisplayState.UPCOMING
        tournament.status == TournamentStatus.LIVE && isAnonymous -> TournamentDisplayState.ANONYMOUS
        tournament.status == TournamentStatus.LIVE && entryStatus == TournamentEntryStatus.IN_PROGRESS -> TournamentDisplayState.RESUME
        (tournament.status == TournamentStatus.LIVE || tournament.status == TournamentStatus.CLOSED || tournament.status == TournamentStatus.FINALIZED)
            && (entryStatus == TournamentEntryStatus.COMPLETED || entryStatus == TournamentEntryStatus.AUTO_SUBMITTED) -> TournamentDisplayState.COMPLETED
        tournament.status == TournamentStatus.LIVE -> TournamentDisplayState.PLAY
        // Closed/Finalized tournament but user never played → show "coming soon" awareness banner
        tournament.status == TournamentStatus.CLOSED || tournament.status == TournamentStatus.FINALIZED -> TournamentDisplayState.ENDED
        else -> TournamentDisplayState.HIDDEN
    }

    if (displayState == TournamentDisplayState.HIDDEN) return

    // State-specific values
    val badgeText: String
    val badgeColor: Color
    val ctaText: String
    val ctaColor: Color
    val ctaEnabled: Boolean
    val subtitle: String

    when (displayState) {
        TournamentDisplayState.UPCOMING -> {
            badgeText = "UPCOMING"
            badgeColor = UpcomingYellow
            ctaText = "Starting Soon"
            ctaColor = DisabledGray
            ctaEnabled = false
            subtitle = "Starts in ${formatCountdownMs(timeToStart)}"
        }
        TournamentDisplayState.PLAY -> {
            badgeText = "LIVE"
            badgeColor = LiveGreen
            ctaText = "Play Now \u2192"
            ctaColor = PurpleCta
            ctaEnabled = true
            subtitle = "Ends in ${formatCountdownMs(timeLeft)} \u00B7 ${tournament.participantCount} playing"
        }
        TournamentDisplayState.ANONYMOUS -> {
            badgeText = "LIVE"
            badgeColor = LiveGreen
            ctaText = "Link Account to Play"
            ctaColor = UpcomingYellow
            ctaEnabled = true
            subtitle = "Ends in ${formatCountdownMs(timeLeft)} \u00B7 ${tournament.participantCount} playing"
        }
        TournamentDisplayState.RESUME -> {
            badgeText = "IN PROGRESS"
            badgeColor = InProgressYellow
            ctaText = "Resume \u2192"
            ctaColor = InProgressYellow
            ctaEnabled = true
            val answered = entry?.questionsAnswered ?: 0
            subtitle = "$answered/${tournament.questionCount} answered \u00B7 Timer paused"
        }
        TournamentDisplayState.COMPLETED -> {
            badgeText = "COMPLETED"
            badgeColor = CompletedIndigo
            ctaText = "View Results"
            ctaColor = CompletedIndigo
            ctaEnabled = true
            val score = entry?.score ?: 0
            val rank = entry?.rank ?: 0
            subtitle = "Score: $score/${tournament.questionCount} \u00B7 Rank #$rank"
        }
        TournamentDisplayState.ENDED -> {
            badgeText = "ENDED"
            badgeColor = DisabledGray
            ctaText = "Coming Soon"
            ctaColor = DisabledGray
            ctaEnabled = false
            subtitle = "${tournament.participantCount} participated \u00B7 Next tournament soon!"
        }
        TournamentDisplayState.HIDDEN -> return
    }

    val onCtaClick: () -> Unit = {
        when (displayState) {
            TournamentDisplayState.PLAY -> onPlay()
            TournamentDisplayState.ANONYMOUS -> onLinkAccount()
            TournamentDisplayState.RESUME -> onResume()
            TournamentDisplayState.COMPLETED -> onViewResults()
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BannerDark1, BannerDark2, BannerDark3),
                ),
            )
            .then(
                if (ctaEnabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCtaClick,
                ) else Modifier,
            ),
    ) {
        // Glow blob
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(PurpleCta.copy(alpha = 0.2f)),
        )

        // Stars
        val starPositions = remember {
            (0 until 5).map { i ->
                val x = (15f + i * 20f)
                val y = (10f + (i % 3) * 25f)
                x to y
            }
        }
        starPositions.forEachIndexed { i, (x, y) ->
            Box(
                modifier = Modifier
                    .offset(x = (x * 2.5f).dp, y = (y * 0.8f).dp)
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
                    .graphicsLayer { alpha = pulseAlpha },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (badgeText == "LIVE") {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .graphicsLayer { alpha = pulseAlpha },
                        )
                    }
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        letterSpacing = 1.2.sp,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "\u2694\uFE0F ${tournament.title}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CTA button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (ctaEnabled) Brush.linearGradient(
                                listOf(ctaColor, ctaColor.copy(alpha = 0.85f)),
                            )
                            else Brush.linearGradient(
                                listOf(Color(0xFF4B5563), Color(0xFF4B5563)),
                            ),
                        )
                        .then(
                            if (ctaEnabled) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCtaClick,
                            ) else Modifier,
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .graphicsLayer { alpha = if (ctaEnabled) 1f else 0.6f },
                ) {
                    Text(
                        text = ctaText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Trophy / Sword emoji
            Text(
                text = when (displayState) {
                    TournamentDisplayState.COMPLETED -> "\uD83C\uDFC5"
                    TournamentDisplayState.ENDED -> "\u23F3"
                    else -> "\u2694\uFE0F"
                },
                fontSize = 44.sp,
                modifier = Modifier
                    .offset(y = trophyBob.dp)
                    .graphicsLayer {
                        shadowElevation = 10f
                    },
            )
        }
    }
}

private fun formatCountdownMs(ms: Long): String {
    if (ms <= 0) return "Ended"
    val totalMinutes = ms / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}
