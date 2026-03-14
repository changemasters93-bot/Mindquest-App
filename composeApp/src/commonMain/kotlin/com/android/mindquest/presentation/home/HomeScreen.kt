package com.android.mindquest.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.model.UserStats
import com.android.mindquest.presentation.components.AvatarView
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LinkAccountDialog
import com.android.mindquest.presentation.components.LoadingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mindquest.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChapters: (Module) -> Unit,
    onNavigateToDailyChallenge: (DailyChallenge) -> Unit,
    onNavigateToTournament: () -> Unit,
    onNavigateToTournamentResult: () -> Unit = {},
    onNavigateToIqTest: (quizId: String) -> Unit = {},
    onLinkAccount: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onPhoneSignIn: () -> Unit = {},
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val dailyChallenges by viewModel.dailyChallenges.collectAsState()

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showLinkAccountDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refreshAll()
                delay(800)
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
    ) {
        when (val state = dashboardState) {
            is UiState.Loading -> {
                LoadingView(modifier = Modifier.fillMaxSize())
            }

            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refreshAll() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is UiState.Empty -> {
                ErrorView(
                    message = stringResource(Res.string.error_no_data),
                    onRetry = { viewModel.refreshAll() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is UiState.Offline -> {
                ErrorView(
                    message = stringResource(Res.string.error_offline_appear),
                    onRetry = { viewModel.refreshAll() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is UiState.Success -> {
                val data = state.data

                // Gather daily-challenges list (empty if still loading/error)
                val challenges: List<DailyChallenge> = when (val cs = dailyChallenges) {
                    is UiState.Success -> cs.data
                    else -> emptyList()
                }

                val completedMissions = challenges.count { it.isDone }
                val totalMissions = challenges.size

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MindquestColors.Background),
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        // ── 1. Header: Avatar + Greeting + XP pill ───────────
                        item(key = "home_header") {
                            HomeHeader(
                                user = data.user,
                                stats = data.stats,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 12.dp,
                                ),
                            )
                        }

                        // ── 2. Hero Banner: Today's Mission card ─────────────
                        item(key = "hero_banner") {
                            HeroBanner(
                                dailyChallenges = challenges,
                                completedMissions = completedMissions,
                                totalMissions = totalMissions,
                                onStartChallenge = { challenge ->
                                    if (challenge != null) {
                                        onNavigateToDailyChallenge(challenge)
                                    } else {
                                        // Fallback: open first module
                                        data.modules.firstOrNull()
                                            ?.let { onNavigateToChapters(it) }
                                    }
                                },
                            )
                        }

                        // ── 3. Tournament Banner ─────────────────────────────
                        data.activeTournament?.let { tournament ->
                            item(key = "tournament_banner") {
                                TournamentHomeBanner(
                                    tournament = tournament,
                                    entry = data.activeTournamentEntry,
                                    isAnonymous = data.user.isAnonymous,
                                    onPlay = onNavigateToTournament,
                                    onResume = onNavigateToTournament,
                                    onViewResults = onNavigateToTournamentResult,
                                    onLinkAccount = { showLinkAccountDialog = true },
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp,
                                    ),
                                )
                            }
                        }

                        // ── 4. Daily Practice — Subject grid ─────────────────
                        if (data.modules.isNotEmpty()) {
                            item(key = "subjects_header") {
                                SectionHeaderWithSub(
                                    title = stringResource(Res.string.home_daily_practice),
                                    subtitle = stringResource(Res.string.home_daily_practice_subtitle),
                                    emoji = "\uD83D\uDCDA",
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 20.dp,
                                        bottom = 8.dp,
                                    ),
                                )
                            }

                            // 2-column grid using chunked rows
                            val rows = data.modules.chunked(2)
                            items(
                                count = rows.size,
                                key = { index -> "subject_row_$index" },
                            ) { index ->
                                val row = rows[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    row.forEach { module ->
                                        SubjectCard(
                                            module = module,
                                            onClick = { onNavigateToChapters(module) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    // Fill remaining space if odd number of items
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // ── 5. Brain Training — IQ Test ──────────────────────
                        item(key = "iq_test_card") {
                            SectionHeaderWithSub(
                                title = stringResource(Res.string.home_brain_training),
                                subtitle = stringResource(Res.string.home_brain_training_subtitle),
                                emoji = "\uD83E\uDDE0",
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = 8.dp,
                                ),
                            )
                            if (data.iqQuizId != null) {
                                IqTestCard(
                                    onStartIqTest = { onNavigateToIqTest(data.iqQuizId) },
                                    lastAttemptDateMillis = data.lastIqTestDateMillis,
                                    cooldownHours = data.iqCooldownHours,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Link Account Dialog (anonymous user tournament sign-in) ───
    if (showLinkAccountDialog) {
        LinkAccountDialog(
            onGoogle = {
                showLinkAccountDialog = false
                onGoogleSignIn()
            },
            onPhone = {
                showLinkAccountDialog = false
                onPhoneSignIn()
            },
            onDismiss = { showLinkAccountDialog = false },
        )
    }
    } // end outer Box
}

// ═══════════════════════════════════════════════════════════════════
// HOME HEADER — avatar, greeting, grade, XP pill
// ═══════════════════════════════════════════════════════════════════

private val HeaderXpBg1 = Color(0xFFFFFBEB)
private val HeaderXpBg2 = Color(0xFFFEF3C7)
private val HeaderXpBorder = Color(0xFFFDE68A)
private val HeaderAmberText = Color(0xFF92400E)
private val HeaderAmberSub = Color(0xFFB45309)
private val HeaderBoltBg1 = Color(0xFFFBBF24)
private val HeaderBoltBg2 = Color(0xFFF59E0B)

@Composable
private fun HomeHeader(
    user: User,
    stats: UserStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with online indicator
        AvatarView(
            avatarId = user.avatarId,
            size = 44.dp,
            showOnlineIndicator = true,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Greeting + grade
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.home_greeting, user.displayName),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
            )
            if (user.gradeLabel.isNotEmpty()) {
                Text(
                    text = user.gradeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MindquestColors.TextTertiary,
                )
            }
        }

        // XP pill
        XpHeaderPill(
            totalXp = stats.totalXp,
            level = stats.level,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// XP HEADER PILL — compact golden pill with bolt icon
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun XpHeaderPill(
    totalXp: Long,
    level: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, HeaderXpBorder, RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(listOf(HeaderXpBg1, HeaderXpBg2)),
            )
            .padding(start = 4.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bolt circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(HeaderBoltBg1, HeaderBoltBg2)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u26A1",
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column {
            Text(
                text = stringResource(Res.string.common_xp_format, totalXp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HeaderAmberText,
            )
            Text(
                text = stringResource(Res.string.common_level_format, level),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = HeaderAmberSub,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SECTION HEADER WITH SUBTITLE
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeaderWithSub(
    title: String,
    subtitle: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MindquestColors.TextTertiary,
            )
        }
    }
}
