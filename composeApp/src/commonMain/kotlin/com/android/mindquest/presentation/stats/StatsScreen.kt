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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
    onStartIqTest: () -> Unit = {},
) {
    LaunchedEffect(userId) {
        viewModel.loadStats(userId)
    }

    val statsState by viewModel.statsState.collectAsState()
    val activePeriod by viewModel.activePeriod.collectAsState()
    val activityLoading by viewModel.activityLoading.collectAsState()

    when (val state = statsState) {
        is UiState.Loading -> LoadingView()
        is UiState.Success -> StatsContent(
            data = state.data,
            activePeriod = activePeriod,
            activityLoading = activityLoading,
            onPeriodChange = { viewModel.changePeriod(it) },
            onStartIqTest = onStartIqTest,
        )
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
private fun StatsContent(
    data: StatsData,
    activePeriod: String = "last_week",
    activityLoading: Boolean = false,
    onPeriodChange: (String) -> Unit = {},
    onStartIqTest: () -> Unit = {},
) {
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

        // IQ Score Card (dark hero) — or "Take IQ Test" prompt when no score
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100)) { it / 4 },
            ) {
                val iqScore = data.stats.iqScore
                if (iqScore != null && iqScore > 0) {
                    IqScoreCard(iqScore = iqScore, animated = visible)
                } else {
                    IqTestPromptCard(onStartIqTest = onStartIqTest)
                }
            }
        }

        // Activity Chart
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 4 },
            ) {
                ActivityChartSection(
                    dailyActivity = data.dailyActivity,
                    activePeriod = activePeriod,
                    activityLoading = activityLoading,
                    onPeriodChange = onPeriodChange,
                )
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

        // Subject cards sorted by backend rank (per-module rank among all users)
        val rankedSubjects = data.subjectPerformance
            .sortedBy { if (it.rank == 0) Int.MAX_VALUE else it.rank }
            .map { subj -> subj to subj.rank }

        itemsIndexed(rankedSubjects) { index, (subject, rank) ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 350 + index * 70)) +
                    slideInVertically(tween(400, delayMillis = 350 + index * 70)) { it / 3 },
            ) {
                SubjectCardNew(
                    subject = subject,
                    rank = rank,
                    totalModules = data.subjectPerformance.size,
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

// ── IQ Test Prompt (when user has no IQ score) ─────────────────────────────────

@Composable
private fun IqTestPromptCard(onStartIqTest: () -> Unit = {}) {
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
                .background(Color(0xFF818CF8).copy(alpha = 0.08f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "\uD83E\uDDE0", fontSize = 40.sp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Discover Your IQ Level",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Take the IQ Challenge to unlock your score and track your cognitive growth!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Journey level preview (dimmed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IQ_LEVELS.forEach { level ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = level.emoji,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.35f),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = level.label,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.2f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                        ),
                    )
                    .clickable { onStartIqTest() }
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83E\uDDE0  Start IQ Challenge",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// ── Activity Period ──────────────────────────────────────────────────────────────

private enum class ActivityPeriod(val label: String, val apiKey: String, val days: Int) {
    LAST_WEEK("Last Week", "last_week", 7),
    LAST_MONTH("Last Month", "last_month", 30),
    LAST_6_MONTHS("6 Months", "last_6_months", 180);

    /** Subtitle shown below the "Activity" header. */
    val chartSubtitle: String
        get() = when (this) {
            LAST_WEEK -> "Your last 7 days"
            LAST_MONTH -> "Your last 30 days"
            LAST_6_MONTHS -> "Your last 6 months"
        }

    /** Label for the XP stat tile. */
    val xpLabel: String
        get() = when (this) {
            LAST_WEEK -> "Week XP"
            LAST_MONTH -> "Month XP"
            LAST_6_MONTHS -> "6-Month XP"
        }

    /** Label for the quizzes stat tile. */
    val quizzesLabel: String
        get() = when (this) {
            LAST_WEEK -> "Week Quizzes"
            LAST_MONTH -> "Month Quizzes"
            LAST_6_MONTHS -> "6-Month Quizzes"
        }

    /** Empty-state message. */
    val emptyMessage: String
        get() = when (this) {
            LAST_WEEK -> "No activity this week"
            LAST_MONTH -> "No activity this month"
            LAST_6_MONTHS -> "No activity in the last 6 months"
        }
}

// ── Activity Chart Section ──────────────────────────────────────────────────────

@Composable
private fun ActivityChartSection(
    dailyActivity: List<DailyActivity>,
    activePeriod: String = "last_week",
    activityLoading: Boolean = false,
    onPeriodChange: (String) -> Unit = {},
) {
    val selectedPeriod = ActivityPeriod.entries.find { it.apiKey == activePeriod }
        ?: ActivityPeriod.LAST_WEEK

    // Build a lookup from backend data keyed by date
    val dataByDate = dailyActivity.associateBy { it.date }

    val totalXp = dailyActivity.sumOf { it.xp }
    val totalQuizzes = dailyActivity.sumOf { it.quizzes }

    // Always generate full-range chart data so labels appear even without data
    val chartData = generateFullRangeData(selectedPeriod, dataByDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header + period tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MindquestColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedPeriod.chartSubtitle,
                        fontSize = 11.sp,
                        color = MindquestColors.TextSecondary,
                    )
                }

                // Period dropdown selector
                Box {
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF3F4F6))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = selectedPeriod.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MindquestColors.Primary,
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select period",
                            modifier = Modifier.size(16.dp),
                            tint = MindquestColors.Primary,
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        ActivityPeriod.entries.forEach { period ->
                            val isActive = period == selectedPeriod
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = period.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MindquestColors.Primary
                                        else MindquestColors.TextPrimary,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    if (!isActive) onPeriodChange(period.apiKey)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat tiles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActivityStatTile(
                    emoji = "\u26A1",
                    label = selectedPeriod.xpLabel,
                    value = totalXp.toString(),
                    color = Color(0xFF6D28D9),
                    bgStart = Color(0xFFF5F3FF),
                    bgEnd = Color(0xFFEDE9FE),
                    borderColor = Color(0xFFDDD6FE),
                    modifier = Modifier.weight(1f),
                )
                ActivityStatTile(
                    emoji = "\uD83D\uDCDD",
                    label = selectedPeriod.quizzesLabel,
                    value = totalQuizzes.toString(),
                    color = Color(0xFF1D4ED8),
                    bgStart = Color(0xFFEFF6FF),
                    bgEnd = Color(0xFFDBEAFE),
                    borderColor = Color(0xFFBFDBFE),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (activityLoading) {
                // Subtle loading indicator while period data is refreshing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 3.dp,
                    )
                }
            } else {
                // Bar chart — always shows full range labels even without data
                ActivityBarChart(
                    data = chartData,
                    period = selectedPeriod,
                )
            }
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
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.7f),
                letterSpacing = 0.8.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
        }
    }
}

/**
 * Generates the full-range chart data for the selected period, filling in
 * zero-value entries for dates that the backend did not return data for.
 *
 * - LAST_WEEK:     7 individual days (today – 6 … today)
 * - LAST_MONTH:    ~5 weekly chunks over the last 30 days
 * - LAST_6_MONTHS: 6 monthly buckets (this month – 5 … this month)
 */
private fun generateFullRangeData(
    period: ActivityPeriod,
    dataByDate: Map<String, DailyActivity>,
): List<DailyActivity> {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    return when (period) {
        ActivityPeriod.LAST_WEEK -> {
            // 7 individual days: today-6 … today
            (0..6).map { idx ->
                val d = today.minus(DatePeriod(days = 6 - idx))
                val key = d.toString() // "YYYY-MM-DD"
                dataByDate[key] ?: DailyActivity(date = key, quizzes = 0, xp = 0)
            }
        }

        ActivityPeriod.LAST_MONTH -> {
            // 5 weekly chunks covering the last ~35 days
            val numWeeks = 5
            val startDate = today.minus(DatePeriod(days = numWeeks * 7 - 1))
            (0 until numWeeks).map { weekIdx ->
                val weekStart = startDate.plus(DatePeriod(days = weekIdx * 7))
                val weekEnd = if (weekIdx == numWeeks - 1) today
                else startDate.plus(DatePeriod(days = (weekIdx + 1) * 7 - 1))
                // Sum all matching dates within the week
                var xp = 0
                var quizzes = 0
                var d = weekStart
                while (d <= weekEnd) {
                    dataByDate[d.toString()]?.let { xp += it.xp; quizzes += it.quizzes }
                    d = d.plus(DatePeriod(days = 1))
                }
                DailyActivity(date = weekStart.toString(), quizzes = quizzes, xp = xp)
            }
        }

        ActivityPeriod.LAST_6_MONTHS -> {
            // 6 monthly buckets
            (0 until 6).map { idx ->
                val m = today.minus(DatePeriod(months = 5 - idx))
                // Use first day of that month as the bucket key
                val bucketDate = LocalDate(m.year, m.monthNumber, 1)
                val key = bucketDate.toString()
                // Sum all days in that month from the backend data
                val prefix = key.substring(0, 7) // "YYYY-MM"
                var xp = 0
                var quizzes = 0
                dataByDate.forEach { (dateKey, activity) ->
                    if (dateKey.startsWith(prefix)) {
                        xp += activity.xp
                        quizzes += activity.quizzes
                    }
                }
                DailyActivity(date = key, quizzes = quizzes, xp = xp)
            }
        }
    }
}

/**
 * Derives a display label from a date string ("YYYY-MM-DD") based on the active period.
 *
 * The **last** bar in each period gets a special "anchor" label:
 * - LAST_WEEK   → "Mon", "Tue", … last → **"Today"**
 * - LAST_MONTH  → "Mar 5", "Mar 12", … last → **"This Wk"**
 * - LAST_6_MONTHS → "Oct", "Nov", … last → **month name** (current month)
 */
private fun dateLabel(dateStr: String, period: ActivityPeriod, isLast: Boolean = false): String {
    if (dateStr.length < 10) return dateStr

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    return try {
        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(5, 7).toInt()  // 1-12
        val day = dateStr.substring(8, 10).toInt()    // 1-31

        when (period) {
            ActivityPeriod.LAST_WEEK -> {
                if (isLast) "Today"
                else {
                    // Tomohiko Sakamoto's day-of-week algorithm
                    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
                    val y = if (month < 3) year - 1 else year
                    val dow = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
                    dayNames[if (dow == 0) 6 else dow - 1]
                }
            }
            ActivityPeriod.LAST_MONTH -> {
                if (isLast) "This Wk"
                else "${monthNames[month - 1]} $day"
            }
            ActivityPeriod.LAST_6_MONTHS -> {
                monthNames[month - 1]
            }
        }
    } catch (_: Exception) {
        dateStr.takeLast(5)
    }
}

@Composable
private fun ActivityBarChart(data: List<DailyActivity>, period: ActivityPeriod = ActivityPeriod.LAST_WEEK) {
    val maxXp = data.maxOfOrNull { it.xp }?.coerceAtLeast(1) ?: 1
    val barMaxHeight = 100f
    val lastIdx = data.size - 1

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
                val isLast = index == lastIdx
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
                    // Latest bar XP label
                    if (isLast && activity.xp > 0) {
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
                                if (isLast) {
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

        // Labels – derived from actual dates and the active period
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            data.forEachIndexed { index, activity ->
                val isLast = index == lastIdx
                val label = dateLabel(activity.date, period, isLast = isLast)
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) MindquestColors.Primary else Color(0xFFCBD5E1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Subject Card with Tag Pill ──────────────────────────────────────────────────

@Composable
private fun SubjectCardNew(
    subject: SubjectPerformance,
    rank: Int = 0,
    totalModules: Int = 0,
    modifier: Modifier = Modifier,
) {
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

                // 4 stat chips: Rank, Accuracy, Chapters, Best
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    if (rank > 0) {
                        SubjectStatChip(
                            emoji = "\uD83C\uDFC6",
                            value = "#$rank",
                            label = "Rank",
                            color = when (rank) {
                                1 -> Color(0xFFF59E0B) // gold
                                2 -> Color(0xFF9CA3AF) // silver
                                3 -> Color(0xFFC09060) // bronze
                                else -> MindquestColors.Primary
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 7.sp,
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
