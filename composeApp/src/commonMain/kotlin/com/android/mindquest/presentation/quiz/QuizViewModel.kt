package com.android.mindquest.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.NudgeHintType
import com.android.mindquest.domain.model.NudgeState
import com.android.mindquest.domain.model.Question
import com.android.mindquest.domain.model.QuestionType
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizBehavior
import com.android.mindquest.domain.model.QuizConfig
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.QuizSubmitPayload
import com.android.mindquest.domain.model.SubmissionMode
import com.android.mindquest.domain.usecase.SubmitQuizAttemptUseCase
import com.android.mindquest.domain.usecase.SubmitSingleAnswerUseCase
import com.android.mindquest.domain.usecase.SubmitTournamentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Quiz play state — drives the UI for all quiz types.
 *
 * [isCorrect] is null when feedback is disabled (IQ/Tournament modes),
 * which tells the UI to skip green/red highlighting.
 */
data class QuizPlayState(
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: Question? = null,
    val selectedOptionId: String? = null,
    val isConfirmed: Boolean = false,
    val isCorrect: Boolean? = null,
    val fillBlankAnswer: String = "",
    val orderedOptionIds: List<String> = emptyList(),
    val matchedPairs: Map<String, String> = emptyMap(),
    val selectedMatchLeft: String? = null,
    val selectedWordIds: Set<String> = emptySet(),
) {
    fun hasAnswer(): Boolean {
        val q = currentQuestion ?: return false
        return when (q.questionType) {
            QuestionType.FILL_BLANK -> fillBlankAnswer.isNotBlank()
            QuestionType.ORDERING -> orderedOptionIds.isNotEmpty()
            QuestionType.MATCH -> matchedPairs.size == (q.matchPairs?.size ?: 0)
            QuestionType.SELECT_WORD -> selectedWordIds.isNotEmpty()
            else -> selectedOptionId != null
        }
    }
}

/**
 * Unified ViewModel that drives all quiz types (Module, IQ, Tournament).
 *
 * Behavior is controlled by [QuizConfig] read from [QuizSessionHolder].
 * The config determines:
 * - Whether answer feedback is shown (green/red highlighting)
 * - Whether explanations are displayed
 * - How answers are submitted (batch vs per-question)
 * - Whether nudge hints appear on wrong answers
 * - Whether auto-advance is enabled
 * - Whether pause/resume is supported
 *
 * @param submitQuizAttempt  Batch submission for MODULE / IQ_TEST modes
 * @param submitTournament   Batch submission for TOURNAMENT mode (end of quiz)
 * @param submitSingleAnswer Per-question submission for TOURNAMENT mode (non-blocking)
 */
class QuizViewModel(
    private val submitQuizAttempt: SubmitQuizAttemptUseCase,
    private val submitTournament: SubmitTournamentUseCase,
    private val submitSingleAnswer: SubmitSingleAnswerUseCase,
) : ViewModel() {

    private val _quizState = MutableStateFlow(QuizPlayState())
    val quizState: StateFlow<QuizPlayState> = _quizState.asStateFlow()

    private val _resultState = MutableStateFlow<UiState<QuizResult>>(UiState.Loading)
    val resultState: StateFlow<UiState<QuizResult>> = _resultState.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _nudgeState = MutableStateFlow<NudgeState?>(null)
    val nudgeState: StateFlow<NudgeState?> = _nudgeState.asStateFlow()

    private val _answers = mutableListOf<QuizAnswer>()
    val answers: List<QuizAnswer> get() = _answers.toList()

    private var _questions: List<Question> = emptyList()
    val questions: List<Question> get() = _questions

    /** Current quiz config — drives all behavioral differences */
    private var _config: QuizConfig = QuizConfig.module()
    val quizConfig: QuizConfig get() = _config

    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var quizId: String = ""
    private var userId: String = ""
    private var startTimeMs: Long = 0L
    private var questionStartMs: Long = 0L

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Initialize and start a quiz with the given config.
     * Called directly or via [startFromSession].
     */
    fun startQuiz(quiz: Quiz, userId: String) {
        this._questions = quiz.questions
        this.quizId = quiz.id
        this.userId = userId
        this.startTimeMs = Clock.System.now().toEpochMilliseconds()
        this.questionStartMs = startTimeMs
        this._answers.clear()
        this._isPaused.value = false
        this._nudgeState.value = null

        _quizState.value = buildQuestionState(_questions.firstOrNull(), 0)
        _timeLeft.value = quiz.timeLimitSeconds
        _resultState.value = UiState.Loading
        startTimer(quiz.timeLimitSeconds)
    }

    /**
     * Start quiz from [QuizSessionHolder] (called via LaunchedEffect on screen entry).
     * Reads the quiz data AND the [QuizConfig] from the session holder.
     * Guard: does nothing if quiz is already in progress or finished.
     */
    fun startFromSession(userId: String) {
        if (_questions.isNotEmpty()) return
        val quiz = QuizSessionHolder.currentQuiz ?: return
        this._config = QuizSessionHolder.config ?: QuizConfig.module()
        startQuiz(quiz, userId)
    }

    fun selectOption(optionId: String) {
        if (_quizState.value.isConfirmed) return
        _quizState.value = _quizState.value.copy(selectedOptionId = optionId)
    }

    fun setFillBlankAnswer(text: String) {
        if (_quizState.value.isConfirmed) return
        _quizState.value = _quizState.value.copy(fillBlankAnswer = text)
    }

    fun moveOrderItem(fromIndex: Int, toIndex: Int) {
        if (_quizState.value.isConfirmed) return
        val current = _quizState.value.orderedOptionIds.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _quizState.value = _quizState.value.copy(orderedOptionIds = current)
    }

    fun selectMatchLeft(matchPairId: String) {
        if (_quizState.value.isConfirmed) return
        _quizState.value = _quizState.value.copy(selectedMatchLeft = matchPairId)
    }

    fun selectMatchRight(rightText: String) {
        if (_quizState.value.isConfirmed) return
        val leftId = _quizState.value.selectedMatchLeft ?: return
        val current = _quizState.value.matchedPairs.toMutableMap()
        current[leftId] = rightText
        _quizState.value = _quizState.value.copy(
            matchedPairs = current,
            selectedMatchLeft = null,
        )
    }

    fun toggleWordSelection(optionId: String) {
        if (_quizState.value.isConfirmed) return
        val current = _quizState.value.selectedWordIds.toMutableSet()
        if (optionId in current) current.remove(optionId) else current.add(optionId)
        _quizState.value = _quizState.value.copy(selectedWordIds = current)
    }

    /**
     * Lock in the current answer.
     *
     * Config-driven behavior:
     * - [QuizConfig.showAnswerFeedback] → sets isCorrect (null hides feedback)
     * - [QuizConfig.submissionMode] PER_QUESTION → fire-and-forget submit
     * - [QuizConfig.autoAdvanceDelayMs] → auto-advance after delay
     * - [QuizConfig.enableNudges] → show nudge hint on wrong answer
     */
    fun confirmAnswer() {
        val state = _quizState.value
        if (state.isConfirmed) return
        val question = state.currentQuestion ?: return
        if (!state.hasAnswer()) return

        val now = Clock.System.now().toEpochMilliseconds()
        val (isCorrect, selectedAnswer) = evaluateAnswer(question, state)

        val answer = QuizAnswer(
            questionId = question.id,
            selected = selectedAnswer,
            isCorrect = isCorrect,
            timeMs = now - questionStartMs,
        )
        _answers.add(answer)

        // Config: show or hide feedback
        // - showAnswerFeedback → persistent green/red (user clicks Next manually)
        // - feedbackFlashDurationMs → brief green/red flash, then auto-advance
        // - neither → no highlighting at all
        val showFeedbackResult = _config.showAnswerFeedback || _config.feedbackFlashDurationMs != null
        _quizState.value = state.copy(
            isConfirmed = true,
            isCorrect = if (showFeedbackResult) isCorrect else null,
        )

        // Config: per-question submission for tournaments (non-blocking)
        if (_config.submissionMode == SubmissionMode.PER_QUESTION_NON_BLOCKING) {
            val entryId = _config.tournamentEntryId
            if (entryId != null) {
                viewModelScope.launch {
                    submitSingleAnswer(entryId, answer) // fire-and-forget
                }
            }
        }

        // Config: feedback flash auto-advance (takes priority over autoAdvanceDelayMs)
        val flashMs = _config.feedbackFlashDurationMs
        if (flashMs != null) {
            autoAdvanceJob = viewModelScope.launch {
                delay(flashMs)
                nextQuestion()
            }
        } else {
            // Config: auto-advance after delay (no flash)
            _config.autoAdvanceDelayMs?.let { delayMs ->
                autoAdvanceJob = viewModelScope.launch {
                    delay(delayMs)
                    nextQuestion()
                }
            }
        }

        // Config: nudge system for module quizzes
        _nudgeState.value = null // clear previous nudge
        if (_config.enableNudges && !isCorrect) {
            generateNudge(question, state)
        }
    }

    fun nextQuestion() {
        autoAdvanceJob?.cancel()
        _nudgeState.value = null

        val nextIdx = _quizState.value.currentIndex + 1
        if (nextIdx >= _questions.size) {
            _quizState.value = _quizState.value.copy(currentIndex = nextIdx, currentQuestion = null)
            finishQuiz()
            return
        }

        // Brief null-question state forces the UI to unmount the question content,
        // guaranteeing no stale selection/feedback leaks into the next question.
        _quizState.value = QuizPlayState(
            currentIndex = nextIdx,
            totalQuestions = _questions.size,
            currentQuestion = null,
        )

        viewModelScope.launch {
            // Yield so Compose can process the null state (hides question UI),
            // then emit the actual next question on the following frame.
            delay(30)
            questionStartMs = Clock.System.now().toEpochMilliseconds()
            _quizState.value = buildQuestionState(_questions[nextIdx], nextIdx)
        }
    }

    // ── Pause / Resume ──────────────────────────────────────────────────

    /**
     * Pause the quiz (only if [QuizConfig.allowPause] is true).
     * Stops timer and any auto-advance job.
     */
    fun pauseQuiz() {
        if (!_config.allowPause) return
        _isPaused.value = true
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
    }

    /**
     * Resume the quiz from pause state.
     * Restarts the timer from the current remaining time.
     */
    fun resumeQuiz() {
        _isPaused.value = false
        startTimer(_timeLeft.value)
    }

    // ── Nudge System (Module quiz only) ─────────────────────────────────

    /**
     * Generate a contextual nudge hint after a wrong answer.
     * Nudge types cycle through: encouragement → elimination → category.
     */
    private fun generateNudge(question: Question, state: QuizPlayState) {
        val wrongCount = _answers.count { !it.isCorrect }

        val nudge = when {
            // First wrong answer: generic encouragement
            wrongCount <= 1 -> NudgeState(
                message = "Don't worry! Take your time on the next one.",
                hintType = NudgeHintType.ENCOURAGEMENT,
            )
            // Second wrong: elimination hint
            wrongCount == 2 -> {
                val wrongOption = question.options
                    .filter { !it.isCorrect && it.id != state.selectedOptionId }
                    .firstOrNull()
                if (wrongOption != null) {
                    NudgeState(
                        message = "Hint: \"${wrongOption.label}\" is also not the answer.",
                        hintType = NudgeHintType.ELIMINATION_HINT,
                    )
                } else {
                    NudgeState(
                        message = "You're getting closer! Keep going!",
                        hintType = NudgeHintType.ENCOURAGEMENT,
                    )
                }
            }
            // Third+: category/topic hint
            else -> NudgeState(
                message = "Tip: Read the question carefully and eliminate unlikely options.",
                hintType = NudgeHintType.CATEGORY_HINT,
            )
        }
        _nudgeState.value = nudge
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun buildQuestionState(question: Question?, index: Int): QuizPlayState {
        return QuizPlayState(
            currentIndex = index,
            totalQuestions = _questions.size,
            currentQuestion = question,
            orderedOptionIds = if (question?.questionType == QuestionType.ORDERING) {
                question.options.sortedBy { it.displayOrder }.map { it.id }.shuffled()
            } else emptyList(),
        )
    }

    private fun evaluateAnswer(
        question: Question,
        state: QuizPlayState,
    ): Pair<Boolean, String> {
        return when (question.questionType) {
            QuestionType.FILL_BLANK -> {
                val answer = state.fillBlankAnswer.trim()
                val correctLabel = question.options.find { it.isCorrect }?.label ?: ""
                Pair(answer.equals(correctLabel, ignoreCase = true), answer)
            }

            QuestionType.ORDERING -> {
                val correctOrder = question.options
                    .sortedBy { it.correctPosition ?: it.displayOrder }
                    .map { it.id }
                Pair(
                    state.orderedOptionIds == correctOrder,
                    state.orderedOptionIds.joinToString(","),
                )
            }

            QuestionType.MATCH -> {
                val allCorrect = state.matchedPairs.all { (leftId, rightText) ->
                    question.matchPairs?.find { it.id == leftId }?.rightText == rightText
                }
                Pair(
                    allCorrect,
                    state.matchedPairs.entries.joinToString(",") { "${it.key}:${it.value}" },
                )
            }

            QuestionType.SELECT_WORD -> {
                val correctIds = question.options
                    .filter { it.isCorrect }
                    .map { it.id }
                    .toSet()
                Pair(
                    state.selectedWordIds == correctIds,
                    state.selectedWordIds.joinToString(","),
                )
            }

            else -> {
                val selected = state.selectedOptionId ?: ""
                val correctOption = question.options.find { it.isCorrect }
                Pair(selected == correctOption?.id, selected)
            }
        }
    }

    // ── Timer ────────────────────────────────────────────────────────────

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _timeLeft.value = remaining
            }
            autoSubmit()
        }
    }

    private fun autoSubmit() {
        val state = _quizState.value
        if (!state.isConfirmed && state.currentQuestion != null && state.hasAnswer()) {
            val (isCorrect, selectedAnswer) = evaluateAnswer(state.currentQuestion, state)
            val answer = QuizAnswer(
                questionId = state.currentQuestion.id,
                selected = selectedAnswer,
                isCorrect = isCorrect,
                timeMs = 0L,
            )
            _answers.add(answer)

            // Per-question submit for tournament even on auto-submit
            if (_config.submissionMode == SubmissionMode.PER_QUESTION_NON_BLOCKING) {
                val entryId = _config.tournamentEntryId
                if (entryId != null) {
                    viewModelScope.launch {
                        submitSingleAnswer(entryId, answer)
                    }
                }
            }
        }
        // Mark quiz as finished
        _quizState.value = _quizState.value.copy(
            currentIndex = _questions.size,
            currentQuestion = null,
        )
        finishQuiz()
    }

    // ── Submission ───────────────────────────────────────────────────────

    /**
     * Submit the completed quiz.
     *
     * Routes to the appropriate backend based on [QuizConfig.behavior]:
     * - MODULE / IQ_TEST → [SubmitQuizAttemptUseCase] (batch)
     * - TOURNAMENT → [SubmitTournamentUseCase] (batch as authoritative record)
     *
     * Also saves completed data to [QuizSessionHolder] for the review screen.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun finishQuiz() {
        timerJob?.cancel()
        autoAdvanceJob?.cancel()

        // Save completed data for review screen
        QuizSessionHolder.saveCompletedData(_questions, _answers.toList())

        val now = Clock.System.now().toEpochMilliseconds()
        val timeTaken = ((now - startTimeMs) / 1000).toInt()

        viewModelScope.launch {
            _resultState.value = UiState.Loading

            when (_config.behavior) {
                QuizBehavior.MODULE, QuizBehavior.IQ_TEST -> {
                    val idempotencyKey = Uuid.random().toString()
                    val payload = QuizSubmitPayload(
                        userId = userId,
                        quizId = quizId,
                        answers = _answers.toList(),
                        timeTakenSecs = timeTaken,
                        idempotencyKey = idempotencyKey,
                    )
                    when (val result = submitQuizAttempt(payload)) {
                        is Resource.Success -> _resultState.value = UiState.Success(result.data)
                        is Resource.Error -> _resultState.value = UiState.Error(result.message)
                        is Resource.Loading -> { /* no-op */ }
                    }
                }

                QuizBehavior.TOURNAMENT -> {
                    val entryId = _config.tournamentEntryId ?: return@launch
                    when (val result = submitTournament(entryId, _answers.toList(), timeTaken)) {
                        is Resource.Success -> _resultState.value = UiState.Success(result.data)
                        is Resource.Error -> _resultState.value = UiState.Error(result.message)
                        is Resource.Loading -> { /* no-op */ }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        super.onCleared()
    }
}
