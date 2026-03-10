package com.android.mindquest.presentation.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.components.SecondaryButton
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizIntroScreen(
    moduleColor: String,
    onStartQuiz: () -> Unit,
    onBack: () -> Unit,
) {
    val quiz = QuizSessionHolder.currentQuiz
    val accentColor = introParseHexColor(moduleColor)
    val accentDark = accentColor.copy(
        red = accentColor.red * 0.8f,
        green = accentColor.green * 0.8f,
        blue = accentColor.blue * 0.8f,
    )

    var showHeader by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
        delay(300)
        showDetails = true
        delay(300)
        showButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
    ) {
        TopAppBar(
            title = { Text("Quiz", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MindquestColors.TextPrimary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MindquestColors.TextPrimary,
            ),
        )

        if (quiz == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Quiz not found",
                    color = MindquestColors.TextSecondary,
                    fontSize = 16.sp,
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero section with emoji and title
            AnimatedVisibility(
                visible = showHeader,
                enter = scaleIn(
                    initialScale = 0.5f,
                    animationSpec = tween(600, easing = EaseOutBack),
                ) + fadeIn(tween(400)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Emoji circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.15f),
                                        accentDark.copy(alpha = 0.15f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = QuizSessionHolder.moduleEmoji.ifEmpty { "\uD83D\uDCDD" },
                            fontSize = 44.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = quiz.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MindquestColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )

                    if (QuizSessionHolder.moduleTitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = QuizSessionHolder.moduleTitle,
                            fontSize = 14.sp,
                            color = MindquestColors.TextTertiary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quiz info cards
            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(500, easing = EaseOutBack),
                ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 2x2 stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IntroStatCard(
                            emoji = "\u2753",
                            value = "${quiz.questionCount}",
                            label = "Questions",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f),
                        )
                        IntroStatCard(
                            emoji = "\u23F1\uFE0F",
                            value = formatIntroTime(quiz.timeLimitSeconds),
                            label = "Time Limit",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IntroStatCard(
                            emoji = "\u26A1",
                            value = "${quiz.maxXp}",
                            label = "Max XP",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f),
                        )
                        IntroStatCard(
                            emoji = "\uD83C\uDFAF",
                            value = quiz.difficulty?.replaceFirstChar { it.uppercaseChar() } ?: "Medium",
                            label = "Difficulty",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Previous attempt info
                    if (quiz.bestScore != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF3C7),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = "\uD83C\uDFC6", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Previous Best",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309),
                                    )
                                    Text(
                                        text = "${quiz.bestScore}/${quiz.questionCount} correct",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                    )
                                }
                                Text(
                                    text = "Attempt #${quiz.attemptCount}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(500, easing = EaseOutBack),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryButton(
                        text = if (quiz.bestScore != null) "Try Again \uD83D\uDD25" else "Start Quiz \uD83D\uDE80",
                        onClick = onStartQuiz,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SecondaryButton(
                        text = "Go Back",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun IntroStatCard(
    emoji: String,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MindquestColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MindquestColors.TextTertiary,
            )
        }
    }
}

private fun formatIntroTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        if (remainingSeconds > 0) "${minutes}m ${remainingSeconds}s" else "${minutes}m"
    } else {
        "${seconds}s"
    }
}

private fun introParseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt(),
            )
            8 -> Color(
                alpha = ((colorLong shr 24) and 0xFF).toInt(),
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt(),
            )
            else -> Color(0xFF6200EE)
        }
    } catch (_: Exception) {
        Color(0xFF6200EE)
    }
}
