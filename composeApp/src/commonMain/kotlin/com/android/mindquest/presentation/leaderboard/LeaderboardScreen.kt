package com.android.mindquest.presentation.leaderboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardEntry
import com.android.mindquest.domain.model.UserRank
import com.android.mindquest.presentation.components.AvatarView
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LoadingView
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.CircularProgressIndicator
import kotlin.math.absoluteValue
import kotlin.random.Random

// ── Colors ───────────────────────────────────────────────────────────────
private val PodiumBgTop = Color(0xFF08081E)
private val PodiumBgCenter = Color(0xFF131138)
private val PodiumBgBottom = Color(0xFF1E1060)
private val FilterBg = Color(0xFFECEEF5)
private val FilterActiveBg = Color.White
private val FilterActiveText = Color(0xFF4F46E5)
private val FilterInactiveText = Color(0xFF6B7280)
private val GoldRing = Color(0xFFFFB800)
private val SilverRing = Color(0xFF9BA8B8)
private val BronzeRing = Color(0xFFC0784A)
private val GoldGradStart = Color(0xFFFFB800)
private val GoldGradEnd = Color(0xFFFFD95A)
private val SilverGradStart = Color(0xFF9BA8B8)
private val SilverGradEnd = Color(0xFFC8D3E0)
private val BronzeGradStart = Color(0xFFC0784A)
private val BronzeGradEnd = Color(0xFFD4A070)
private val YourRankBg = Color(0xFF1C1A3A)
private val UserRowTint = Color(0xFF4F46E5)
private val XpChipBg = Color(0xFFFFFBEB)
private val XpChipBorder = Color(0xFFFDE68A)
private val XpChipText = Color(0xFF92400E)

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    userId: String = "current_user",
) {
    val leaderboardState by viewModel.leaderboardState.collectAsState()
    val activeFilterIndex by viewModel.activeFilterIndex.collectAsState()

    LaunchedEffect(userId, activeFilterIndex) {
        viewModel.loadLeaderboard(userId, viewModel.activeFilter)
    }

    // Extract user info from leaderboard data for header
    val currentUserEntry = (leaderboardState as? UiState.Success)?.data
        ?.rankedUsers?.find { it.userId == userId }
    val userRank = (leaderboardState as? UiState.Success)?.data?.userRank

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
    ) {
        // ── Header (populated from leaderboard data) ──────────────────
        LeaderboardHeader(
            userName = currentUserEntry?.displayName ?: "",
            userAvatarId = currentUserEntry?.avatarId ?: 1,
            userXp = currentUserEntry?.totalXp?.toInt() ?: 0,
            userRank = userRank?.rankGlobal ?: 0,
        )

        // ── Filter Tabs ──────────────────────────────────────────────────
        FilterTabs(
            activeIndex = activeFilterIndex,
            onSelect = { viewModel.setFilter(it) },
        )

        // ── Content ──────────────────────────────────────────────────────
        when (val state = leaderboardState) {
            is UiState.Loading -> LoadingView(message = "Loading leaderboard...")
            is UiState.Error -> ErrorView(
                message = state.message,
                onRetry = { viewModel.loadLeaderboard(userId, viewModel.activeFilter) },
            )
            is UiState.Empty -> EmptyLeaderboard()
            is UiState.Offline -> ErrorView(
                message = "You're offline. Check your connection.",
                onRetry = { viewModel.loadLeaderboard(userId, viewModel.activeFilter) },
            )
            is UiState.Success -> {
                LeaderboardContent(
                    data = state.data,
                    currentUserId = userId,
                    viewModel = viewModel,
                    userId = userId,
                )
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardHeader(
    userName: String,
    userAvatarId: Int,
    userXp: Int,
    userRank: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MindquestColors.Surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarView(avatarId = userAvatarId, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName.ifEmpty { "Student" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
            )
            if (userRank > 0) {
                Text(
                    text = "Rank #$userRank",
                    fontSize = 12.sp,
                    color = MindquestColors.TextTertiary,
                )
            }
        }
        // XP badge
        if (userXp > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "\u26A1 $userXp XP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E),
                )
            }
        }
    }
}

// ── Filter Tabs ──────────────────────────────────────────────────────────

@Composable
private fun FilterTabs(
    activeIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val labels = LeaderboardViewModel.FILTER_LABELS
    val emojis = LeaderboardViewModel.FILTER_EMOJIS

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FilterBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            labels.forEachIndexed { index, label ->
                val isActive = index == activeIndex

                // Instant color switch — animated transitions caused a visible
                // "ripple" on the deselecting tab when switching filters.
                val bgColor = if (isActive) FilterActiveBg else Color.Transparent
                val textColor = if (isActive) FilterActiveText else FilterInactiveText

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isActive) Modifier.shadow(4.dp, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = emojis[index], fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

// ── Leaderboard Content (scrollable) ─────────────────────────────────────

@Composable
private fun LeaderboardContent(
    data: LeaderboardData,
    currentUserId: String,
    viewModel: LeaderboardViewModel,
    userId: String,
) {
    val topThree = data.rankedUsers.take(3)
    val allEntries = data.rankedUsers
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Podium
        item {
            PodiumSection(topThree = topThree)
        }

        // Your Rank card
        item {
            YourRankCard(
                userRank = data.userRank,
                currentUser = data.rankedUsers.find { it.userId == currentUserId },
            )
        }

        // All Rankings header
        item {
            Text(
                text = "ALL RANKINGS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
            )
        }

        // Rank rows
        itemsIndexed(allEntries) { index, entry ->
            RankRow(
                entry = entry,
                isCurrentUser = entry.userId == currentUserId,
            )

            // Trigger pagination when near the bottom
            if (index >= allEntries.size - 5 && viewModel.hasMorePages && !isLoadingMore) {
                LaunchedEffect(allEntries.size) {
                    viewModel.loadMoreLeaderboard(userId)
                }
            }
        }

        // Loading indicator for pagination
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF4F46E5),
                    )
                }
            }
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// ── Sparkle particles (golden twinkle particles on podium) ─────────────

@Composable
private fun SparkleParticles(infiniteTransition: androidx.compose.animation.core.InfiniteTransition) {
    val sparklePositions = remember {
        listOf(
            Triple(10f, 18f, 0), Triple(45f, 8f, 1), Triple(75f, 25f, 2),
            Triple(80f, 10f, 0), Triple(25f, 20f, 1), Triple(62f, 5f, 2),
        )
    }

    sparklePositions.forEachIndexed { i, (xPct, yPct, variant) ->
        val sparkleAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1800 + i * 300,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sparkle_$i",
        )
        val sparkleSize = 6f + (variant % 3) * 3f
        Box(
            modifier = Modifier
                .offset(x = (xPct * 3.5f).dp, y = (yPct * 1.5f).dp)
                .size(sparkleSize.dp)
                .clip(CircleShape)
                .background(
                    if (i % 2 == 0) Color(0xFFFFDC3C).copy(alpha = sparkleAlpha * 0.8f)
                    else Color.White.copy(alpha = sparkleAlpha * 0.6f),
                ),
        )
    }
}

// ── Podium Section ───────────────────────────────────────────────────────

@Composable
private fun PodiumSection(topThree: List<LeaderboardEntry>) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_twinkle")

    // Generate star positions
    val stars = remember {
        List(28) { i ->
            Triple(
                ((i * 37 + 5) % 95) / 100f,
                ((i * 53 + 3) % 70) / 100f,
                1f + (i % 3) * 0.8f,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(PodiumBgTop, PodiumBgCenter, PodiumBgBottom),
                ),
            ),
    ) {
        // Star field
        stars.forEach { (xFrac, yFrac, size) ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (1200 + (xFrac * 1800).toInt()),
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "star_${xFrac}_$yFrac",
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = (xFrac * 340).dp,
                        y = (yFrac * 260).dp,
                    )
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha)),
            )
        }

        // Ambient glow behind 1st place (center-bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-60).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFFB400).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Sparkle particles
        SparkleParticles(infiniteTransition)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            // Title row — "🏆 Top Performers" on left, "This Week" badge on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83C\uDFC6 TOP PERFORMERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "This Week",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 podium players: order is 2nd | 1st | 3rd
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                // 2nd place
                if (topThree.size > 1) {
                    PodiumPlayer(
                        entry = topThree[1],
                        rank = 2,
                        avatarSize = 58,
                        ringColor = SilverRing,
                        ringGradEnd = SilverGradEnd,
                        podiumHeight = 78,
                        podiumColor = SilverGradStart,
                        podiumGradEnd = SilverGradEnd,
                        rankTextColor = Color(0xFF5A6A80),
                        labelColor = Color(0xFFB0BCCC),
                        modifier = Modifier.weight(1f),
                    )
                }

                // 1st place
                if (topThree.isNotEmpty()) {
                    PodiumPlayer(
                        entry = topThree[0],
                        rank = 1,
                        avatarSize = 76,
                        ringColor = GoldRing,
                        ringGradEnd = GoldGradEnd,
                        podiumHeight = 110,
                        podiumColor = Color(0xFFF5A623),
                        podiumGradEnd = Color(0xFFFFE566),
                        rankTextColor = Color(0xFF7A4500),
                        labelColor = Color(0xFFFFD060),
                        showCrown = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 3rd place
                if (topThree.size > 2) {
                    PodiumPlayer(
                        entry = topThree[2],
                        rank = 3,
                        avatarSize = 56,
                        ringColor = BronzeRing,
                        ringGradEnd = BronzeGradEnd,
                        podiumHeight = 56,
                        podiumColor = Color(0xFFA06838),
                        podiumGradEnd = Color(0xFFD4A070),
                        rankTextColor = Color(0xFF7A4828),
                        labelColor = Color(0xFFC8986A),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumPlayer(
    entry: LeaderboardEntry,
    rank: Int,
    avatarSize: Int,
    ringColor: Color,
    ringGradEnd: Color = ringColor,
    podiumHeight: Int,
    podiumColor: Color,
    podiumGradEnd: Color = podiumColor,
    rankTextColor: Color = Color.White.copy(alpha = 0.8f),
    labelColor: Color = Color.White.copy(alpha = 0.6f),
    showCrown: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crown_bounce")
    val crownOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "crown_y",
    )

    val isFirst = rank == 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // Crown for 1st place with float animation
        if (showCrown) {
            Text(
                text = "\uD83D\uDC51",
                fontSize = 26.sp,
                modifier = Modifier.offset(y = crownOffset.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Spacer(modifier = Modifier.height(38.dp))
        }

        // Avatar with medal ring
        Box(contentAlignment = Alignment.Center) {
            // Ambient glow behind avatar for 1st place
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .size((avatarSize + 32).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ringColor.copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }

            // Outer ring gradient
            Box(
                modifier = Modifier
                    .size((avatarSize + 10).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ringGradEnd, ringColor),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Dark inner gap
                Box(
                    modifier = Modifier
                        .size((avatarSize + 4).dp)
                        .clip(CircleShape)
                        .background(PodiumBgTop),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarView(
                        avatarId = entry.avatarId,
                        size = avatarSize.dp,
                    )
                }
            }

            // Rank medal badge (bottom-right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ringGradEnd, ringColor),
                        ),
                    )
                    .border(2.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$rank",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = rankTextColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Name
        Text(
            text = entry.displayName,
            fontSize = if (isFirst) 16.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.3).sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // XP chip (dark themed on podium)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, labelColor.copy(alpha = 0.27f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "\u26A1 ${entry.totalXp} XP",
                fontSize = if (isFirst) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Podium bar with gradient + shine stripe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(podiumHeight.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(podiumGradEnd, podiumColor),
                    ),
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Shine stripe overlay
            Box(
                modifier = Modifier
                    .offset(x = (-10).dp)
                    .width(20.dp)
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.12f)),
            )

            Text(
                text = "$rank",
                fontSize = if (isFirst) 26.sp else 20.sp,
                fontWeight = FontWeight.Black,
                color = rankTextColor.copy(alpha = 0.8f),
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

// ── Your Rank Card ───────────────────────────────────────────────────────

@Composable
private fun YourRankCard(
    userRank: UserRank,
    currentUser: LeaderboardEntry?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(YourRankBg)
            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        // Subtle bg orb (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-40).dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF6366F1).copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Avatar + YOU badge
            if (currentUser != null) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFA78BFA).copy(alpha = 0.25f), CircleShape),
                    ) {
                        AvatarView(avatarId = currentUser.avatarId, size = 46.dp)
                    }
                    // YOU badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4338CA))
                            .border(2.dp, YourRankBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "YOU",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.3.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = (-0.2).sp,
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // XP progress bar to next rank
                    val xpGap = userRank.xpGapToNext ?: 0L
                    val nextRank = (userRank.rankCountry ?: userRank.rankGlobal) - 1
                    if (xpGap > 0 && nextRank > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                            ) {
                                val progress = (1f - (xpGap.toFloat() / (xpGap + 100f)))
                                    .coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFF818CF8),
                                                ),
                                            ),
                                        ),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$xpGap XP to #$nextRank",
                                fontSize = 10.sp,
                                color = Color(0xFFC7D2FE).copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Your ranking will appear here",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Rank box — styled like JSX
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "#${userRank.rankGlobal}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF818CF8),
                        letterSpacing = (-1).sp,
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "RANK",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF818CF8).copy(alpha = 0.4f),
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

// ── Rank Row ─────────────────────────────────────────────────────────────

@Composable
private fun RankRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
) {
    val bgColor = if (isCurrentUser) {
        Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF)))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }
    val borderColor = if (isCurrentUser) Color(0xFFC7D2FE) else Color(0xFFF3F4F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank number or medal
        val rankDisplay = when (entry.rank) {
            1 -> "\uD83E\uDD47"
            2 -> "\uD83E\uDD48"
            3 -> "\uD83E\uDD49"
            else -> "${entry.rank}"
        }
        val rankColor = when (entry.rank) {
            1 -> Color(0xFFF5A623)
            2 -> Color(0xFF9CA3AF)
            3 -> Color(0xFFC09060)
            else -> Color(0xFF9CA3AF)
        }
        Text(
            text = rankDisplay,
            fontSize = if (entry.rank <= 3) 20.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = rankColor,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.width(10.dp))

        AvatarView(avatarId = entry.avatarId, size = 34.dp)

        Spacer(modifier = Modifier.width(10.dp))

        // Name + YOU badge
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) Color(0xFF3730A3) else Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF4F46E5))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "YOU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Trend arrow
        val trendValue = entry.trend.toIntOrNull() ?: 0
        val trendFlat = trendValue == 0
        if (!trendFlat) {
            val trendUp = trendValue > 0
            val trendText = if (trendUp) "\u25B2+$trendValue" else "\u25BC${trendValue}"
            val trendColor = if (trendUp) Color(0xFF10B981) else Color(0xFFEF4444)
            Text(
                text = trendText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = trendColor,
                modifier = Modifier.padding(end = 6.dp),
            )
        } else {
            Text(
                text = "\u2014",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(end = 6.dp),
            )
        }

        // XP pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCurrentUser) Color(0xFF4F46E5).copy(alpha = 0.1f)
                    else Color(0xFFF3F4F6),
                )
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = "${entry.totalXp}",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrentUser) Color(0xFF4F46E5) else Color(0xFF111827),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────────────

@Composable
private fun EmptyLeaderboard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "\uD83C\uDFC6", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No rankings yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MindquestColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete quizzes to earn XP and climb the leaderboard!",
            fontSize = 14.sp,
            color = MindquestColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}
