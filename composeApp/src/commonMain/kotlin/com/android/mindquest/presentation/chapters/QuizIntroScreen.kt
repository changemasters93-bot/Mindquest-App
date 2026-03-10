package com.android.mindquest.presentation.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.domain.model.Quiz

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun difficultyColor(difficulty: String?): Color {
    return when (difficulty?.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFF59E0B)
        "hard" -> Color(0xFFEF4444)
        else -> Color(0xFF6366F1)
    }
}

private fun difficultyContainerColor(difficulty: String?): Color {
    return when (difficulty?.lowercase()) {
        "easy" -> Color(0xFFDCFCE7)
        "medium" -> Color(0xFFFEF3C7)
        "hard" -> Color(0xFFFEE2E2)
        else -> Color(0xFFE0E7FF)
    }
}

// ── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun QuizIntroScreen(
    quiz: Quiz,
    moduleTitle: String,
    moduleEmoji: String,
    moduleColor: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val accentColor = parseColor(moduleColor)
    val accentDark = darkenColor(accentColor, 0.25f)
    val timeLimitMinutes = quiz.timeLimitSeconds / 60
    val timePerQuestion = if (quiz.questionCount > 0) quiz.timeLimitSeconds / quiz.questionCount else 30

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(accentColor, accentDark)),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quiz.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Text(
                            text = "$moduleTitle \u00B7 Grade 5",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Difficulty badge
                quiz.difficulty?.let { difficulty ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(difficultyContainerColor(difficulty))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = difficulty.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor(difficulty),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Hero Card ────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = accentColor.copy(alpha = 0.2f),
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.9f), accentDark),
                        ),
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = moduleEmoji,
                        fontSize = 56.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = quiz.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = moduleTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Feature Rows ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureRow(
                emoji = "\uD83D\uDCDD",
                title = "${quiz.questionCount} Questions",
                subtitle = "Multiple choice (A-D)",
                accentColor = accentColor,
            )
            FeatureRow(
                emoji = "\u26A1",
                title = "Earn up to ${quiz.maxXp} XP",
                subtitle = "Speed + accuracy bonus",
                accentColor = accentColor,
            )
            FeatureRow(
                emoji = "\u23F1\uFE0F",
                title = "$timeLimitMinutes min time limit",
                subtitle = "$timePerQuestion seconds per question",
                accentColor = accentColor,
            )
            FeatureRow(
                emoji = "\uD83C\uDFC6",
                title = "Rank on Leaderboard",
                subtitle = "Finish to see your rank",
                accentColor = accentColor,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Start Quiz Button ────────────────────────────────────────────
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = accentColor.copy(alpha = 0.4f),
                    spotColor = accentColor.copy(alpha = 0.4f),
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(accentColor, accentDark)),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Start Quiz \uD83D\uDE80",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Hint Text ────────────────────────────────────────────────────
        Text(
            text = "Answer fast + accurately to earn more XP",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MindquestColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Feature Row ──────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emoji,
                    fontSize = 22.sp,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
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
}
