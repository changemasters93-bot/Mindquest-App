package com.android.mindquest.presentation.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.domain.model.MatchPair
import com.android.mindquest.domain.model.Question
import com.android.mindquest.domain.model.QuestionOption
import com.android.mindquest.domain.model.QuestionType
import com.android.mindquest.domain.model.QuizBehavior
import com.android.mindquest.presentation.components.MindquestProgressBar
import com.android.mindquest.presentation.components.PrimaryButton
import com.android.mindquest.presentation.quiz.visual.QuizVisualTokenMapper
import com.android.mindquest.presentation.quiz.visual.VisualTokenFromText
import com.android.mindquest.presentation.quiz.visual.VisualTokenView
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun QuizPlayScreen(
    viewModel: QuizViewModel,
    moduleColor: String,
    onFinish: () -> Unit,
    onReview: () -> Unit = {},
    onRetry: () -> Unit = {},
    onClose: () -> Unit = onFinish,
    onPause: () -> Unit = {},
) {
    val quizState by viewModel.quizState.collectAsState()
    val resultState by viewModel.resultState.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val nudge by viewModel.nudgeState.collectAsState()

    val config = viewModel.quizConfig
    val accentColor = parseHexColor(config.accentColorHex ?: moduleColor)
    val currentQuestion = quizState.currentQuestion

    LaunchedEffect(timeLeft) {
        if (timeLeft <= 0 && currentQuestion != null) {
            if (!quizState.isConfirmed) {
                viewModel.confirmAnswer()
            }
        }
    }

    // Show loading spinner while quiz is being fetched from API (daily challenge / IQ test)
    if (quizState.totalQuestions == 0 && currentQuestion == null) {
        when (resultState) {
            is com.android.mindquest.core.util.UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MindquestColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "\u274C", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (resultState as com.android.mindquest.core.util.UiState.Error).message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MindquestColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(text = "Go Back", onClick = onClose)
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MindquestColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading quiz...",
                            fontSize = 16.sp,
                            color = MindquestColors.TextSecondary,
                        )
                    }
                }
            }
        }
        return
    }

    // Show results screen when quiz is submitted
    val quizFinished = quizState.currentIndex >= quizState.totalQuestions && quizState.totalQuestions > 0
    if (quizFinished) {
        when (val result = resultState) {
            is com.android.mindquest.core.util.UiState.Success -> {
                if (config.behavior == QuizBehavior.IQ_TEST) {
                    val iqScore = result.data.iqScore
                        ?: (55 + (result.data.score * 105.0 / result.data.totalQuestions.coerceAtLeast(1)).toInt())
                    IqResultContent(
                        iqScore = iqScore,
                        score = result.data.score,
                        totalQuestions = result.data.totalQuestions,
                        xpEarned = result.data.xpEarned,
                        onExit = onFinish,
                    )
                } else {
                    QuizResultsScreen(
                        result = result.data,
                        answers = viewModel.answers,
                        accentColor = accentColor,
                        onFinish = onFinish,
                        onReview = if (config.showReviewButton) onReview else ({}),
                        onRetry = if (config.showRetryButton) onRetry else ({}),
                        showReview = config.showReviewButton,
                        showRetry = config.showRetryButton,
                    )
                }
            }
            is com.android.mindquest.core.util.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MindquestColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Submitting your answers...",
                            fontSize = 16.sp,
                            color = MindquestColors.TextSecondary,
                        )
                    }
                }
            }
            else -> {
                // Error state — offer retry before falling back to close
                Box(
                    modifier = Modifier.fillMaxSize().background(MindquestColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    ) {
                        Text(text = "\u274C", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Submission failed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MindquestColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your answers are saved. Tap retry to submit again.",
                            fontSize = 14.sp,
                            color = MindquestColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Retry",
                            onClick = { viewModel.retrySubmission() },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            text = "Go Back",
                            onClick = onClose,
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        QuizTopBar(
            timeLeft = timeLeft,
            currentIndex = quizState.currentIndex,
            totalQuestions = quizState.totalQuestions,
            accentColor = accentColor,
            moduleEmoji = QuizSessionHolder.moduleEmoji,
            moduleTitle = config.topBarLabel,
            onClose = onClose,
            showPause = config.allowPause,
            onPause = onPause,
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (currentQuestion != null) {
            // key() forces Compose to destroy/recreate when the question changes,
            // preventing animated colors (green/red feedback) from bleeding across questions.
            key(quizState.currentIndex) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Question display – adapt based on type
                when (currentQuestion.questionType) {
                    QuestionType.STATEMENT_REASON -> QuestionCard(
                        title = currentQuestion.prompt ?: "Analyze the statement and reason",
                        prompt = "",
                    )
                    QuestionType.MATRIX, QuestionType.TABLE_DATA,
                    QuestionType.MEMORY, QuestionType.GRID_PATTERN,
                    QuestionType.GRID_CELL_SELECT, QuestionType.GRID_PATTERN_BOOLEAN,
                    QuestionType.SEQUENCE_TAP -> QuestionCard(
                        title = currentQuestion.title,
                        prompt = "",
                    )
                    else -> QuestionCard(
                        title = currentQuestion.title,
                        prompt = currentQuestion.prompt ?: "",
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Whether to show correct/incorrect answer feedback
                // Persistent feedback OR brief flash both enable green/red highlighting
                val showFeedback = config.showAnswerFeedback || config.feedbackFlashDurationMs != null

                // Type-specific answer UI
                when (currentQuestion.questionType) {
                    QuestionType.TRUE_FALSE -> TrueFalseOptions(
                        options = currentQuestion.options,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.FILL_BLANK -> FillBlankSection(
                        answer = quizState.fillBlankAnswer,
                        isConfirmed = quizState.isConfirmed,
                        correctAnswer = currentQuestion.options.find { it.isCorrect }?.label ?: "",
                        isCorrect = if (showFeedback) quizState.isCorrect else null,
                        onAnswerChange = { viewModel.setFillBlankAnswer(it) },
                    )

                    QuestionType.ORDERING -> OrderingSection(
                        options = currentQuestion.options,
                        orderedIds = quizState.orderedOptionIds,
                        isConfirmed = quizState.isConfirmed,
                        accentColor = accentColor,
                        showFeedback = showFeedback,
                        onMoveItem = { from, to -> viewModel.moveOrderItem(from, to) },
                    )

                    QuestionType.MATCH -> {
                        val matchPairs = currentQuestion.matchPairs
                        if (!matchPairs.isNullOrEmpty()) {
                            MatchSection(
                                matchPairs = matchPairs,
                                matchedPairs = quizState.matchedPairs,
                                selectedLeft = quizState.selectedMatchLeft,
                                isConfirmed = quizState.isConfirmed,
                                accentColor = accentColor,
                                showFeedback = showFeedback,
                                onSelectLeft = { viewModel.selectMatchLeft(it) },
                                onSelectRight = { viewModel.selectMatchRight(it) },
                            )
                        } else {
                            // Fallback: render as MCQ when match_pairs data is missing
                            McqOptionsGrid(
                                options = currentQuestion.options,
                                selectedOptionId = quizState.selectedOptionId,
                                isConfirmed = quizState.isConfirmed,
                                showFeedback = showFeedback,
                                accentColor = accentColor,
                                onSelect = { viewModel.selectOption(it) },
                            )
                        }
                    }

                    QuestionType.SELECT_WORD -> SelectWordSection(
                        options = currentQuestion.options,
                        selectedIds = quizState.selectedWordIds,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onToggle = { viewModel.toggleWordSelection(it) },
                    )

                    QuestionType.STATEMENT_REASON -> StatementReasonSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.VISUAL_SINGLE_CHOICE -> VisualChoiceGrid(
                        options = currentQuestion.options,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.MATRIX -> MatrixSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.GRID_PATTERN -> GridPatternSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.TABLE_DATA -> TableDataSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.MEMORY -> MemorySection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.GRID_CELL_SELECT -> GridCellSelectSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.GRID_PATTERN_BOOLEAN -> GridPatternBooleanSection(
                        question = currentQuestion,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )

                    QuestionType.SEQUENCE_TAP -> SequenceTapSection(
                        question = currentQuestion,
                        sequenceTapIds = quizState.sequenceTapIds,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onTap = { viewModel.tapSequenceItem(it) },
                    )

                    else -> McqOptionsGrid(
                        options = currentQuestion.options,
                        selectedOptionId = quizState.selectedOptionId,
                        isConfirmed = quizState.isConfirmed,
                        showFeedback = showFeedback,
                        accentColor = accentColor,
                        onSelect = { viewModel.selectOption(it) },
                    )
                }

                // Explanation (only in feedback modes)
                if (config.showExplanation && quizState.isConfirmed && currentQuestion.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExplanationCard(explanation = currentQuestion.explanation)
                }

                // Nudge hint (module quiz only, on wrong answers)
                if (nudge != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NudgeCard(nudge = nudge!!, accentColor = accentColor)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            } // end key(quizState.currentIndex)

            // Bottom Button
            if (!quizState.isConfirmed) {
                PrimaryButton(
                    text = "Confirm",
                    onClick = { viewModel.confirmAnswer() },
                    enabled = quizState.hasAnswer(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                )
            } else if (config.autoAdvanceDelayMs == null && config.feedbackFlashDurationMs == null) {
                // Manual advance — show Next/Finish button
                // (hidden when autoAdvanceDelayMs or feedbackFlashDurationMs auto-advances)
                val isLastQuestion = quizState.currentIndex >= quizState.totalQuestions - 1
                PrimaryButton(
                    text = if (isLastQuestion) "Finish Quiz \u2192" else "Next \u2192",
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                )
            }
            // If autoAdvanceDelayMs or feedbackFlashDurationMs != null, no button shown (auto-advances)
        } else {
            // Loading indicator while quiz data is being fetched from API
            // (daily challenges, IQ tests — quiz loads lazily)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accentColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading quiz...",
                        fontSize = 16.sp,
                        color = MindquestColors.TextSecondary,
                    )
                }
            }
        }
    }
}

// ── Quiz Results Screen ─────────────────────────────────────────────────

@Composable
private fun QuizResultsScreen(
    result: com.android.mindquest.domain.model.QuizResult,
    answers: List<com.android.mindquest.domain.model.QuizAnswer>,
    accentColor: Color,
    onFinish: () -> Unit,
    onReview: () -> Unit = {},
    onRetry: () -> Unit = {},
    showReview: Boolean = true,
    showRetry: Boolean = true,
) {
    val starEmoji = when (result.starRating) {
        5 -> "\u2B50\u2B50\u2B50\u2B50\u2B50"
        4 -> "\u2B50\u2B50\u2B50\u2B50"
        3 -> "\u2B50\u2B50\u2B50"
        2 -> "\u2B50\u2B50"
        1 -> "\u2B50"
        else -> ""
    }

    val celebEmoji = when {
        result.accuracyPct >= 90 -> "\uD83C\uDF89"
        result.accuracyPct >= 70 -> "\uD83C\uDF1F"
        result.accuracyPct >= 50 -> "\uD83D\uDC4D"
        else -> "\uD83D\uDCAA"
    }

    val messageText = when {
        result.accuracyPct >= 90 -> "Outstanding!"
        result.accuracyPct >= 70 -> "Great Job!"
        result.accuracyPct >= 50 -> "Good Effort!"
        else -> "Keep Practicing!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF312E81),
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Celebration emoji
        Text(text = celebEmoji, fontSize = 64.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // Stars
        Text(text = starEmoji, fontSize = 28.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // Message
        Text(
            text = messageText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quiz Complete",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Score ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${result.score}/${result.totalQuestions}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Correct",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2x2 Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResultStatCard(
                emoji = "\uD83C\uDFAF",
                value = "${result.accuracyPct}%",
                label = "Accuracy",
                modifier = Modifier.weight(1f),
            )
            ResultStatCard(
                emoji = "\u26A1",
                value = "${result.xpEarned}",
                label = "XP Earned",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResultStatCard(
                emoji = "\uD83C\uDF10",
                value = "#${result.rankGlobal}",
                label = "Rank",
                modifier = Modifier.weight(1f),
            )
            ResultStatCard(
                emoji = "\u2B50",
                value = "${result.starRating}/5",
                label = "Stars",
                modifier = Modifier.weight(1f),
            )
        }

        // Level up notification
        if (result.levelChanged) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
                        ),
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83C\uDF89 Level Up! You reached Level ${result.level}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Continue (primary)
            PrimaryButton(
                text = "Continue",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            )

            // Review Answers (only in modes with feedback)
            if (showReview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable(onClick = onReview),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDCCB Review Answers",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Try Again (only in modes that allow retry)
            if (showRetry) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDD04 Try Again",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ── Top Bar (JSX-matching header) ────────────────────────────────────────

@Composable
private fun QuizTopBar(
    timeLeft: Int,
    currentIndex: Int,
    totalQuestions: Int,
    accentColor: Color,
    moduleEmoji: String = "",
    moduleTitle: String = "",
    onClose: () -> Unit = {},
    showPause: Boolean = false,
    onPause: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: Close · Module info · Timer (+ optional Pause)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MindquestColors.SurfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2715",
                    fontSize = 16.sp,
                    color = MindquestColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Module emoji + title
            if (moduleEmoji.isNotEmpty()) {
                Text(text = moduleEmoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = moduleTitle.ifEmpty { "Quiz" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Pause button (tournament only)
            if (showPause) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MindquestColors.SurfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u23F8\uFE0F",
                        fontSize = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Timer pill
            TimerPill(timeLeft = timeLeft, accentColor = accentColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 2: Progress bar with question counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val progress = if (totalQuestions > 0) {
                (currentIndex.toFloat() + if (totalQuestions > 0) 0.5f else 0f) / totalQuestions.toFloat()
            } else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 400),
            )

            // Segmented progress bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MindquestColors.SurfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f)),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Question counter badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${currentIndex + 1}/$totalQuestions",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TimerPill(timeLeft: Int, accentColor: Color) {
    val timerColor by animateColorAsState(
        targetValue = when {
            timeLeft <= 5 -> MindquestColors.Error
            timeLeft <= 10 -> Color(0xFFFF9800)
            else -> accentColor
        },
        animationSpec = tween(durationMillis = 300),
    )

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeText = if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')}" else "$seconds"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(timerColor.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u23F1\uFE0F",
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeText,
                color = timerColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Question Card ───────────────────────────────────────────────────────

@Composable
private fun QuestionCard(title: String, prompt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MindquestColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = title,
                color = MindquestColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            )
            if (prompt.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = prompt,
                    color = MindquestColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

// ── MCQ 2x2 Grid ────────────────────────────────────────────────────────

private enum class OptionState { DEFAULT, SELECTED, CORRECT, INCORRECT }

@Composable
private fun McqOptionsGrid(
    options: List<QuestionOption>,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    val letters = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    val rows = options.chunked(2)

    rows.forEachIndexed { rowIdx, row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEachIndexed { colIdx, option ->
                val globalIdx = rowIdx * 2 + colIdx
                val letter = letters.getOrElse(globalIdx) { "${globalIdx + 1}" }
                val isSelected = selectedOptionId == option.id
                val state = when {
                    !isConfirmed && isSelected -> OptionState.SELECTED
                    isConfirmed && showFeedback && option.isCorrect -> OptionState.CORRECT
                    isConfirmed && showFeedback && isSelected && !option.isCorrect -> OptionState.INCORRECT
                    isConfirmed && isSelected -> OptionState.SELECTED
                    else -> OptionState.DEFAULT
                }

                GridOptionCard(
                    letter = letter,
                    label = option.label,
                    optionState = state,
                    accentColor = accentColor,
                    enabled = !isConfirmed,
                    onClick = { onSelect(option.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun GridOptionCard(
    letter: String,
    label: String,
    optionState: OptionState,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (optionState == OptionState.SELECTED) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
    )

    // Instant color changes — no tween animation.
    // Animated colors caused stale feedback (green/red) to briefly leak into the
    // next question even with key()-based recreation, because the animation system
    // could show one frame of the old target before snapping to the new value.
    val backgroundColor = when (optionState) {
        OptionState.CORRECT -> MindquestColors.Success.copy(alpha = 0.12f)
        OptionState.INCORRECT -> MindquestColors.Error.copy(alpha = 0.12f)
        OptionState.SELECTED -> accentColor.copy(alpha = 0.08f)
        OptionState.DEFAULT -> MindquestColors.Surface
    }

    val borderColor = when (optionState) {
        OptionState.CORRECT -> MindquestColors.Success
        OptionState.INCORRECT -> MindquestColors.Error
        OptionState.SELECTED -> accentColor
        OptionState.DEFAULT -> MindquestColors.SurfaceVariant
    }

    val badgeColor = when (optionState) {
        OptionState.CORRECT -> MindquestColors.Success
        OptionState.INCORRECT -> MindquestColors.Error
        OptionState.SELECTED -> accentColor
        OptionState.DEFAULT -> MindquestColors.SurfaceVariant
    }

    Card(
        modifier = modifier
            .scale(scale)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (optionState != OptionState.DEFAULT) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Letter badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                when (optionState) {
                    OptionState.CORRECT -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    OptionState.INCORRECT -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorrect",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    else -> Text(
                        text = letter,
                        color = if (optionState == OptionState.SELECTED) Color.White
                        else MindquestColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                color = MindquestColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (optionState != OptionState.DEFAULT) FontWeight.Medium
                else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── TRUE / FALSE ────────────────────────────────────────────────────────

@Composable
private fun TrueFalseOptions(
    options: List<QuestionOption>,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        options.forEach { option ->
            val isSelected = selectedOptionId == option.id
            val isTrue = option.label.equals("True", ignoreCase = true)

            val state = when {
                !isConfirmed && isSelected -> OptionState.SELECTED
                isConfirmed && showFeedback && option.isCorrect -> OptionState.CORRECT
                isConfirmed && showFeedback && isSelected && !option.isCorrect -> OptionState.INCORRECT
                isConfirmed && isSelected -> OptionState.SELECTED
                else -> OptionState.DEFAULT
            }

            // Instant color — prevents stale feedback leaking across questions
            val bgColor = when (state) {
                OptionState.CORRECT -> MindquestColors.Success.copy(alpha = 0.15f)
                OptionState.INCORRECT -> MindquestColors.Error.copy(alpha = 0.15f)
                OptionState.SELECTED -> if (isTrue) MindquestColors.Success.copy(alpha = 0.1f)
                else MindquestColors.Error.copy(alpha = 0.1f)
                OptionState.DEFAULT -> MindquestColors.Surface
            }

            // Instant color — prevents stale feedback leaking across questions
            val borderColor = when (state) {
                OptionState.CORRECT -> MindquestColors.Success
                OptionState.INCORRECT -> MindquestColors.Error
                OptionState.SELECTED -> if (isTrue) MindquestColors.Success else MindquestColors.Error
                OptionState.DEFAULT -> MindquestColors.SurfaceVariant
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!isConfirmed) Modifier.clickable { onSelect(option.id) }
                        else Modifier
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(
                    if (state != OptionState.DEFAULT) 2.dp else 1.dp,
                    borderColor,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isTrue) "\u2705" else "\u274C",
                        fontSize = 36.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = option.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MindquestColors.TextPrimary,
                    )
                }
            }
        }
    }
}

// ── FILL BLANK ──────────────────────────────────────────────────────────

@Composable
private fun FillBlankSection(
    answer: String,
    isConfirmed: Boolean,
    correctAnswer: String,
    isCorrect: Boolean?,
    onAnswerChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = answer,
            onValueChange = { if (!isConfirmed) onAnswerChange(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConfirmed,
            label = { Text("Your answer") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MindquestColors.Primary,
                unfocusedBorderColor = MindquestColors.SurfaceVariant,
                disabledBorderColor = when (isCorrect) {
                    true -> MindquestColors.Success
                    false -> MindquestColors.Error
                    null -> MindquestColors.SurfaceVariant
                },
                disabledTextColor = MindquestColors.TextPrimary,
                disabledLabelColor = MindquestColors.TextSecondary,
            ),
        )

        if (isConfirmed && isCorrect == false) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MindquestColors.SuccessContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2705",
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Correct answer: $correctAnswer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MindquestColors.SuccessDark,
                )
            }
        }
    }
}

// ── ORDERING (tap-to-swap) ───────────────────────────────────────────────

@Composable
private fun OrderingSection(
    options: List<QuestionOption>,
    orderedIds: List<String>,
    isConfirmed: Boolean,
    accentColor: Color,
    showFeedback: Boolean = true,
    onMoveItem: (Int, Int) -> Unit,
) {
    val optionsMap = options.associateBy { it.id }
    val correctOrder = options
        .sortedBy { it.correctPosition ?: it.displayOrder }
        .map { it.id }

    // Drag state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val itemHeightDp = 60.dp
    val spacingDp = 6.dp
    val density = LocalDensity.current
    val itemPxHeight = with(density) { (itemHeightDp + spacingDp).toPx() }

    if (!isConfirmed) {
        Text(
            text = "Drag items to reorder",
            fontSize = 12.sp,
            color = MindquestColors.TextTertiary,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacingDp)) {
        orderedIds.forEachIndexed { index, optionId ->
            val option = optionsMap[optionId] ?: return@forEachIndexed
            val isCorrectPosition = isConfirmed && correctOrder.getOrNull(index) == optionId
            val isDragging = draggingIndex == index

            // Idle wiggle animation
            val idleWiggle = remember(optionId) { Animatable(0f) }
            LaunchedEffect(isConfirmed, draggingIndex, optionId) {
                if (!isConfirmed && draggingIndex == null) {
                    while (true) {
                        idleWiggle.animateTo(1.5f, animationSpec = tween(380))
                        idleWiggle.animateTo(-1.5f, animationSpec = tween(380))
                    }
                } else {
                    idleWiggle.snapTo(0f)
                }
            }

            val bgColor = when {
                isConfirmed && showFeedback && isCorrectPosition -> MindquestColors.Success.copy(alpha = 0.1f)
                isConfirmed && showFeedback && !isCorrectPosition -> MindquestColors.Error.copy(alpha = 0.1f)
                isConfirmed && !showFeedback -> accentColor.copy(alpha = 0.06f)
                isDragging -> accentColor.copy(alpha = 0.12f)
                else -> MindquestColors.Surface
            }

            val borderColor = when {
                isConfirmed && showFeedback && isCorrectPosition -> MindquestColors.Success
                isConfirmed && showFeedback && !isCorrectPosition -> MindquestColors.Error
                isConfirmed && !showFeedback -> accentColor.copy(alpha = 0.4f)
                isDragging -> accentColor
                else -> MindquestColors.SurfaceVariant
            }

            val badgeColor = when {
                isConfirmed && showFeedback && isCorrectPosition -> MindquestColors.Success
                isConfirmed && showFeedback -> MindquestColors.Error
                isConfirmed && !showFeedback -> accentColor.copy(alpha = 0.5f)
                isDragging -> accentColor
                else -> MindquestColors.SurfaceVariant
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp)
                    .zIndex(if (isDragging) 10f else 0f)
                    .graphicsLayer {
                        translationX = idleWiggle.value
                        translationY = if (isDragging) dragOffsetY else 0f
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                        shadowElevation = if (isDragging) 12f else 2f
                    }
                    .then(
                        if (!isConfirmed) {
                            Modifier.pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y

                                        val fromIndex = draggingIndex ?: return@detectDragGestures
                                        val draggedCenter =
                                            fromIndex * itemPxHeight + dragOffsetY + itemPxHeight / 2f

                                        val closestIndex = orderedIds.indices.minByOrNull { i ->
                                            kotlin.math.abs(i * itemPxHeight + itemPxHeight / 2f - draggedCenter)
                                        } ?: fromIndex

                                        if (closestIndex != fromIndex) {
                                            onMoveItem(fromIndex, closestIndex)
                                            draggingIndex = closestIndex
                                            dragOffsetY -= (closestIndex - fromIndex) * itemPxHeight
                                        }
                                    },
                                )
                            }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(
                    if (isDragging || isConfirmed) 2.dp else 1.dp,
                    borderColor,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Grip handle
                    if (!isConfirmed) {
                        Text(
                            text = "\u22EE\u22EE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDragging) accentColor else MindquestColors.TextTertiary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    // Position badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isConfirmed && showFeedback) {
                            Icon(
                                imageVector = if (isCorrectPosition) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (isCorrectPosition) "Correct position" else "Incorrect position",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = if (isDragging || (isConfirmed && !showFeedback)) Color.White else MindquestColors.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = option.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MindquestColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    // Drag hint icon
                    if (!isConfirmed) {
                        Text(
                            text = "\u2195",
                            fontSize = 18.sp,
                            color = if (isDragging) accentColor else MindquestColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

// ── MATCH (color-coded pairs) ────────────────────────────────────────────

private val matchColors = listOf(
    Color(0xFF6366F1), // indigo
    Color(0xFFEC4899), // pink
    Color(0xFFF59E0B), // amber
    Color(0xFF10B981), // emerald
    Color(0xFF3B82F6), // blue
    Color(0xFFEF4444), // red
    Color(0xFF8B5CF6), // violet
    Color(0xFF06B6D4), // cyan
)

@Composable
private fun MatchSection(
    matchPairs: List<MatchPair>,
    matchedPairs: Map<String, String>,
    selectedLeft: String?,
    isConfirmed: Boolean,
    accentColor: Color,
    showFeedback: Boolean = true,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit,
) {
    val shuffledRight = remember(matchPairs) {
        matchPairs.map { it.rightText }.shuffled()
    }

    // Assign colors to matched pairs
    val pairColorMap = remember(matchedPairs) {
        val map = mutableMapOf<String, Color>()
        matchedPairs.keys.forEachIndexed { i, leftId ->
            map[leftId] = matchColors[i % matchColors.size]
        }
        map
    }

    Text(
        text = if (selectedLeft != null) "Now tap its match on the right \u2192"
        else "Tap a left item, then tap its match on the right",
        fontSize = 12.sp,
        color = if (selectedLeft != null) accentColor else MindquestColors.TextTertiary,
        fontWeight = if (selectedLeft != null) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.padding(bottom = 10.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Left column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            matchPairs.forEach { pair ->
                val isSelectedLeft = selectedLeft == pair.id
                val isMatched = matchedPairs.containsKey(pair.id)
                val matchColor = pairColorMap[pair.id]
                val isCorrectMatch = isConfirmed && showFeedback && matchedPairs[pair.id] == pair.rightText
                val isWrongMatch = isConfirmed && showFeedback && isMatched && matchedPairs[pair.id] != pair.rightText

                val bgColor = when {
                    isCorrectMatch -> MindquestColors.Success.copy(alpha = 0.12f)
                    isWrongMatch -> MindquestColors.Error.copy(alpha = 0.12f)
                    isConfirmed && !showFeedback && isMatched -> accentColor.copy(alpha = 0.06f)
                    isSelectedLeft -> accentColor.copy(alpha = 0.15f)
                    isMatched && matchColor != null -> matchColor.copy(alpha = 0.08f)
                    else -> MindquestColors.Surface
                }

                val borderClr = when {
                    isCorrectMatch -> MindquestColors.Success
                    isWrongMatch -> MindquestColors.Error
                    isConfirmed && !showFeedback && isMatched -> accentColor.copy(alpha = 0.4f)
                    isSelectedLeft -> accentColor
                    isMatched && matchColor != null -> matchColor.copy(alpha = 0.5f)
                    else -> MindquestColors.SurfaceVariant
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isConfirmed) Modifier.clickable {
                                if (isMatched) {
                                    // Tap matched item to re-select it
                                    onSelectLeft(pair.id)
                                } else {
                                    onSelectLeft(pair.id)
                                }
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(
                        if (isSelectedLeft || isConfirmed) 2.dp else 1.dp,
                        borderClr,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Color dot for matched pairs
                        if (isMatched && matchColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(matchColor),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = pair.leftText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MindquestColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )

                        if (isConfirmed && showFeedback) {
                            Icon(
                                imageVector = if (isCorrectMatch) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (isCorrectMatch) "Correct match" else "Incorrect match",
                                tint = if (isCorrectMatch) MindquestColors.Success else MindquestColors.Error,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Right column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            shuffledRight.forEach { rightText ->
                val isUsed = matchedPairs.values.contains(rightText)
                val hasSelectedLeft = selectedLeft != null
                // Find which left item is matched to this right item
                val matchedLeftId = matchedPairs.entries.find { it.value == rightText }?.key
                val matchColor = if (matchedLeftId != null) pairColorMap[matchedLeftId] else null
                val isHighlighted = hasSelectedLeft && !isUsed

                val bgColor = when {
                    isUsed && matchColor != null -> matchColor.copy(alpha = 0.08f)
                    isHighlighted -> accentColor.copy(alpha = 0.04f)
                    else -> MindquestColors.Surface
                }

                val borderClr = when {
                    isUsed && matchColor != null -> matchColor.copy(alpha = 0.5f)
                    isHighlighted -> accentColor.copy(alpha = 0.4f)
                    else -> MindquestColors.SurfaceVariant
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isConfirmed && hasSelectedLeft && !isUsed)
                                Modifier.clickable { onSelectRight(rightText) }
                            else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(
                        if (isHighlighted) 1.5.dp else 1.dp,
                        borderClr,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isUsed && matchColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(matchColor),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = rightText,
                            fontSize = 13.sp,
                            fontWeight = if (isUsed) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isUsed) MindquestColors.TextTertiary
                            else MindquestColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )

                        if (isHighlighted) {
                            Text(
                                text = "\u2190",
                                fontSize = 14.sp,
                                color = accentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── SELECT WORD ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectWordSection(
    options: List<QuestionOption>,
    selectedIds: Set<String>,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onToggle: (String) -> Unit,
) {
    Text(
        text = "Tap the correct words",
        fontSize = 12.sp,
        color = MindquestColors.TextTertiary,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.id in selectedIds
            val isCorrectWord = isConfirmed && showFeedback && option.isCorrect
            val isWrongSelection = isConfirmed && showFeedback && isSelected && !option.isCorrect
            val isMissed = isConfirmed && showFeedback && option.isCorrect && !isSelected

            val chipBg = when {
                isCorrectWord && isSelected -> MindquestColors.Success.copy(alpha = 0.2f)
                isMissed -> MindquestColors.Success.copy(alpha = 0.1f)
                isWrongSelection -> MindquestColors.Error.copy(alpha = 0.2f)
                isSelected -> accentColor.copy(alpha = 0.15f)
                else -> MindquestColors.Surface
            }

            val chipBorder = when {
                isCorrectWord -> MindquestColors.Success
                isWrongSelection -> MindquestColors.Error
                isSelected -> accentColor
                else -> MindquestColors.SurfaceVariant
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(chipBg)
                    .then(
                        if (!isConfirmed) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onToggle(option.id) }
                        else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = option.label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isCorrectWord) FontWeight.SemiBold
                    else FontWeight.Normal,
                    color = when {
                        isCorrectWord -> MindquestColors.SuccessDark
                        isWrongSelection -> MindquestColors.ErrorDark
                        isSelected -> accentColor
                        else -> MindquestColors.TextPrimary
                    },
                )
            }
        }
    }
}

// ── STATEMENT / REASON ─────────────────────────────────────────────────

@Composable
private fun StatementReasonSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Read statement & reason from metadata (JSONB) first, fallback to title parsing
    val (statementText, reasonText) = remember(question.id) {
        val meta = question.metadata
        val metaStatement = (meta?.get("statement") as? JsonElement)
            ?.jsonPrimitive?.content
        val metaReason = (meta?.get("reason") as? JsonElement)
            ?.jsonPrimitive?.content

        if (metaStatement != null && metaReason != null) {
            metaStatement to metaReason
        } else {
            // Fallback: parse "Statement: ...\nReason: ..." or ". Reason:" from title
            val title = question.title
            val nlParts = title.split("\n")
            if (nlParts.size >= 2) {
                val s = nlParts[0].removePrefix("Statement:").trim()
                val r = nlParts[1].removePrefix("Reason:").trim()
                s to r
            } else {
                // Try splitting on ". Reason:" for single-line format
                val idx = title.indexOf(". Reason:")
                if (idx > 0) {
                    val s = title.substring(0, idx).removePrefix("Statement:").trim()
                    val r = title.substring(idx + ". Reason:".length).trim()
                    s to r
                } else {
                    title.removePrefix("Statement:").trim() to ""
                }
            }
        }
    }

    // Statement card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
        border = BorderStroke(1.dp, Color(0xFF93C5FD)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(text = "\uD83D\uDCDD", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("STATEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(statementText, fontSize = 14.sp, color = MindquestColors.TextPrimary, lineHeight = 20.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Reason card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(text = "\uD83D\uDCA1", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("REASON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(reasonText, fontSize = 14.sp, color = MindquestColors.TextPrimary, lineHeight = 20.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Options as vertical full-width cards (long text, not suited for 2x2 grid)
    question.options.forEach { option ->
        val isSelected = selectedOptionId == option.id
        val state = when {
            !isConfirmed && isSelected -> OptionState.SELECTED
            isConfirmed && showFeedback && option.isCorrect -> OptionState.CORRECT
            isConfirmed && showFeedback && isSelected && !option.isCorrect -> OptionState.INCORRECT
            isConfirmed && isSelected -> OptionState.SELECTED
            else -> OptionState.DEFAULT
        }

        val bgColor = when (state) {
            OptionState.CORRECT -> MindquestColors.Success.copy(alpha = 0.12f)
            OptionState.INCORRECT -> MindquestColors.Error.copy(alpha = 0.12f)
            OptionState.SELECTED -> accentColor.copy(alpha = 0.08f)
            OptionState.DEFAULT -> MindquestColors.Surface
        }

        val borderColor = when (state) {
            OptionState.CORRECT -> MindquestColors.Success
            OptionState.INCORRECT -> MindquestColors.Error
            OptionState.SELECTED -> accentColor
            OptionState.DEFAULT -> MindquestColors.SurfaceVariant
        }

        val badgeColor = when (state) {
            OptionState.CORRECT -> MindquestColors.Success
            OptionState.INCORRECT -> MindquestColors.Error
            OptionState.SELECTED -> accentColor
            OptionState.DEFAULT -> MindquestColors.SurfaceVariant
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .then(if (!isConfirmed) Modifier.clickable { onSelect(option.id) } else Modifier),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(if (state != OptionState.DEFAULT) 2.dp else 1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        OptionState.CORRECT -> Icon(Icons.Default.Check, "Correct", tint = Color.White, modifier = Modifier.size(14.dp))
                        OptionState.INCORRECT -> Icon(Icons.Default.Close, "Incorrect", tint = Color.White, modifier = Modifier.size(14.dp))
                        else -> Text(
                            option.visualLabel ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state == OptionState.SELECTED) Color.White else MindquestColors.TextSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = option.label,
                    fontSize = 13.sp,
                    fontWeight = if (state != OptionState.DEFAULT) FontWeight.Medium else FontWeight.Normal,
                    color = MindquestColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── VISUAL SINGLE CHOICE ───────────────────────────────────────────────

@Composable
private fun VisualChoiceGrid(
    options: List<QuestionOption>,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    val rows = options.chunked(2)

    rows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { option ->
                val isSelected = selectedOptionId == option.id
                val state = when {
                    !isConfirmed && isSelected -> OptionState.SELECTED
                    isConfirmed && showFeedback && option.isCorrect -> OptionState.CORRECT
                    isConfirmed && showFeedback && isSelected && !option.isCorrect -> OptionState.INCORRECT
                    isConfirmed && isSelected -> OptionState.SELECTED
                    else -> OptionState.DEFAULT
                }

                val bgColor = when (state) {
                    OptionState.CORRECT -> MindquestColors.Success.copy(alpha = 0.12f)
                    OptionState.INCORRECT -> MindquestColors.Error.copy(alpha = 0.12f)
                    OptionState.SELECTED -> accentColor.copy(alpha = 0.08f)
                    OptionState.DEFAULT -> MindquestColors.Surface
                }

                val borderColor = when (state) {
                    OptionState.CORRECT -> MindquestColors.Success
                    OptionState.INCORRECT -> MindquestColors.Error
                    OptionState.SELECTED -> accentColor
                    OptionState.DEFAULT -> MindquestColors.SurfaceVariant
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (!isConfirmed) Modifier.clickable { onSelect(option.id) } else Modifier),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(if (state != OptionState.DEFAULT) 2.dp else 1.dp, borderColor),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Large visual — Canvas shape or emoji fallback
                        val visualText = option.visualLabel ?: "\uD83D\uDCCC"
                        val token = QuizVisualTokenMapper.resolve(visualText)
                        VisualTokenView(token = token, size = 52.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = option.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MindquestColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        // Feedback icon
                        if (state == OptionState.CORRECT || state == OptionState.INCORRECT) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Icon(
                                imageVector = if (state == OptionState.CORRECT) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (state == OptionState.CORRECT) "Correct" else "Incorrect",
                                tint = if (state == OptionState.CORRECT) MindquestColors.Success else MindquestColors.Error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ── MATRIX ──────────────────────────────────────────────────────────────

@Composable
private fun MatrixSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Parse matrix rows from promptConfig["rows"] (JSONB array of arrays)
    val matrixRows: List<List<String>> = remember(question.id) {
        val rowsElement = question.promptConfig?.get("rows")
        if (rowsElement is JsonArray) {
            rowsElement.mapNotNull { rowEl ->
                (rowEl as? JsonArray)?.map { it.jsonPrimitive.content }
            }
        } else {
            // Legacy fallback: parse from prompt text
            val prompt = question.prompt ?: ""
            val lines = prompt.lines().filter { it.isNotBlank() }
            if (lines.any { it.contains("?") || it.trim().split("\\s+".toRegex()).size > 1 }) {
                lines.map { it.trim().split("\\s+".toRegex()) }
            } else emptyList()
        }
    }

    if (matrixRows.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("\uD83D\uDD22 Number Matrix", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Spacer(modifier = Modifier.height(12.dp))

                matrixRows.forEach { cells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        cells.forEach { cell ->
                            val isMissing = cell == "?"
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isMissing) accentColor.copy(alpha = 0.15f)
                                        else Color(0xFFE2E8F0),
                                    )
                                    .then(
                                        if (isMissing) Modifier.border(2.dp, accentColor, RoundedCornerShape(8.dp))
                                        else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = cell,
                                    fontSize = if (isMissing) 22.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMissing) accentColor else MindquestColors.TextPrimary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    } else {
        FallbackPromptCard(title = question.prompt ?: question.title, emoji = "\uD83D\uDD22", label = "Number Matrix", accentColor = accentColor)
        Spacer(modifier = Modifier.height(16.dp))
    }

    McqOptionsGrid(
        options = question.options,
        selectedOptionId = selectedOptionId,
        isConfirmed = isConfirmed,
        showFeedback = showFeedback,
        accentColor = accentColor,
        onSelect = onSelect,
    )
}

// ── GRID PATTERN ────────────────────────────────────────────────────────

@Composable
private fun GridPatternSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Parse grid from promptConfig["grid"] (JSONB array of arrays)
    val gridRows: List<List<String>> = remember(question.id) {
        val gridElement = question.promptConfig?.get("grid")
        if (gridElement is JsonArray) {
            gridElement.mapNotNull { rowEl ->
                (rowEl as? JsonArray)?.map { it.jsonPrimitive.content }
            }
        } else {
            // Legacy fallback: parse from prompt text
            val prompt = question.prompt ?: ""
            val items = prompt.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (items.any { it == "?" }) listOf(items) else emptyList()
        }
    }

    if (gridRows.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "\uD83E\uDDE9 Pattern",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                )
                Spacer(modifier = Modifier.height(12.dp))

                gridRows.forEach { cells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        cells.forEach { item ->
                            val isMissing = item == "?"
                            Box(
                                modifier = Modifier
                                    .size(if (isMissing) 50.dp else 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isMissing) accentColor.copy(alpha = 0.12f)
                                        else Color(0xFFEDE9FE),
                                    )
                                    .then(
                                        if (isMissing) Modifier.border(2.dp, accentColor, RoundedCornerShape(10.dp))
                                        else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item,
                                    fontSize = if (isMissing) 22.sp else 20.sp,
                                    fontWeight = if (isMissing) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isMissing) accentColor else MindquestColors.TextPrimary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    } else {
        FallbackPromptCard(title = question.prompt ?: question.title, emoji = "\uD83E\uDDE9", label = "Pattern", accentColor = accentColor)
        Spacer(modifier = Modifier.height(16.dp))
    }

    McqOptionsGrid(
        options = question.options,
        selectedOptionId = selectedOptionId,
        isConfirmed = isConfirmed,
        showFeedback = showFeedback,
        accentColor = accentColor,
        onSelect = onSelect,
    )
}

// ── TABLE DATA ──────────────────────────────────────────────────────────

@Composable
private fun TableDataSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Parse table from promptConfig: "headers" and "rows"
    val tableData: Pair<List<String>, List<List<String>>>? = remember(question.id) {
        val cfg = question.promptConfig
        val headersEl = cfg?.get("headers")
        val rowsEl = cfg?.get("rows")
        if (headersEl is JsonArray && rowsEl is JsonArray) {
            val headers = headersEl.map { it.jsonPrimitive.content }
            val rows = rowsEl.mapNotNull { rowEl ->
                (rowEl as? JsonArray)?.map { it.jsonPrimitive.content }
            }
            headers to rows
        } else {
            // Legacy fallback: parse from prompt as pipe-delimited
            val prompt = question.prompt ?: ""
            val lines = prompt.lines().filter { it.isNotBlank() }
            if (lines.any { "|" in it }) {
                val allRows = lines.map { row ->
                    row.split("|").map { it.trim() }.filter { it.isNotBlank() }
                }
                if (allRows.size >= 2) allRows.first() to allRows.drop(1) else null
            } else null
        }
    }

    if (tableData != null) {
        val (headers, rows) = tableData

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "\uD83D\uDCCA Data Table",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    headers.forEach { cell ->
                        Text(
                            text = cell,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Data rows
                rows.forEachIndexed { index, cells ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        cells.forEach { cell ->
                            Text(
                                text = cell,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MindquestColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (index < rows.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MindquestColors.SurfaceVariant),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Show the actual question from prompt (e.g., "Who scored the highest in Math?")
        val promptQuestion = question.prompt?.takeIf { it.isNotBlank() }
        if (promptQuestion != null) {
            Text(
                text = promptQuestion,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    } else {
        FallbackPromptCard(title = question.prompt ?: question.title, emoji = "\uD83D\uDCCA", label = "Data Table", accentColor = accentColor)
        Spacer(modifier = Modifier.height(16.dp))
    }

    McqOptionsGrid(
        options = question.options,
        selectedOptionId = selectedOptionId,
        isConfirmed = isConfirmed,
        showFeedback = showFeedback,
        accentColor = accentColor,
        onSelect = onSelect,
    )
}

// ── MEMORY ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemorySection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    val memGreen = Color(0xFF10B981)

    // Each card: id, display text (visualLabel or label), and groupKey (label) for pair matching
    data class MemCard(val id: String, val display: String, val groupKey: String)

    val shuffledCards = remember(question.id) {
        question.options.map { opt ->
            MemCard(
                id = opt.id,
                display = opt.visualLabel?.takeIf { it.isNotBlank() } ?: opt.label,
                groupKey = opt.label,
            )
        }.shuffled()
    }

    // Local state for the card-flip matching game
    var revealedIds by remember { mutableStateOf(setOf<String>()) }
    var matchedIds by remember { mutableStateOf(setOf<String>()) }
    var firstPick by remember { mutableStateOf<String?>(null) }
    var lockInput by remember { mutableStateOf(false) }
    val allMatched = matchedIds.size == shuffledCards.size && shuffledCards.isNotEmpty()

    // Auto-select first option when all pairs found (enables Confirm button)
    LaunchedEffect(allMatched) {
        if (allMatched && selectedOptionId == null) {
            onSelect(question.options.first().id)
        }
    }

    // Handle mismatch — hide after brief delay
    LaunchedEffect(firstPick, revealedIds) {
        if (lockInput && firstPick != null) {
            kotlinx.coroutines.delay(600)
            val pick1 = firstPick ?: return@LaunchedEffect
            // Remove non-matched revealed cards
            revealedIds = matchedIds
            firstPick = null
            lockInput = false
        }
    }

    val pairsFound = matchedIds.size / 2
    val totalPairs = shuffledCards.size / 2

    // Status header
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, memGreen.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "\uD83E\uDDE0", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (allMatched) "All pairs found!" else "Tap cards to find matching pairs",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (allMatched) memGreen else Color(0xFF059669),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$pairsFound/$totalPairs",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = memGreen,
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Card grid (4 columns for 8 cards)
    val columns = if (shuffledCards.size <= 6) 3 else 4
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = columns,
    ) {
        shuffledCards.forEach { card ->
            val isRevealed = card.id in revealedIds
            val isMatchedCard = card.id in matchedIds

            val cardBg = when {
                isMatchedCard -> memGreen.copy(alpha = 0.15f)
                isRevealed -> accentColor.copy(alpha = 0.1f)
                else -> MindquestColors.Surface
            }
            val cardBorder = when {
                isMatchedCard -> memGreen
                isRevealed -> accentColor
                else -> MindquestColors.SurfaceVariant
            }

            Card(
                modifier = Modifier
                    .size(72.dp)
                    .then(
                        if (!isRevealed && !lockInput && !isConfirmed) {
                            Modifier.clickable {
                                if (firstPick == null) {
                                    // First card of pair
                                    firstPick = card.id
                                    revealedIds = revealedIds + card.id
                                } else if (firstPick != card.id) {
                                    // Second card — check match
                                    revealedIds = revealedIds + card.id
                                    val pick1 = shuffledCards.find { it.id == firstPick }
                                    if (pick1 != null && pick1.groupKey == card.groupKey) {
                                        // Match found!
                                        matchedIds = matchedIds + pick1.id + card.id
                                        firstPick = null
                                    } else {
                                        // Mismatch — lock briefly then hide
                                        lockInput = true
                                    }
                                }
                            }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(
                    if (isRevealed || isMatchedCard) 2.dp else 1.dp,
                    cardBorder,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRevealed || isMatchedCard || isConfirmed) {
                        // Use visual token rendering for shape:/color: prefixed values
                        val hasVisualToken = card.display.contains(":") ||
                            card.display.lowercase().let { d ->
                                d in listOf("circle", "square", "triangle", "star", "heart",
                                    "diamond", "pentagon", "hexagon", "octagon",
                                    "red", "blue", "green", "yellow", "orange", "purple", "pink")
                            }
                        if (hasVisualToken) {
                            VisualTokenFromText(
                                rawText = card.display,
                                size = 44.dp,
                            )
                        } else {
                            Text(
                                text = card.display,
                                fontSize = if (card.display.length <= 2) 28.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMatchedCard) memGreen else MindquestColors.TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            text = "?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MindquestColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

// ── FALLBACK PROMPT CARD ────────────────────────────────────────────────

@Composable
private fun FallbackPromptCard(
    title: String,
    emoji: String,
    label: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$emoji $label",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindquestColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Explanation Card ────────────────────────────────────────────────────

@Composable
private fun ExplanationCard(explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MindquestColors.SurfaceVariant.copy(alpha = 0.3f),
        ),
        border = BorderStroke(1.dp, MindquestColors.SurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDCA1", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Explanation",
                    color = MindquestColors.TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = explanation,
                color = MindquestColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

// ── GRID CELL SELECT ────────────────────────────────────────────────────

@Composable
private fun GridCellSelectSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Parse grid from promptConfig["grid"]
    val gridRows: List<List<String>> = remember(question.id) {
        val gridElement = question.promptConfig?.get("grid")
        if (gridElement is JsonArray) {
            gridElement.mapNotNull { rowEl ->
                (rowEl as? JsonArray)?.map { it.jsonPrimitive.content }
            }
        } else {
            val prompt = question.prompt ?: ""
            val items = prompt.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (items.any { it == "?" }) listOf(items) else emptyList()
        }
    }

    if (gridRows.isNotEmpty()) {
        // Find correct answer label to highlight after confirm
        val correctLabel = remember(question.id) {
            question.options.find { it.isCorrect }?.label ?: ""
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, MindquestColors.Success.copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "\uD83D\uDD22 Grid Select",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MindquestColors.Success,
                )
                Spacer(modifier = Modifier.height(12.dp))

                gridRows.forEach { cells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        cells.forEach { item ->
                            val isMissing = item == "?"
                            // After confirm, show the correct answer in the missing cell
                            val displayText = if (isMissing && isConfirmed && showFeedback) correctLabel
                            else item
                            val cellBg = when {
                                isMissing && isConfirmed && showFeedback -> MindquestColors.Success.copy(alpha = 0.2f)
                                isMissing -> accentColor.copy(alpha = 0.12f)
                                else -> Color(0xFFDCFCE7)
                            }
                            val cellBorder = when {
                                isMissing && isConfirmed && showFeedback -> MindquestColors.Success
                                isMissing -> accentColor
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .size(if (isMissing) 50.dp else 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellBg)
                                    .then(
                                        if (isMissing) Modifier.border(
                                            2.dp, cellBorder, RoundedCornerShape(10.dp),
                                        ) else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = displayText,
                                    fontSize = if (isMissing) 22.sp else 20.sp,
                                    fontWeight = if (isMissing) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isMissing && isConfirmed && showFeedback) MindquestColors.Success
                                    else if (isMissing) accentColor
                                    else MindquestColors.TextPrimary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    } else {
        FallbackPromptCard(
            title = question.prompt ?: question.title,
            emoji = "\uD83D\uDD22",
            label = "Grid Select",
            accentColor = accentColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    McqOptionsGrid(
        options = question.options,
        selectedOptionId = selectedOptionId,
        isConfirmed = isConfirmed,
        showFeedback = showFeedback,
        accentColor = accentColor,
        onSelect = onSelect,
    )
}

// ── GRID PATTERN BOOLEAN ────────────────────────────────────────────────

@Composable
private fun GridPatternBooleanSection(
    question: Question,
    selectedOptionId: String?,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    // Parse grid from promptConfig["grid"]
    val gridRows: List<List<String>> = remember(question.id) {
        val gridElement = question.promptConfig?.get("grid")
        if (gridElement is JsonArray) {
            gridElement.mapNotNull { rowEl ->
                (rowEl as? JsonArray)?.map { it.jsonPrimitive.content }
            }
        } else emptyList()
    }

    // Parse property info from metadata
    val propertyLabel = remember(question.id) {
        val meta = question.metadata
        val property = (meta?.get("property") as? JsonElement)?.jsonPrimitive?.content
            ?: (meta?.get("property") as? String)
        val axis = (meta?.get("axis") as? JsonElement)?.jsonPrimitive?.content
            ?: (meta?.get("axis") as? String)
        buildString {
            append("\uD83D\uDD04 ")
            if (property != null) append(property.replaceFirstChar { it.uppercase() })
            else append("Property")
            append(" Check")
            if (axis != null) append(" \u2022 ${axis.replaceFirstChar { it.uppercase() }} Axis")
        }
    }

    if (gridRows.isNotEmpty()) {
        // Grid visualization card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    propertyLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                )
                Spacer(modifier = Modifier.height(12.dp))

                gridRows.forEach { cells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        cells.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MindquestColors.TextPrimary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    } else {
        FallbackPromptCard(
            title = question.prompt ?: question.title,
            emoji = "\uD83D\uDD04",
            label = "Pattern Check",
            accentColor = accentColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    // True/False answer buttons
    TrueFalseOptions(
        options = question.options,
        selectedOptionId = selectedOptionId,
        isConfirmed = isConfirmed,
        showFeedback = showFeedback,
        onSelect = onSelect,
    )
}

// ── SEQUENCE TAP ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequenceTapSection(
    question: Question,
    sequenceTapIds: List<String>,
    isConfirmed: Boolean,
    showFeedback: Boolean = true,
    accentColor: Color,
    onTap: (String) -> Unit,
) {
    // Show duration from promptConfig
    val showDurationMs = remember(question.id) {
        val cfg = question.promptConfig
        val durationEl = cfg?.get("show_duration_ms")
        try {
            when (durationEl) {
                is JsonElement -> durationEl.jsonPrimitive.content.toLong()
                is Number -> durationEl.toLong()
                is String -> durationEl.toLong()
                else -> 3000L
            }
        } catch (_: Exception) { 3000L }
    }

    // Correct order for display during memorize phase
    val orderedOptions = remember(question.id) {
        question.options.sortedBy { it.correctPosition ?: it.displayOrder }
    }

    // Shuffled order for recall phase
    var shuffledOptions by remember(question.id) {
        mutableStateOf(emptyList<QuestionOption>())
    }
    var showPhase by remember(question.id) { mutableStateOf(true) }
    var countdownProgress by remember(question.id) { mutableStateOf(1f) }

    // Animated progress for countdown
    val animatedProgress = remember(question.id) { Animatable(1f) }

    LaunchedEffect(question.id) {
        showPhase = true
        countdownProgress = 1f
        // Animate countdown
        animatedProgress.snapTo(1f)
        animatedProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(showDurationMs.toInt(), easing = LinearEasing),
        )
        // Transition to recall phase
        shuffledOptions = question.options.shuffled()
        showPhase = false
    }

    if (showPhase) {
        // ── Memorize Phase ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "\uD83D\uDC41\uFE0F Memorize this sequence!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Countdown bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.value)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFF59E0B)),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    orderedOptions.forEachIndexed { index, option ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDE68A)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Position badge
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.visualLabel ?: option.label,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ── Recall Phase ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "\uD83D\uDC46 Tap in the correct order!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Correct order for feedback comparison
                val correctOrder = remember(question.id) {
                    question.options
                        .sortedBy { it.correctPosition ?: it.displayOrder }
                        .map { it.id }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    shuffledOptions.forEach { option ->
                        val tapIndex = sequenceTapIds.indexOf(option.id)
                        val isTapped = tapIndex >= 0
                        val correctPositionInSeq = correctOrder.indexOf(option.id)

                        val cardBg = when {
                            isConfirmed && showFeedback && isTapped && tapIndex == correctPositionInSeq ->
                                MindquestColors.Success.copy(alpha = 0.2f)
                            isConfirmed && showFeedback && isTapped ->
                                MindquestColors.Error.copy(alpha = 0.2f)
                            isTapped -> accentColor.copy(alpha = 0.15f)
                            else -> Color(0xFFE0F2FE)
                        }
                        val cardBorder = when {
                            isConfirmed && showFeedback && isTapped && tapIndex == correctPositionInSeq ->
                                MindquestColors.Success
                            isConfirmed && showFeedback && isTapped ->
                                MindquestColors.Error
                            isTapped -> accentColor
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .then(
                                    if (isTapped) Modifier.border(
                                        2.dp, cardBorder, RoundedCornerShape(14.dp),
                                    ) else Modifier,
                                )
                                .then(
                                    if (!isConfirmed) Modifier.clickable { onTap(option.id) }
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isTapped) {
                                    // Show tap order badge
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isConfirmed && showFeedback && tapIndex == correctPositionInSeq ->
                                                        MindquestColors.Success
                                                    isConfirmed && showFeedback ->
                                                        MindquestColors.Error
                                                    else -> accentColor
                                                },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${tapIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = option.visualLabel ?: option.label,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // Progress indicator
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${sequenceTapIds.size}/${question.options.size} tapped",
                    fontSize = 12.sp,
                    color = MindquestColors.TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color {
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
