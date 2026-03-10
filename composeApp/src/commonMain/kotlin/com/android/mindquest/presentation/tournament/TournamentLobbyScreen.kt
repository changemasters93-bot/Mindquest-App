package com.android.mindquest.presentation.tournament

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
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
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.model.TournamentEntryStatus
import com.android.mindquest.domain.model.TournamentStatus
import com.android.mindquest.presentation.components.LoadingView
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.components.SecondaryButton
import kotlinx.coroutines.delay
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════
// LOBBY MODES — determines which CTA the lobby shows
// ═══════════════════════════════════════════════════════════════════
private enum class LobbyMode { PLAY, RESUME, UPCOMING }

/**
 * Tournament lobby / detail screen.
 *
 * Handles ALL tournament states:
 * - **PLAY** (LIVE + NOT_STARTED) → "Start Tournament" button
 * - **RESUME** (LIVE + IN_PROGRESS) → "Resume Tournament" button
 * - **UPCOMING** (SCHEDULED) → countdown + "Coming Soon" disabled button
 * - **COMPLETED / AUTO_SUBMITTED** → redirects to result screen
 * - **CLOSED / FINALIZED + no entry** → "Tournament Ended" screen
 *
 * On start/resume, calls [viewModel.startTournament] or
 * [viewModel.resumeTournamentEntry], waits for the entry + quiz,
 * then invokes [onStartQuiz] so the caller can store them in
 * [QuizSessionHolder] and navigate to [QuizPlayScreen].
 */
@Composable
fun TournamentLobbyScreen(
    viewModel: TournamentViewModel,
    onStartQuiz: (Tournament, TournamentEntry, Quiz) -> Unit = { _, _, _ -> },
    onViewResults: () -> Unit = {},
    onBack: () -> Unit,
    userId: String = "current_user",
) {
    LaunchedEffect(userId) {
        viewModel.loadTournament(userId)
    }

    val tournamentState by viewModel.tournamentState.collectAsState()
    val entryState by viewModel.entryState.collectAsState()
    val tournamentQuiz by viewModel.tournamentQuiz.collectAsState()

    // Once startTournament / resumeTournamentEntry succeeds and quiz is
    // available, navigate to QuizPlayScreen.
    LaunchedEffect(entryState, tournamentQuiz) {
        val entry = (entryState as? UiState.Success)?.data ?: return@LaunchedEffect
        val quiz = tournamentQuiz ?: return@LaunchedEffect
        val tournament = (tournamentState as? UiState.Success)?.data ?: return@LaunchedEffect
        onStartQuiz(tournament, entry, quiz)
    }

    when (val state = tournamentState) {
        is UiState.Loading -> LoadingView(message = "Loading tournament...")
        is UiState.Error -> ErrorView(message = state.message, onRetry = onBack)
        is UiState.Empty -> ErrorView(message = "No active tournament", onRetry = onBack)
        is UiState.Offline -> ErrorView(message = "You're offline", onRetry = onBack)
        is UiState.Success -> {
            val tournament = state.data
            val isStarting = entryState is UiState.Loading
            val userEntryStatus = tournament.userEntryStatus

            when {
                // ── COMPLETED / AUTO_SUBMITTED → redirect to results ──
                userEntryStatus == TournamentEntryStatus.COMPLETED ||
                    userEntryStatus == TournamentEntryStatus.AUTO_SUBMITTED -> {
                    LaunchedEffect(Unit) {
                        viewModel.loadTournamentResult(userId)
                        onViewResults()
                    }
                    LoadingView(message = "Loading results...")
                }

                // ── CLOSED / FINALIZED with no entry → tournament ended ──
                (tournament.status == TournamentStatus.CLOSED ||
                    tournament.status == TournamentStatus.FINALIZED) &&
                    userEntryStatus == null -> {
                    TournamentEndedContent(
                        tournament = tournament,
                        onBack = onBack,
                    )
                }

                // ── IN_PROGRESS → resume flow ──
                userEntryStatus == TournamentEntryStatus.IN_PROGRESS -> {
                    TournamentLobbyContent(
                        tournament = tournament,
                        lobbyMode = LobbyMode.RESUME,
                        isStarting = isStarting,
                        onStart = {
                            viewModel.resumeTournamentEntry(userId, tournament.id)
                        },
                        onBack = onBack,
                    )
                }

                // ── SCHEDULED → upcoming, no start ──
                tournament.status == TournamentStatus.SCHEDULED -> {
                    TournamentLobbyContent(
                        tournament = tournament,
                        lobbyMode = LobbyMode.UPCOMING,
                        isStarting = false,
                        onStart = {}, // disabled
                        onBack = onBack,
                    )
                }

                // ── LIVE + NOT_STARTED → default play flow ──
                else -> {
                    TournamentLobbyContent(
                        tournament = tournament,
                        lobbyMode = LobbyMode.PLAY,
                        isStarting = isStarting,
                        onStart = {
                            viewModel.startTournament(userId, tournament.id)
                        },
                        onBack = onBack,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LOBBY CONTENT — adapts badge + CTA based on LobbyMode
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TournamentLobbyContent(
    tournament: Tournament,
    lobbyMode: LobbyMode = LobbyMode.PLAY,
    isStarting: Boolean = false,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lobby")

    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophy_scale",
    )

    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "star_alpha",
    )

    // Countdown timer (for UPCOMING mode)
    var countdownText by remember { mutableStateOf("") }
    LaunchedEffect(tournament.startsAt) {
        while (true) {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val diff = tournament.startsAt - now
            countdownText = if (diff <= 0) {
                ""
            } else {
                val hours = diff / 3600000
                val minutes = (diff % 3600000) / 60000
                val seconds = (diff % 60000) / 1000
                "${hours}h ${minutes}m ${seconds}s"
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF7C3AED),
                        Color(0xFF4F46E5),
                    ),
                ),
            ),
    ) {
        // Decorative stars
        val stars = remember {
            List(20) {
                Triple(
                    Random.nextFloat(),
                    Random.nextFloat(),
                    Random.nextFloat() * 2f + 0.5f,
                )
            }
        }
        stars.forEach { (xF, yF, size) ->
            Box(
                modifier = Modifier
                    .offset(x = (xF * 380).dp, y = (yF * 800).dp)
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = starAlpha * Random.nextFloat())),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Trophy
            Text(
                text = "\uD83C\uDFC6",
                fontSize = (64 * trophyScale).sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Status badge ─────────────────────────────────────
            when (lobbyMode) {
                LobbyMode.PLAY -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MindquestColors.Success)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "LIVE NOW",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                }
                LobbyMode.RESUME -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "IN PROGRESS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                }
                LobbyMode.UPCOMING -> {
                    if (countdownText.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "STARTS IN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 2.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = countdownText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tournament title
            Text(
                text = tournament.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoCard(
                    emoji = "\uD83D\uDC65",
                    value = "${tournament.participantCount}",
                    label = "Players",
                    modifier = Modifier.weight(1f),
                )
                InfoCard(
                    emoji = "\u2753",
                    value = "${tournament.questionCount}",
                    label = "Questions",
                    modifier = Modifier.weight(1f),
                )
                InfoCard(
                    emoji = "\u23F1\uFE0F",
                    value = "${tournament.timeLimitSeconds / 60}m",
                    label = "Time",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rewards section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text = "\uD83C\uDFC5 Rewards",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        RewardItem(medal = "\uD83E\uDD47", label = "1st", reward = "100 XP")
                        RewardItem(medal = "\uD83E\uDD48", label = "2nd", reward = "75 XP")
                        RewardItem(medal = "\uD83E\uDD49", label = "3rd", reward = "50 XP")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rules card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text = "\uD83D\uDCCB Rules",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RuleItem(text = "Answer all questions within the time limit")
                    RuleItem(text = "Faster answers earn bonus points")
                    RuleItem(text = "No going back to previous questions")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── CTA button ───────────────────────────────────────
            when (lobbyMode) {
                LobbyMode.PLAY -> {
                    PrimaryButton(
                        text = if (isStarting) "Starting..." else "Start Tournament",
                        onClick = onStart,
                        enabled = !isStarting,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                }
                LobbyMode.RESUME -> {
                    PrimaryButton(
                        text = if (isStarting) "Resuming..." else "Resume Tournament",
                        onClick = onStart,
                        enabled = !isStarting,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                }
                LobbyMode.UPCOMING -> {
                    PrimaryButton(
                        text = "Coming Soon",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryButton(
                text = "Go Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TOURNAMENT ENDED — shown for CLOSED / FINALIZED with no entry
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TournamentEndedContent(
    tournament: Tournament,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF7C3AED),
                        Color(0xFF4F46E5),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "\uD83C\uDFC1", fontSize = 64.sp) // Checkered flag

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tournament Ended",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tournament.title,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${tournament.participantCount} participants competed",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            SecondaryButton(
                text = "Go Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun InfoCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(14.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun RewardItem(
    medal: String,
    label: String,
    reward: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = medal, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = reward,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun RuleItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "\u2022", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
