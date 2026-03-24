package com.android.mindquest.presentation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.model.TournamentResult
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.model.UserStats
import com.android.mindquest.presentation.components.AvatarView
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LoadingView
import com.android.mindquest.presentation.components.CertificateFullViewDialog
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.components.SecondaryButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.android.mindquest.presentation.components.XpPill
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

// ── Badge definitions ───────────────────────────────────────────────────────────

private data class Badge(
    val emoji: String,
    val title: String,
    val earned: Boolean,
)

private fun getBadges(stats: UserStats): List<Badge> = listOf(
    Badge("\uD83C\uDF1F", "First Quiz", stats.quizzesCompleted >= 1),
    Badge("\uD83D\uDD25", "3-Day Streak", stats.streakCurrent >= 3),
    Badge("\u26A1", "100 XP", stats.totalXp >= 100),
    Badge("\uD83C\uDFC6", "Tournament", stats.tournamentsPlayed > 0),
    Badge("\uD83D\uDE80", "10 Quizzes", stats.quizzesCompleted >= 10),
    Badge("\uD83D\uDCAA", "7-Day Streak", stats.streakCurrent >= 7),
    Badge("\uD83E\uDDE0", "IQ Master", (stats.iqScore ?: 0) >= 120),
    Badge("\uD83D\uDC51", "50 Quizzes", stats.quizzesCompleted >= 50),
    Badge("\uD83C\uDF08", "500 XP", stats.totalXp >= 500),
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignOut: () -> Unit,
    onLinkGoogle: (() -> Unit)? = null,
    userId: String = "current_user",
) {
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val profileState by viewModel.profileState.collectAsState()

    when (val state = profileState) {
        is UiState.Loading -> LoadingView()
        is UiState.Success -> ProfileContent(data = state.data, onSignOut = onSignOut, onLinkGoogle = onLinkGoogle)
        is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.loadProfile(userId) })
        is UiState.Empty -> ErrorView(message = "Profile data not available.", onRetry = { viewModel.loadProfile(userId) })
        is UiState.Offline -> ErrorView(message = "You are offline. Please check your connection.", onRetry = { viewModel.loadProfile(userId) })
    }
}

@Composable
private fun ProfileContent(
    data: ProfileData,
    onSignOut: () -> Unit,
    onLinkGoogle: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    var selectedCertificate by remember { mutableStateOf<TournamentResult?>(null) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Profile header with gradient
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 3 },
            ) {
                ProfileHeader(user = data.user, stats = data.stats)
            }
        }

        // Stats summary (4-column)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 100)) +
                    slideInVertically(tween(500, delayMillis = 100)) { it / 4 },
            ) {
                StatsSummaryRow(stats = data.stats)
            }
        }

        // Badges grid
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 200)) +
                    slideInVertically(tween(400, delayMillis = 200)) { it / 4 },
            ) {
                BadgesSection(stats = data.stats)
            }
        }

        // Tournament Trophies (horizontal scroll cards for top 3 finishes)
        item {
            val topFinishes = data.tournamentResults.filter { it.rank <= 3 }
            if (topFinishes.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = 300)) +
                        slideInVertically(tween(400, delayMillis = 300)) { it / 4 },
                ) {
                    TournamentTrophiesSection(trophies = topFinishes)
                }
            }
        }

        // Tournament Certificates (always shown — has its own empty state)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 350)) +
                    slideInVertically(tween(400, delayMillis = 350)) { it / 4 },
            ) {
                TournamentCertificatesSection(
                    results = data.tournamentResults,
                    onViewCertificate = { selectedCertificate = it },
                )
            }
        }

        // Tournament history section (full list)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 400)) +
                    slideInVertically(tween(400, delayMillis = 400)) { it / 4 },
            ) {
                SectionHeader(title = "Tournament History", emoji = "\uD83C\uDFC6")
            }
        }

        if (data.tournamentResults.isEmpty()) {
            item {
                EmptySection(message = "No tournaments played yet. Join one!")
            }
        } else {
            itemsIndexed(data.tournamentResults.take(10)) { index, result ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = 450 + index * 60)) +
                        slideInVertically(tween(400, delayMillis = 450 + index * 60)) { it / 3 },
                ) {
                    TournamentResultRow(
                        result = result,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        // Settings section
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 550)) +
                    slideInVertically(tween(400, delayMillis = 550)) { it / 4 },
            ) {
                SettingsSection(
                    user = data.user,
                    onSignOut = onSignOut,
                    isAnonymous = data.user.authProvider == "anonymous",
                    onLinkGoogle = onLinkGoogle,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    // Certificate full-view dialog overlay
    selectedCertificate?.let { cert ->
        CertificateFullViewDialog(
            result = cert,
            userName = data.user.displayName,
            onDismiss = { selectedCertificate = null },
        )
    }
    } // end outer Box
}

// ── Profile Header ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(user: User, stats: UserStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF312E81),
                    ),
                ),
            ),
    ) {
        // Decorative orbs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 20.dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar with ring + glow
            Box(contentAlignment = Alignment.Center) {
                // Subtle glow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF059669).copy(alpha = 0.3f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarView(
                        avatarId = user.avatarId,
                        modifier = Modifier.size(72.dp),
                    )
                }
                // Online indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = user.displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Grade + Location on one line
            val locationParts = mutableListOf(user.gradeLabel)
            val location = listOfNotNull(user.cityName, user.countryName)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            if (location.isNotBlank()) locationParts.add(location)

            Text(
                text = locationParts.joinToString(" \u00B7 "),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )

            if (!user.schoolName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\uD83C\uDFEB ${user.schoolName}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // XP + Level stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCompact(stats.totalXp.toInt()),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "XP",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Lvl ${stats.level}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Level",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

// ── Stats Summary Row ───────────────────────────────────────────────────────────

@Composable
private fun StatsSummaryRow(stats: UserStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatPill(
            emoji = "\u26A1",
            value = formatCompact(stats.totalXp.toInt()),
            label = "XP",
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f),
        )
        StatPill(
            emoji = "\uD83D\uDD25",
            value = "${stats.streakCurrent}d",
            label = "Streak",
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f),
        )
        StatPill(
            emoji = "\uD83C\uDFC5",
            value = getBadges(stats).count { it.earned }.toString(),
            label = "Badges",
            color = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f),
        )
        StatPill(
            emoji = "\u2705",
            value = stats.quizzesCompleted.toString(),
            label = "Quizzes",
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatPill(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MindquestColors.TextTertiary,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

// ── Badges Grid ─────────────────────────────────────────────────────────────────

@Composable
private fun BadgesSection(stats: UserStats) {
    val badges = getBadges(stats)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83C\uDFC5", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Achievements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MindquestColors.Primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${badges.count { it.earned }}/${badges.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MindquestColors.Primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-column grid
        val rows = badges.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { badge ->
                    BadgeCard(
                        badge = badge,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill remaining space if not full row
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BadgeCard(
    badge: Badge,
    modifier: Modifier = Modifier,
) {
    if (!badge.earned) {
        // Unearned badge — dashed style with lock
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.emoji,
                    fontSize = 28.sp,
                    color = Color.Gray,
                    modifier = Modifier.then(Modifier),
                )
                // Lock icon at bottom-right
                Text(
                    text = "\uD83D\uDD12",
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        // Earned badge — golden glow
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFFFFBEB))
                .border(2.dp, Color(0xFFFDE68A), RoundedCornerShape(22.dp))
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.emoji,
                    fontSize = 32.sp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "\u2713 Earned",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
            )
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, emoji: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MindquestColors.TextPrimary,
        )
    }
}

@Composable
private fun EmptySection(message: String) {
    Text(
        text = message,
        fontSize = 14.sp,
        color = MindquestColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

// ── Tournament Result Row ───────────────────────────────────────────────────────

@Composable
private fun TournamentResultRow(
    result: TournamentResult,
    modifier: Modifier = Modifier,
) {
    val rankEmoji = when (result.rank) {
        1 -> "\uD83E\uDD47"
        2 -> "\uD83E\uDD48"
        3 -> "\uD83E\uDD49"
        else -> "\uD83C\uDFC5"
    }

    val rankColor = when (result.rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF9CA3AF)
        3 -> Color(0xFFCD7F32)
        else -> MindquestColors.TextPrimary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(rankColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = rankEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MindquestColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${result.score}/${result.totalQuestions} correct",
                    fontSize = 12.sp,
                    color = MindquestColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(rankColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "#${result.rank}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankColor,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "of ${result.participantCount}",
                    fontSize = 10.sp,
                    color = MindquestColors.TextTertiary,
                )
            }
        }
    }
}

// ── Settings Section ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    user: User,
    onSignOut: () -> Unit,
    isAnonymous: Boolean = false,
    onLinkGoogle: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        HorizontalDivider(
            color = MindquestColors.TextSecondary.copy(alpha = 0.15f),
            thickness = 1.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account Information Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83D\uDD10", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auth Provider
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MindquestColors.Surface.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    text = "Login Method",
                    fontSize = 12.sp,
                    color = MindquestColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (user.authProvider) {
                        "google" -> "🔵 Google Account"
                        "phone" -> "📱 Phone Number"
                        "google_and_phone" -> "🔵 Google + 📱 Phone"
                        else -> "👤 Anonymous"
                    },
                    fontSize = 14.sp,
                    color = MindquestColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Email
        if (user.email != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MindquestColors.Surface.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Email Address",
                        fontSize = 12.sp,
                        color = MindquestColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.email!!,
                        fontSize = 13.sp,
                        color = MindquestColors.TextPrimary,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Phone
        if (user.phone != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MindquestColors.Surface.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Phone Number",
                        fontSize = 12.sp,
                        color = MindquestColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.phone!!,
                        fontSize = 13.sp,
                        color = MindquestColors.TextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Location Info
        if (!user.countryName.isNullOrEmpty() || !user.cityName.isNullOrEmpty() || !user.schoolName.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MindquestColors.Surface.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Location & School",
                        fontSize = 12.sp,
                        color = MindquestColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!user.countryName.isNullOrEmpty()) {
                        Text(
                            text = "🌍 ${user.countryName}",
                            fontSize = 13.sp,
                            color = MindquestColors.TextPrimary,
                        )
                    }
                    if (!user.cityName.isNullOrEmpty()) {
                        Text(
                            text = "🏙️ ${user.cityName}",
                            fontSize = 13.sp,
                            color = MindquestColors.TextPrimary,
                        )
                    }
                    if (!user.schoolName.isNullOrEmpty()) {
                        Text(
                            text = "🏫 ${user.schoolName}",
                            fontSize = 13.sp,
                            color = MindquestColors.TextPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Settings Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\u2699\uFE0F", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Settings items with colored icon backgrounds
        SettingsItem(
            emoji = "\uD83D\uDD14",
            title = "Notifications",
            subtitle = "Daily reminders & challenges",
            iconBg = Color(0xFFEEF2FF),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SettingsItem(
            emoji = "\uD83D\uDD0A",
            title = "Sound Effects",
            subtitle = "Quiz sounds & music",
            iconBg = Color(0xFFF0FDF4),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SettingsItem(
            emoji = "\uD83C\uDF19",
            title = "Dark Mode",
            subtitle = "Coming soon",
            iconBg = Color(0xFFF5F3FF),
        )

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = "Edit Profile",
            onClick = { /* navigate to edit profile */ },
            modifier = Modifier.fillMaxWidth(),
        )

        // Show "Link Google Account" for anonymous users
        if (isAnonymous && onLinkGoogle != null) {
            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = "\uD83D\uDD17 Link Google Account",
                onClick = onLinkGoogle,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Save your progress, enter tournaments & climb the leaderboard",
                fontSize = 11.sp,
                color = MindquestColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SecondaryButton(
            text = "Sign Out",
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingsItem(
    emoji: String,
    title: String,
    subtitle: String,
    iconBg: Color = Color(0xFFF3F4F6),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MindquestColors.TextTertiary,
            )
        }

        // Chevron
        Text(
            text = "\u203A",
            fontSize = 20.sp,
            color = Color(0xFFD1D5DB),
        )
    }
}

// ── Tournament Trophies Section (horizontal scroll) ──────────────────────────

@Composable
private fun TournamentTrophiesSection(trophies: List<TournamentResult>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83C\uDFC6", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tournament Trophies",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${trophies.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9CA3AF),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            trophies.forEach { trophy ->
                val rankConfig = when (trophy.rank) {
                    1 -> Triple("\uD83C\uDFC6", "Winner", Color(0xFFF59E0B))
                    2 -> Triple("\uD83E\uDD48", "2nd Place", Color(0xFF9CA3AF))
                    3 -> Triple("\uD83E\uDD49", "3rd Place", Color(0xFFCD7F32))
                    else -> Triple("\uD83C\uDFC5", "Top ${trophy.rank}", MindquestColors.Primary)
                }
                val (emoji, label, color) = rankConfig

                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(color.copy(alpha = 0.07f))
                        .border(1.5.dp, color.copy(alpha = 0.19f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = emoji,
                        fontSize = if (trophy.rank == 1) 36.sp else 32.sp,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = label.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        letterSpacing = 0.3.sp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = trophy.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${trophy.score}/${trophy.totalQuestions}",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                    )

                    Text(
                        text = "${trophy.participantCount} participants",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF),
                    )
                }
            }
        }
    }
}

// ── Tournament Certificates Section ──────────────────────────────────────────

@Composable
private fun TournamentCertificatesSection(
    results: List<TournamentResult>,
    onViewCertificate: (TournamentResult) -> Unit = {},
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83D\uDCDC", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Certificates",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            if (results.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${results.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9CA3AF),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Empty state ─────────────────────────────────────────────
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFF3F4F6), RoundedCornerShape(18.dp))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83D\uDCDC", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Certificates Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Complete tournaments to earn achievement\nand participation certificates!",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
            }
            return
        }

        // ── Certificate cards ───────────────────────────────────────
        results.forEach { result ->
            val isWinner = result.rank <= 3
            val certColor = when (result.rank) {
                1 -> Color(0xFFF59E0B)
                2 -> Color(0xFF9CA3AF)
                3 -> Color(0xFFCD7F32)
                else -> Color(0xFF4F46E5)
            }
            val certEmoji = when (result.rank) {
                1 -> "\uD83C\uDFC6"
                2 -> "\uD83E\uDD48"
                3 -> "\uD83E\uDD49"
                else -> "\uD83D\uDCDC"
            }
            val certLabel = when (result.rank) {
                1 -> "Winner"
                2 -> "2nd Place"
                3 -> "3rd Place"
                else -> "Rank #${result.rank}"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.5.dp, certColor.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onViewCertificate(result) }
                    .padding(18.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        // Trophy/Medal icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(certColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = certEmoji, fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Badge label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(certColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = certLabel.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = certColor,
                                    letterSpacing = 0.8.sp,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = result.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Info row
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "\uD83D\uDCCA ${result.score}/${result.totalQuestions}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280),
                                )
                                Text(
                                    text = "\uD83D\uDC65 ${result.participantCount}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280),
                                )
                            }
                        }
                    }

                    // Certificate footer
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = certColor.copy(alpha = 0.12f),
                        thickness = 1.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Mindquest \u00B7 ${if (isWinner) "Achievement" else "Participation"} Certificate",
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF),
                        )
                        Text(
                            text = "View Full \u2192",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = certColor,
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────

private fun formatCompact(value: Int): String {
    return when {
        value >= 1_000_000 -> "${((value / 100_000) / 10.0)}M"
        value >= 1_000 -> "${((value / 100) / 10.0)}K"
        else -> value.toString()
    }
}
