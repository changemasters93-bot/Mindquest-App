package com.android.mindquest.presentation.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LoadingView
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.components.SecondaryButton
import com.android.mindquest.presentation.components.StarRating
import com.android.mindquest.presentation.components.XpPill
import kotlinx.coroutines.delay

@Composable
fun QuizResultScreen(
    viewModel: QuizViewModel,
    moduleColor: String,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    val resultState by viewModel.resultState.collectAsState()
    val accentColor = parseResultHexColor(moduleColor)

    when (val state = resultState) {
        is UiState.Loading -> {
            LoadingView()
        }

        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = onRetry
            )
        }

        is UiState.Success -> {
            val result = state.data
            ResultContent(
                score = result.score,
                totalQuestions = result.totalQuestions,
                accuracyPct = result.accuracyPct,
                xpEarned = result.xpEarned,
                totalXp = result.totalXp.toInt(),
                level = result.level,
                levelChanged = result.levelChanged,
                rankGlobal = result.rankGlobal,
                starRating = result.starRating,
                isReplay = result.isReplay,
                accentColor = accentColor,
                onReview = onReview,
                onRetry = onRetry,
                onExit = onExit
            )
        }

        else -> {}
    }
}

@Composable
private fun ResultContent(
    score: Int,
    totalQuestions: Int,
    accuracyPct: Int,
    xpEarned: Int,
    totalXp: Int,
    level: Int,
    levelChanged: Boolean,
    rankGlobal: Int,
    starRating: Int,
    isReplay: Boolean,
    accentColor: Color,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showStars by remember { mutableStateOf(false) }
    var showScore by remember { mutableStateOf(false) }
    var showAccuracy by remember { mutableStateOf(false) }
    var showXp by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        delay(200)
        showStars = true
        delay(400)
        showScore = true
        delay(300)
        showAccuracy = true
        delay(300)
        showXp = true
        delay(300)
        showDetails = true
        delay(200)
        showButtons = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { -40 },
                animationSpec = tween(500, easing = EaseOutBack)
            )
        ) {
            Text(
                text = if (starRating >= 3) "Excellent!" else if (starRating >= 2) "Good Job!" else "Keep Trying!",
                color = MindquestColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Star Rating
        AnimatedVisibility(
            visible = showStars,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = tween(600, easing = EaseOutElastic)
            ) + fadeIn(tween(300))
        ) {
            StarRating(
                rating = starRating,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Score Fraction
        AnimatedVisibility(
            visible = showScore,
            enter = scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(500, easing = EaseOutBack)
            ) + fadeIn(tween(300))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score/$totalQuestions",
                    color = accentColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Questions Correct",
                    color = MindquestColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Accuracy Circle
        AnimatedVisibility(
            visible = showAccuracy,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = tween(600, easing = EaseOutBack)
            ) + fadeIn(tween(400))
        ) {
            AccuracyCircle(
                accuracyPct = accuracyPct,
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // XP Earned Pill
        AnimatedVisibility(
            visible = showXp,
            enter = scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(500, easing = EaseOutBack)
            ) + fadeIn(tween(300))
        ) {
            XpPill(
                xp = xpEarned,
                level = level,
                maxXpForLevel = (level + 1) * 100,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Level and Rank Row
        AnimatedVisibility(
            visible = showDetails,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(400)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Level Badge
                StatBadge(
                    icon = {
                        Text(
                            text = "\uD83D\uDCC8",
                            fontSize = 24.sp,
                        )
                    },
                    label = "Level",
                    value = "$level",
                    highlight = levelChanged,
                    accentColor = accentColor
                )

                // Global Rank
                StatBadge(
                    icon = {
                        Text(
                            text = "\uD83C\uDF0D",
                            fontSize = 24.sp,
                        )
                    },
                    label = "Global Rank",
                    value = "#$rankGlobal",
                    highlight = false,
                    accentColor = accentColor
                )

                // Total XP
                StatBadge(
                    icon = {
                        Text(
                            text = "\uD83C\uDFC6",
                            fontSize = 24.sp,
                        )
                    },
                    label = "Total XP",
                    value = "$totalXp",
                    highlight = false,
                    accentColor = accentColor
                )
            }
        }

        // Level Changed Banner
        if (levelChanged) {
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = showDetails,
                enter = scaleIn(
                    initialScale = 0.5f,
                    animationSpec = tween(600, easing = EaseOutBack)
                ) + fadeIn(tween(400))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "\uD83C\uDFC6",
                            fontSize = 28.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Level Up!",
                                color = accentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You reached Level $level",
                                color = MindquestColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Buttons
        AnimatedVisibility(
            visible = showButtons,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(400)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    text = "Continue",
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryButton(
                    text = "Review Answers",
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryButton(
                    text = "Try Again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AccuracyCircle(
    accuracyPct: Int,
    accentColor: Color
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(accuracyPct) {
        animatedProgress.animateTo(
            targetValue = accuracyPct / 100f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            // Track
            drawArc(
                color = accentColor.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedProgress.value * 100).toInt()}%",
                color = MindquestColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Accuracy",
                color = MindquestColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatBadge(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    highlight: Boolean,
    accentColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (highlight) 1.1f else 1f,
        animationSpec = tween(600, easing = EaseOutBack)
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) accentColor.copy(alpha = 0.08f)
            else MindquestColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (highlight) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = MindquestColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = MindquestColors.TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun parseResultHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt()
            )
            8 -> Color(
                alpha = ((colorLong shr 24) and 0xFF).toInt(),
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt()
            )
            else -> Color(0xFF6200EE)
        }
    } catch (e: Exception) {
        Color(0xFF6200EE)
    }
}
