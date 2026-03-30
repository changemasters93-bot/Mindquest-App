package com.android.mindquest.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
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
import com.android.mindquest.domain.usecase.GetQuizWithQuestionsUseCase
import com.android.mindquest.domain.usecase.SubmitQuizAttemptUseCase
import com.android.mindquest.domain.usecase.SubmitSingleAnswerUseCase
import com.android.mindquest.domain.usecase.SubmitTournamentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    val sequenceTapIds: List<String> = emptyList(),
) {
    fun hasAnswer(): Boolean {
        val q = currentQuestion ?: return false
        return when (q.questionType) {
            QuestionType.FILL_BLANK -> fillBlankAnswer.isNotBlank()
            QuestionType.ORDERING -> orderedOptionIds.isNotEmpty()
            QuestionType.MATCH -> matchedPairs.size == (q.matchPairs?.size ?: 0)
            QuestionType.SELECT_WORD -> selectedWordIds.isNotEmpty()
            QuestionType.SEQUENCE_TAP -> sequenceTapIds.size == q.options.size
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
    private val getQuizWithQuestions: GetQuizWithQuestionsUseCase,
    private val quizStateManager: QuizStateManager,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("QuizViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

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

    /** Mutex-based debounce guard: prevents double-tap on confirmAnswer(). */
    private val confirmMutex = Mutex()
    /** Cached submission data for retry on failure. */
    private var pendingSubmissionTimeTaken: Int = 0

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
     *
     * If [QuizSessionHolder.currentQuiz] has questions, starts immediately.
     * Otherwise (daily challenge, IQ test), loads the quiz from the API first.
     *
     * Guard: does nothing if quiz is already in progress or finished.
     */
    fun startFromSession(userId: String) {
        if (_questions.isNotEmpty()) return
        this._config = QuizSessionHolder.config ?: QuizConfig.module()

        val quiz = QuizSessionHolder.currentQuiz
        if (quiz != null && quiz.questions.isNotEmpty()) {
            // Quiz already has questions (module quiz from chapter list)
            startQuiz(quiz, userId)
        } else {
            // Need to load quiz from API (daily challenge, IQ test)
            val qId = quiz?.id ?: QuizSessionHolder.quizId ?: return
            viewModelScope.launch(exceptionHandler) {
                _resultState.value = UiState.Loading
                when (val result = getQuizWithQuestions(qId, userId)) {
                    is Resource.Success -> {
                        val loaded = result.data
                        if (loaded.isLocked) {
                            _resultState.value = UiState.Error("This quiz is on cooldown. Try again later.")
                        } else {
                            QuizSessionHolder.currentQuiz = loaded
                            startQuiz(loaded, userId)
                        }
                    }
                    is Resource.Error -> {
                        _resultState.value = UiState.Error(result.message)
                        snackbarManager.showError("Failed to load quiz. Please try again.")
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            }
        }
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

    fun tapSequenceItem(optionId: String) {
        if (_quizState.value.isConfirmed) return
        val current = _quizState.value.sequenceTapIds.toMutableList()
        if (optionId in current) {
            // Undo: remove this and all subsequent taps
            val idx = current.indexOf(optionId)
            current.subList(idx, current.size).clear()
        } else {
            current.add(optionId)
        }
        _quizState.value = _quizState.value.copy(sequenceTapIds = current)
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
        // Mutex tryLock: if another coroutine is already confirming, drop this tap.
        // This prevents the non-atomic check-then-act double-tap bug.
        if (!confirmMutex.tryLock()) return

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
        // Safe to unlock: isConfirmed=true now guards against re-entry
        confirmMutex.unlock()

        // Persist quiz state for process death recovery
        persistCurrentState()

        // Config: per-question submission for tournaments (non-blocking)
        if (_config.submissionMode == SubmissionMode.PER_QUESTION_NON_BLOCKING) {
            val entryId = _config.tournamentEntryId
            if (entryId != null) {
                viewModelScope.launch(exceptionHandler) {
                    submitSingleAnswer(entryId, answer) // fire-and-forget
                }
            }
        }

        // Config: feedback flash auto-advance (takes priority over autoAdvanceDelayMs)
        val flashMs = _config.feedbackFlashDurationMs
        if (flashMs != null) {
            autoAdvanceJob = viewModelScope.launch(exceptionHandler) {
                delay(flashMs)
                nextQuestion()
            }
        } else {
            // Config: auto-advance after delay (no flash)
            _config.autoAdvanceDelayMs?.let { delayMs ->
                autoAdvanceJob = viewModelScope.launch(exceptionHandler) {
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

        viewModelScope.launch(exceptionHandler) {
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
        // Persist state on pause in case process is killed while backgrounded
        persistCurrentState()
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

            QuestionType.SEQUENCE_TAP -> {
                val correctOrder = question.options
                    .sortedBy { it.correctPosition ?: it.displayOrder }
                    .map { it.id }
                Pair(
                    state.sequenceTapIds == correctOrder,
                    state.sequenceTapIds.joinToString(","),
                )
            }

            else -> {
                val selected = state.selectedOptionId ?: ""
                val correctOption = question.options.find { it.isCorrect }
                Pair(selected == correctOption?.id, selected)
            }
        }
    }

    // ── Process Death Recovery ─────────────────────────────────────────

    /**
     * Build a [PersistableQuizState] snapshot from the current in-memory state.
     * Called after each confirmed answer and on pause to persist progress.
     */
    private fun buildPersistableState(): PersistableQuizState {
        return PersistableQuizState(
            quizId = quizId,
            userId = userId,
            behaviorType = _config.behavior.name,
            currentIndex = _quizState.value.currentIndex,
            totalQuestions = _questions.size,
            timeRemainingSeconds = _timeLeft.value,
            startTimeMs = startTimeMs,
            answers = _answers.map { answer ->
                PersistableAnswer(
                    questionId = answer.questionId,
                    selected = answer.selected,
                    isCorrect = answer.isCorrect,
                    timeMs = answer.timeMs,
                )
            },
            tournamentEntryId = _config.tournamentEntryId,
            moduleColor = QuizSessionHolder.moduleColor,
            moduleEmoji = QuizSessionHolder.moduleEmoji,
            moduleTitle = QuizSessionHolder.moduleTitle,
        )
    }

    /**
     * Persist the current quiz state to disk.
     * Called after each answer confirmation and on pause.
     */
    private fun persistCurrentState() {
        if (quizId.isBlank() || _questions.isEmpty()) return
        quizStateManager.saveState(buildPersistableState())
    }

    // ── Timer ────────────────────────────────────────────────────────────

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(exceptionHandler) {
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
                    viewModelScope.launch(exceptionHandler) {
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

        // Clear persisted quiz state — quiz is complete, no recovery needed
        quizStateManager.clearState()

        val now = Clock.System.now().toEpochMilliseconds()
        pendingSubmissionTimeTaken = ((now - startTimeMs) / 1000).toInt()

        performSubmission()
    }

    /**
     * Retry a failed quiz submission.
     * Called from the UI retry button when [resultState] is [UiState.Error].
     */
    fun retrySubmission() {
        if (_resultState.value !is UiState.Error) return
        performSubmission()
    }

    /**
     * Perform the actual quiz submission. Used by both [finishQuiz] and [retrySubmission].
     * Wrapped in [NonCancellable] to prevent coroutine cancellation from losing the attempt.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun performSubmission() {
        viewModelScope.launch(exceptionHandler) {
            _resultState.value = UiState.Loading

            // Use NonCancellable to ensure submission completes even if ViewModel is cleared
            withContext(NonCancellable) {
                when (_config.behavior) {
                    QuizBehavior.MODULE, QuizBehavior.IQ_TEST -> {
                        val idempotencyKey = Uuid.random().toString()
                        val payload = QuizSubmitPayload(
                            userId = userId,
                            quizId = quizId,
                            answers = _answers.toList(),
                            timeTakenSecs = pendingSubmissionTimeTaken,
                            idempotencyKey = idempotencyKey,
                        )
                        when (val result = submitQuizAttempt(payload)) {
                            is Resource.Success -> _resultState.value = UiState.Success(result.data)
                            is Resource.Error -> {
                                _resultState.value = UiState.Error(result.message)
                                snackbarManager.showError("Failed to submit quiz. Please retry.")
                            }
                            is Resource.Loading -> { /* no-op */ }
                        }
                    }

                    QuizBehavior.TOURNAMENT -> {
                        val entryId = _config.tournamentEntryId ?: return@withContext
                        when (val result = submitTournament(entryId, _answers.toList(), pendingSubmissionTimeTaken)) {
                            is Resource.Success -> _resultState.value = UiState.Success(result.data)
                            is Resource.Error -> {
                                _resultState.value = UiState.Error(result.message)
                                snackbarManager.showError("Failed to submit tournament results.")
                            }
                            is Resource.Loading -> { /* no-op */ }
                        }
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
