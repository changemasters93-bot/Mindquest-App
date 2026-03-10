package com.android.mindquest.presentation.quiz

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizReviewScreen(
    onBack: () -> Unit,
) {
    val questions = QuizSessionHolder.completedQuestions
    val answers = QuizSessionHolder.completedAnswers

    val totalCorrect = answers.count { it.isCorrect }
    val totalQuestions = questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Review Answers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MindquestColors.TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MindquestColors.Background,
                titleContentColor = MindquestColors.TextPrimary
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                SummaryCard(
                    totalCorrect = totalCorrect,
                    totalQuestions = totalQuestions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Question Items
            itemsIndexed(questions) { index, question ->
                val answer = answers.getOrNull(index)
                val selectedOptionId = answer?.selected
                val isCorrect = answer?.isCorrect ?: false

                val selectedOption = question.options.find { it.id == selectedOptionId }
                val correctOption = question.options.find { it.isCorrect }

                ReviewQuestionItem(
                    questionNumber = index + 1,
                    questionTitle = question.title,
                    userAnswer = selectedOption?.label ?: "No answer",
                    correctAnswer = correctOption?.label ?: "",
                    isCorrect = isCorrect,
                    explanation = question.explanation,
                    timeMs = answer?.timeMs ?: 0
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalCorrect: Int,
    totalQuestions: Int
) {
    val accuracyPct = if (totalQuestions > 0) (totalCorrect * 100) / totalQuestions else 0
    val summaryColor = when {
        accuracyPct >= 80 -> MindquestColors.Success
        accuracyPct >= 50 -> Color(0xFFFF9800)
        else -> MindquestColors.Error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = summaryColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Score circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(summaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$totalCorrect",
                    color = summaryColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "$totalCorrect out of $totalQuestions correct",
                    color = MindquestColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$accuracyPct% accuracy",
                    color = MindquestColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ReviewQuestionItem(
    questionNumber: Int,
    questionTitle: String,
    userAnswer: String,
    correctAnswer: String,
    isCorrect: Boolean,
    explanation: String,
    timeMs: Long
) {
    val statusColor = if (isCorrect) MindquestColors.Success else MindquestColors.Error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MindquestColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Question number + Status icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$questionNumber",
                            color = statusColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isCorrect) "Correct" else "Incorrect",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (timeMs > 0) {
                    val seconds = timeMs / 1000
                    Text(
                        text = "${seconds}s",
                        color = MindquestColors.TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question title
            Text(
                text = questionTitle,
                color = MindquestColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MindquestColors.SurfaceVariant,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // User's answer
            AnswerRow(
                label = "Your answer",
                answer = userAnswer,
                isCorrect = isCorrect,
                color = statusColor
            )

            // Correct answer (shown only when wrong)
            if (!isCorrect) {
                Spacer(modifier = Modifier.height(10.dp))
                AnswerRow(
                    label = "Correct answer",
                    answer = correctAnswer,
                    isCorrect = true,
                    color = MindquestColors.Success
                )
            }

            // Explanation
            if (explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = MindquestColors.SurfaceVariant,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Explanation",
                    color = MindquestColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = explanation,
                    color = MindquestColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AnswerRow(
    label: String,
    answer: String,
    isCorrect: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MindquestColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = answer,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
