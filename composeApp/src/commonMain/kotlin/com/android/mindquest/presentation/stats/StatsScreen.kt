package com.android.mindquest.presentation.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import com.android.mindquest.domain.model.DailyActivity
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.model.SubjectPerformance
import com.android.mindquest.domain.model.UserStats
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LoadingView
import com.android.mindquest.presentation.components.MindquestProgressBar
import kotlinx.coroutines.delay

// ── IQ Journey Levels ───────────────────────────────────────────────────────────

private data class IqLevel(
    val label: String,
    val emoji: String,
    val min: Int,
    val max: Int,
    val color: Color,
    val dark: Color,
)

private val IQ_LEVELS = listOf(
    IqLevel("Rising Star", "\uD83C\uDF31", 55, 84, Color(0xFF6EE7B7), Color(0xFF059669)),
    IqLevel("Sharp Thinker", "\u26A1", 85, 114, Color(0xFF818CF8), Color(0xFF4F46E5)),
    IqLevel("Brain Explorer", "\uD83D\uDD2D", 115, 124, Color(0xFF34D399), Color(0xFF059669)),
    IqLevel("Genius Zone", "\uD83D\uDE80", 125, 134, Color(0xFFFBBF24), Color(0xFFD97706)),
    IqLevel("Mastermind", "\uD83C\uDFC6", 135, 160, Color(0xFFF472B6), Color(0xFFDB2777)),
)

// ── Subject tag helpers ─────────────────────────────────────────────────────────

private data class SubjectTag(
    val label: String,
    val bg: Color,
    val textColor: Color,
    val border: Color,
)

private fun getSubjectTag(accuracyPct: Int): SubjectTag {
    return when {
        accuracyPct >= 80 -> SubjectTag(
            "\uD83D\uDCAA Strength",
            Color(0xFF10B981).copy(alpha = 0.12f),
            Color(0xFF10B981),
            Color(0xFF10B981).copy(alpha = 0.3f),
        )
        accuracyPct >= 70 -> SubjectTag(
            "\uD83D\uDC4D Good",
            Color(0xFF6366F1).copy(alpha = 0.12f),
            Color(0xFF818CF8),
            Color(0xFF6366F1).copy(alpha = 0.3f),
        )
        accuracyPct >= 60 -> SubjectTag(
            "\uD83D\uDCC8 Improving",
            Color(0xFFFBBF24).copy(alpha = 0.12f),
            Color(0xFFFBBF24),
            Color(0xFFFBBF24).copy(alpha = 0.3f),
        )
        else -> SubjectTag(
            "\uD83C\uDFAF Needs Work",
            Color(0xFFF87171).copy(alpha = 0.12f),
            Color(0xFFF87171),
            Color(0xFFF87171).copy(alpha = 0.3f),
        )
    }
}

// ── Subject accent colors (same as home SubjectCard) ────────────────────────────

private val subjectColors = mapOf(
    "math" to Color(0xFF6366F1),
    "mathematics" to Color(0xFF6366F1),
    "english" to Color(0xFFF59E0B),
    "science" to Color(0xFF10B981),
    "reasoning" to Color(0xFF8B5CF6),
    "logical reasoning" to Color(0xFF8B5CF6),
    "social" to Color(0xFF0EA5E9),
    "hindi" to Color(0xFFEF4444),
    "computer" to Color(0xFF06B6D4),
    "gk" to Color(0xFFEC4899),
)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    userId: String = "current_user",
) {
    LaunchedEffect(userId) {
        viewModel.loadStats(userId)
    }

    val statsState by viewModel.statsState.collectAsState()

    when (val state = statsState) {
        is UiState.Loading -> LoadingView()
        is UiState.Success -> StatsContent(data = state.data)
        is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.loadStats(userId) })
        is UiState.Empty -> ErrorView(
            message = "No stats available yet. Start a quiz to see your progress!",
            onRetry = { viewModel.loadStats(userId) },
        )
        is UiState.Offline -> ErrorView(
            message = "You are offline. Please check your connection.",
            onRetry = { viewModel.loadStats(userId) },
        )
    }
}

@Composable
private fun StatsContent(data: StatsData) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                Text(
                    text = "Your Stats",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MindquestColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track your learning journey",
                    fontSize = 14.sp,
                    color = MindquestColors.TextSecondary,
                )
            }
        }

        // 3 Summary pills
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
            ) {
                SummaryPills(stats = data.stats, accuracyPct = data.accuracyPct)
            }
        }

        // IQ Score Card (dark hero)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100)) { it / 4 },
            ) {
                IqScoreCard(iqScore = data.stats.iqScore ?: 112, animated = visible)
            }
        }

        // Activity Chart
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 4 },
            ) {
                ActivityChartSection(dailyActivity = data.dailyActivity, stats = data.stats)
            }
        }

        // Subject Breakdown header
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 300)) + slideInVertically(tween(500, delayMillis = 300)) { it / 4 },
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "By Subject",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MindquestColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Chapter progress \u00B7 Quiz accuracy",
                        fontSize = 12.sp,
                        color = MindquestColors.TextSecondary,
                    )
                }
            }
        }

        // Subject cards with tag pills
        itemsIndexed(data.subjectPerformance) { index, subject ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 350 + index * 70)) +
                    slideInVertically(tween(400, delayMillis = 350 + index * 70)) { it / 3 },
            ) {
                SubjectCardNew(
                    subject = subject,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// ── 3 Summary Pills ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryPills(stats: UserStats, accuracyPct: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryPill(
            value = stats.quizzesCompleted.toString(),
            label = "Quizzes",
            color = Color(0xFF6366F1),
            modifier = Modifier.weight(1f),
        )
        SummaryPill(
            value = "${accuracyPct.toInt()}%",
            label = "Accuracy",
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f),
        )
        SummaryPill(
            value = if (stats.streakCurrent > 0) "${stats.streakCurrent}d \uD83D\uDD25" else "0d",
            label = "Streak",
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPill(
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MindquestColors.TextTertiary,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

// ── IQ Score Card ───────────────────────────────────────────────────────────────

@Composable
private fun IqScoreCard(iqScore: Int, animated: Boolean) {
    val currentLevelIdx = IQ_LEVELS.indexOfFirst { iqScore in it.min..it.max }.coerceAtLeast(0)
    val currentLevel = IQ_LEVELS[currentLevelIdx]
    val nextLevel = IQ_LEVELS.getOrNull(currentLevelIdx + 1)
    val pointsToNext = if (nextLevel != null) nextLevel.min - iqScore else 0
    val progressInBand = iqScore - currentLevel.min
    val bandSize = currentLevel.max - currentLevel.min
    val progressPct = if (bandSize > 0) ((progressInBand * 100) / bandSize) else 100

    val infiniteTransition = rememberInfiniteTransition(label = "iq_twinkle")
    val twinkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "twinkle",
    )

    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progressPct / 100f else 0f,
        animationSpec = tween(1000, delayMillis = 500),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
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
        // Decorative glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(currentLevel.color.copy(alpha = 0.08f)),
        )

        // Twinkle stars
        listOf(0.1f to 0.15f, 0.45f to 0.08f, 0.75f to 0.2f, 0.9f to 0.12f).forEach { (xf, yf) ->
            Box(
                modifier = Modifier
                    .offset(x = (xf * 300).dp, y = (yf * 200).dp)
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = twinkleAlpha * 0.5f)),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            // Top: emoji + level name + IQ number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level emoji box
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentLevel.color.copy(alpha = 0.15f))
                            .border(1.5.dp, currentLevel.color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = currentLevel.emoji, fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CURRENT LEVEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.35f),
                            letterSpacing = 1.1.sp,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentLevel.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }

                // IQ number
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = iqScore.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = currentLevel.color,
                    )
                    Text(
                        text = "IQ Score",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Journey path - 5 nodes
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background line
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )

                // Active line
                val activeFraction = currentLevelIdx.toFloat() / (IQ_LEVELS.size - 1).toFloat()
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .fillMaxWidth(activeFraction * 0.9f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(IQ_LEVELS[0].color, currentLevel.color),
                            ),
                        ),
                )

                // Level nodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IQ_LEVELS.forEachIndexed { i, level ->
                        val done = i < currentLevelIdx
                        val current = i == currentLevelIdx
                        val locked = i > currentLevelIdx
                        val nodeSize = if (current) 40.dp else 32.dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(nodeSize)
                                    .clip(CircleShape)
                                    .background(
                                        brush = when {
                                            done -> Brush.linearGradient(listOf(level.color, level.color))
                                            current -> Brush.linearGradient(listOf(level.color, level.dark))
                                            else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f)))
                                        },
                                        shape = CircleShape,
                                    )
                                    .then(
                                        if (current) Modifier.border(2.5.dp, level.color, CircleShape)
                                        else if (locked) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (done) "\u2713" else level.emoji,
                                    fontSize = if (current) 18.sp else 14.sp,
                                    color = if (locked) Color.White.copy(alpha = 0.35f) else Color.Unspecified,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = level.label,
                                fontSize = 8.sp,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    current -> level.color
                                    done -> Color.White.copy(alpha = 0.5f)
                                    else -> Color.White.copy(alpha = 0.2f)
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom: progress to next level
            if (nextLevel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Progress in ${currentLevel.label}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "$progressPct%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(currentLevel.dark, currentLevel.color),
                                        ),
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\uD83C\uDFAF", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$pointsToNext more points to reach ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "${nextLevel.label} ${nextLevel.emoji}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = nextLevel.color,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Activity Chart Section ──────────────────────────────────────────────────────

@Composable
private fun ActivityChartSection(dailyActivity: List<DailyActivity>, stats: UserStats) {
    val last7 = dailyActivity.takeLast(7)
    val totalXp = last7.sumOf { it.xp }
    val totalQuizzes = last7.sumOf { it.quizzes }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Text(
                text = "Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MindquestColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "XP earned over time",
                fontSize = 11.sp,
                color = MindquestColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stat tiles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActivityStatTile(
                    emoji = "\u26A1",
                    label = "Total XP",
                    value = totalXp.toString(),
                    color = Color(0xFF6D28D9),
                    bgStart = Color(0xFFF5F3FF),
                    bgEnd = Color(0xFFEDE9FE),
                    borderColor = Color(0xFFDDD6FE),
                    modifier = Modifier.weight(1f),
                )
                ActivityStatTile(
                    emoji = "\uD83D\uDCDD",
                    label = "Quizzes",
                    value = totalQuizzes.toString(),
                    color = Color(0xFF1D4ED8),
                    bgStart = Color(0xFFEFF6FF),
                    bgEnd = Color(0xFFDBEAFE),
                    borderColor = Color(0xFFBFDBFE),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar chart
            WeeklyBarChart(data = last7)
        }
    }
}

@Composable
private fun ActivityStatTile(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    bgStart: Color,
    bgEnd: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = "$emoji ${label.uppercase()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.7f),
                letterSpacing = 0.8.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(data: List<DailyActivity>) {
    val maxXp = data.maxOfOrNull { it.xp }?.coerceAtLeast(1) ?: 1
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val barMaxHeight = 100f
    val todayIdx = data.size - 1

    Column {
        // Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            data.forEachIndexed { index, activity ->
                val fraction = if (maxXp > 0) activity.xp.toFloat() / maxXp else 0f
                val isToday = index == todayIdx
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(500, delayMillis = index * 60),
                )
                val barHeight = (barMaxHeight * animatedFraction).coerceAtLeast(3f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    // Today XP label
                    if (isToday && activity.xp > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MindquestColors.Primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${activity.xp} XP",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                if (isToday) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFA5B4FC), Color(0xFF4F46E5)),
                                    )
                                } else if (activity.xp > 0) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFC7D2FE), Color(0xFFA5B4FC)),
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)),
                                    )
                                },
                            ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            data.forEachIndexed { index, _ ->
                val isToday = index == todayIdx
                Text(
                    text = dayLabels.getOrElse(index) { "" },
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MindquestColors.Primary else Color(0xFFCBD5E1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Subject Card with Tag Pill ──────────────────────────────────────────────────

@Composable
private fun SubjectCardNew(subject: SubjectPerformance, modifier: Modifier = Modifier) {
    val subjectKey = subject.title.lowercase().trim()
    val accentColor = subjectColors[subjectKey] ?: Color(0xFF6366F1)
    val tag = getSubjectTag(subject.accuracyPct)

    val accColor = when {
        subject.accuracyPct >= 80 -> Color(0xFF10B981)
        subject.accuracyPct >= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFF87171)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.White,
                        ),
                    ),
                )
                .border(1.5.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.08f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                // Top: emoji + name + tag pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Emoji icon box
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.5.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = subject.emoji, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = subject.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MindquestColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Tag pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tag.bg)
                            .border(1.dp, tag.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = tag.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tag.textColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 stat chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SubjectStatChip(
                        emoji = "\uD83C\uDFAF",
                        value = "${subject.accuracyPct}%",
                        label = "Accuracy",
                        color = accColor,
                        modifier = Modifier.weight(1f),
                    )
                    SubjectStatChip(
                        emoji = "\uD83D\uDCDA",
                        value = "${subject.chaptersCompleted}/${subject.totalChapters}",
                        label = "Chapters",
                        color = accentColor,
                        modifier = Modifier.weight(1f),
                    )
                    SubjectStatChip(
                        emoji = "\uD83C\uDFC5",
                        value = "${subject.bestScorePct}%",
                        label = "Best",
                        color = MindquestColors.XpGold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectStatChip(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
                .padding(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                color = Color(0xFFC4C9D6),
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────

private fun formatNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> "${((value / 100_000) / 10.0)}M"
        value >= 1_000 -> "${((value / 100) / 10.0)}K"
        else -> value.toString()
    }
}
