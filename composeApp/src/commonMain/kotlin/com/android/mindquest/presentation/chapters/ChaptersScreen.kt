package com.android.mindquest.presentation.chapters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.ChapterState
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizState
import com.android.mindquest.presentation.components.ErrorView
import com.android.mindquest.presentation.components.LoadingView

// ── Helpers ──────────────────────────────────────────────────────────────────

internal fun parseColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return try {
        Color(("FF$clean").toLong(16))
    } catch (_: Exception) {
        Color(0xFF4F46E5)
    }
}

internal fun darkenColor(color: Color, factor: Float = 0.3f): Color {
    return Color(
        red = color.red * (1f - factor),
        green = color.green * (1f - factor),
        blue = color.blue * (1f - factor),
        alpha = color.alpha,
    )
}

// ── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun ChaptersScreen(
    moduleId: String,
    moduleTitle: String,
    moduleEmoji: String,
    moduleColor: String,
    userId: String,
    onBack: () -> Unit,
    onQuizSelect: (Quiz) -> Unit,
    viewModel: ChaptersViewModel,
) {
    val moduleState by viewModel.moduleState.collectAsState()
    val expandedChapterId by viewModel.expandedChapterId.collectAsState()
    val chapterQuizzesState by viewModel.chapterQuizzesState.collectAsState()

    val accentColor = parseColor(moduleColor)
    val accentDark = darkenColor(accentColor, 0.25f)

    LaunchedEffect(moduleId) {
        viewModel.loadModule(moduleId, userId)
    }

    when (val state = moduleState) {
        is UiState.Loading -> LoadingView(message = "Loading chapters...")
        is UiState.Error -> ErrorView(
            message = state.message,
            onRetry = { viewModel.loadModule(moduleId, userId) },
        )
        is UiState.Empty -> ErrorView(
            message = "No chapters found",
            onRetry = { viewModel.loadModule(moduleId, userId) },
        )
        is UiState.Offline -> ErrorView(
            message = "You are offline. Please check your connection.",
            onRetry = { viewModel.loadModule(moduleId, userId) },
        )
        is UiState.Success -> {
            val (module, chapters) = state.data
            ChaptersContent(
                module = module,
                chapters = chapters,
                accentColor = accentColor,
                accentDark = accentDark,
                moduleEmoji = moduleEmoji,
                expandedChapterId = expandedChapterId,
                chapterQuizzesState = chapterQuizzesState,
                onBack = onBack,
                onChapterClick = { chapterId ->
                    viewModel.expandChapter(chapterId, userId)
                },
                onQuizSelect = onQuizSelect,
            )
        }
    }
}

// ── Content ──────────────────────────────────────────────────────────────────

@Composable
private fun ChaptersContent(
    module: Module,
    chapters: List<Chapter>,
    accentColor: Color,
    accentDark: Color,
    moduleEmoji: String,
    expandedChapterId: String?,
    chapterQuizzesState: UiState<List<Quiz>>,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onQuizSelect: (Quiz) -> Unit,
) {
    val totalChapters = chapters.size
    val completedChapters = chapters.count { it.state == ChapterState.COMPLETED }
    val overallProgress = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MindquestColors.Background),
    ) {
        // ── Hero Header ──────────────────────────────────────────────────
        item {
            HeroHeader(
                module = module,
                accentColor = accentColor,
                accentDark = accentDark,
                moduleEmoji = moduleEmoji,
                completedChapters = completedChapters,
                totalChapters = totalChapters,
                overallProgress = overallProgress,
                onBack = onBack,
            )
        }

        // ── Chapter Cards ────────────────────────────────────────────────
        itemsIndexed(chapters) { index, chapter ->
            val isExpanded = expandedChapterId == chapter.id

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300 + index * 80)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400 + index * 80),
                    ),
            ) {
                ChapterAccordionCard(
                    chapter = chapter,
                    chapterIndex = index,
                    isExpanded = isExpanded,
                    accentColor = accentColor,
                    accentDark = accentDark,
                    moduleEmoji = moduleEmoji,
                    chapterQuizzesState = chapterQuizzesState,
                    onChapterClick = { onChapterClick(chapter.id) },
                    onQuizSelect = onQuizSelect,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ── Hero Header ──────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    module: Module,
    accentColor: Color,
    accentDark: Color,
    moduleEmoji: String,
    completedChapters: Int,
    totalChapters: Int,
    overallProgress: Float,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(accentColor, accentDark),
                ),
            )
            .statusBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button
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
                        text = module.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Grade 5",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                // XP badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MindquestColors.XpGradientStart,
                                    MindquestColors.XpGradientEnd,
                                ),
                            ),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "\u26A1 XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall progress card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(16.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Overall Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(overallProgress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(overallProgress * 100).toInt()}% complete \u00B7 ${totalChapters - completedChapters} chapters remaining",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

// ── Chapter Accordion Card ───────────────────────────────────────────────────

@Composable
private fun ChapterAccordionCard(
    chapter: Chapter,
    chapterIndex: Int,
    isExpanded: Boolean,
    accentColor: Color,
    accentDark: Color,
    moduleEmoji: String,
    chapterQuizzesState: UiState<List<Quiz>>,
    onChapterClick: () -> Unit,
    onQuizSelect: (Quiz) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLocked = chapter.state == ChapterState.LOCKED
    val isCompleted = chapter.state == ChapterState.COMPLETED
    val progress = chapter.progress
    val quizzesDone = progress?.quizzesDone ?: 0
    val totalQuizzes = progress?.totalQuizzes ?: chapter.quizCount
    val chapterProgress = if (totalQuizzes > 0) quizzesDone.toFloat() / totalQuizzes else 0f

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron_rotation",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isExpanded) Modifier.border(
                    2.dp,
                    accentColor,
                    RoundedCornerShape(20.dp),
                ) else Modifier.border(
                    1.5f.dp,
                    Color(0xFFF3F4F6),
                    RoundedCornerShape(20.dp),
                ),
            )
            .shadow(
                elevation = if (isExpanded) 8.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isExpanded) accentColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFFF9FAFB) else Color.White,
        ),
    ) {
        Column {
            // ── Chapter Header (tappable) ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isLocked) Modifier.clickable(onClick = onChapterClick)
                        else Modifier
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isExpanded && !isLocked) Modifier.border(
                                1.5f.dp,
                                accentColor.copy(alpha = 0.27f),
                                RoundedCornerShape(14.dp),
                            ) else Modifier,
                        )
                        .background(
                            when {
                                isLocked -> Color(0xFFF3F4F6)
                                isCompleted -> MindquestColors.SuccessContainer
                                isExpanded -> accentColor.copy(alpha = 0.08f)
                                else -> Color(0xFFF9FAFB)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLocked) {
                        Text(
                            text = "\uD83D\uDD12",
                            fontSize = 18.sp,
                        )
                    } else if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MindquestColors.Success,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            text = moduleEmoji,
                            fontSize = 22.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Chapter ${chapter.chapterNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLocked) Color(0xFF9CA3AF)
                            else Color(0xFF111827),
                        )

                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MindquestColors.SuccessContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "\u2713 Done",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = chapter.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLocked) Color(0xFFC4C9D4)
                        else Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mini progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isLocked) Color(0xFFE5E7EB)
                                else accentColor.copy(alpha = 0.12f),
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(chapterProgress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isCompleted) MindquestColors.Success
                                    else accentColor,
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$quizzesDone/$totalQuizzes quizzes completed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MindquestColors.TextTertiary,
                    )
                }

                if (!isLocked) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(chevronRotation),
                        tint = if (isExpanded) accentColor else Color(0xFF9CA3AF),
                    )
                }
            }

            // ── Expanded Quiz List ───────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded && !isLocked,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(150)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Accent divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(accentColor.copy(alpha = 0.09f)),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFB))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    when (chapterQuizzesState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = accentColor,
                                    strokeWidth = 3.dp,
                                )
                            }
                        }
                        is UiState.Success -> {
                            val quizzes = chapterQuizzesState.data
                            quizzes.forEachIndexed { qIndex, quiz ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            delayMillis = qIndex * 60,
                                        ),
                                    ) + fadeIn(
                                        tween(
                                            durationMillis = 200,
                                            delayMillis = qIndex * 60,
                                        ),
                                    ),
                                ) {
                                    QuizItem(
                                        quiz = quiz,
                                        quizIndex = qIndex,
                                        accentColor = accentColor,
                                        accentDark = accentDark,
                                        onClick = { onQuizSelect(quiz) },
                                    )
                                }
                                if (qIndex < quizzes.lastIndex) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = "Failed to load quizzes",
                                fontSize = 13.sp,
                                color = MindquestColors.Error,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        else -> { /* Empty / Offline - no-op */ }
                    }
                }
            }
        }
    }
}

// ── Quiz Item ────────────────────────────────────────────────────────────────

@Composable
private fun QuizItem(
    quiz: Quiz,
    quizIndex: Int,
    accentColor: Color,
    accentDark: Color,
    onClick: () -> Unit,
) {
    when (quiz.state) {
        QuizState.COMPLETED -> CompletedQuizItem(quiz = quiz, quizIndex = quizIndex, accentColor = accentColor, onClick = onClick)
        QuizState.UNLOCKED -> AvailableQuizItem(
            quiz = quiz,
            quizIndex = quizIndex,
            accentColor = accentColor,
            accentDark = accentDark,
            onClick = onClick,
        )
        QuizState.LOCKED -> LockedQuizItem(quiz = quiz, quizIndex = quizIndex)
    }
}

@Composable
private fun CompletedQuizItem(
    quiz: Quiz,
    quizIndex: Int,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.06f))
            .border(1.dp, accentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkmark bubble
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u2713",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Quiz ${quizIndex + 1}: ${quiz.title}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MindquestColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (quiz.bestScore != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Score: ${quiz.bestScore}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }
        }

        // Small check circle on the right
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Review",
                tint = accentColor,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun AvailableQuizItem(
    quiz: Quiz,
    quizIndex: Int,
    accentColor: Color,
    accentDark: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(listOf(accentColor, accentDark)),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${quizIndex + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quiz ${quizIndex + 1}: ${quiz.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${quiz.questionCount} questions \u00B7 Up to ${quiz.maxXp} XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Start Quiz",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quiz info chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuizInfoChip(emoji = "\uD83D\uDCDD", text = "${quiz.questionCount} Qs")
            QuizInfoChip(emoji = "\u26A1", text = "${quiz.maxXp} XP")
            QuizInfoChip(emoji = "\u23F1\uFE0F", text = "${quiz.timeLimitSeconds / 60}m")
        }
    }
}

@Composable
private fun QuizInfoChip(emoji: String, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "$emoji $text",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun LockedQuizItem(
    quiz: Quiz,
    quizIndex: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF9FAFB))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lock bubble
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDD12",
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Quiz ${quizIndex + 1}: ${quiz.title}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Complete previous quiz to unlock",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD1D5DB),
            )
        }
    }
}
