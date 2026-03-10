package com.android.mindquest.presentation.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.model.TournamentEntryStatus
import com.android.mindquest.domain.usecase.GetActiveTournamentUseCase
import com.android.mindquest.domain.usecase.StartTournamentUseCase
import com.android.mindquest.domain.usecase.SubmitTournamentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentPlayState(
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val questionText: String = "",
    val options: List<TournamentOptionState> = emptyList(),
    val selectedOptionId: String? = null,
    val isConfirmed: Boolean = false,
    val isCorrect: Boolean? = null,
    val correctOptionId: String? = null,
    val questionsAnswered: Int = 0,
    val score: Int = 0,
    val isFinished: Boolean = false
)

data class TournamentOptionState(
    val id: String,
    val text: String,
    val isSelected: Boolean = false,
    val isCorrect: Boolean? = null,
    val isRevealed: Boolean = false
)

class TournamentViewModel(
    private val getActiveTournamentUseCase: GetActiveTournamentUseCase,
    private val startTournamentUseCase: StartTournamentUseCase,
    private val submitTournamentUseCase: SubmitTournamentUseCase
) : ViewModel() {

    private val _tournamentState = MutableStateFlow<UiState<Tournament>>(UiState.Loading)
    val tournamentState: StateFlow<UiState<Tournament>> = _tournamentState.asStateFlow()

    private val _entryState = MutableStateFlow<UiState<TournamentEntry>>(UiState.Empty)
    val entryState: StateFlow<UiState<TournamentEntry>> = _entryState.asStateFlow()

    private val _playState = MutableStateFlow(TournamentPlayState())
    val playState: StateFlow<TournamentPlayState> = _playState.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    /** Quiz built from tournament questions after [startTournament] succeeds. */
    private val _tournamentQuiz = MutableStateFlow<Quiz?>(null)
    val tournamentQuiz: StateFlow<Quiz?> = _tournamentQuiz.asStateFlow()

    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null

    private val answers = mutableListOf<QuizAnswer>()
    private var currentTournamentId: String? = null
    private var currentUserId: String? = null
    private var currentEntryId: String? = null
    private var startTimeSeconds: Int = 0

    fun loadTournament(userId: String, gradeId: String = "default") {
        currentUserId = userId
        viewModelScope.launch {
            _tournamentState.value = UiState.Loading
            when (val resource = getActiveTournamentUseCase(userId, gradeId)) {
                is Resource.Success -> {
                    val tournament = resource.data
                    if (tournament != null) {
                        currentTournamentId = tournament.id
                        _tournamentState.value = UiState.Success(tournament)
                    } else {
                        _tournamentState.value = UiState.Empty
                    }
                }
                is Resource.Error -> {
                    _tournamentState.value = UiState.Error(resource.message)
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun startTournament(userId: String, tournamentId: String) {
        currentUserId = userId
        currentTournamentId = tournamentId
        answers.clear()

        viewModelScope.launch {
            _entryState.value = UiState.Loading
            when (val resource = startTournamentUseCase(userId, tournamentId)) {
                is Resource.Success -> {
                    val entry = resource.data
                    currentEntryId = entry.id
                    startTimeSeconds = entry.timeRemainingSeconds ?: 0
                    _entryState.value = UiState.Success(entry)

                    // Build quiz from tournament questions for use with QuizPlayScreen
                    if (AppConstants.USE_MOCK_DATA) {
                        _tournamentQuiz.value = MockDataSource.mockTournamentQuiz()
                    }
                    // For real API: questions come from TournamentStartResponseDto
                    // and would be mapped here (future implementation).

                    initializePlay(entry)
                    startTimer(entry.timeRemainingSeconds ?: 0)
                }
                is Resource.Error -> {
                    _entryState.value = UiState.Error(resource.message)
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    private fun initializePlay(entry: TournamentEntry) {
        val tournament = (_tournamentState.value as? UiState.Success)?.data ?: return
        _playState.update {
            it.copy(
                currentQuestionIndex = 0,
                totalQuestions = tournament.questionCount,
                questionsAnswered = entry.questionsAnswered ?: 0,
                score = entry.score ?: 0,
                isFinished = false
            )
        }
        loadCurrentQuestion()
    }

    private fun loadCurrentQuestion() {
        val entry = (_entryState.value as? UiState.Success)?.data ?: return
        // Question data is expected to be provided via the entry or fetched separately.
        // For this implementation, we update the play state to reflect the current index.
        _playState.update {
            it.copy(
                selectedOptionId = null,
                isConfirmed = false,
                isCorrect = null,
                correctOptionId = null
            )
        }
    }

    fun selectOption(optionId: String) {
        if (_playState.value.isConfirmed || _isPaused.value) return

        _playState.update { state ->
            state.copy(
                selectedOptionId = optionId,
                options = state.options.map { option ->
                    option.copy(isSelected = option.id == optionId)
                }
            )
        }
    }

    fun confirmAnswer() {
        val state = _playState.value
        if (state.isConfirmed || state.selectedOptionId == null || _isPaused.value) return

        val selectedOptionId = state.selectedOptionId
        val correctOption = state.options.find { it.isCorrect == true }
        val isCorrect = selectedOptionId == correctOption?.id

        val answer = QuizAnswer(
            questionId = "",
            selected = selectedOptionId,
            isCorrect = isCorrect,
            timeMs = 0L,
        )
        answers.add(answer)

        _playState.update { current ->
            current.copy(
                isConfirmed = true,
                isCorrect = isCorrect,
                correctOptionId = correctOption?.id,
                questionsAnswered = current.questionsAnswered + 1,
                score = if (isCorrect) current.score + 1 else current.score,
                options = current.options.map { option ->
                    option.copy(
                        isRevealed = true,
                        isCorrect = option.id == correctOption?.id
                    )
                }
            )
        }

        // Auto-advance after 1.5s delay
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(1500L)
            if (!_isPaused.value) {
                nextQuestion()
            }
        }
    }

    fun nextQuestion() {
        val state = _playState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex >= state.totalQuestions) {
            _playState.update { it.copy(isFinished = true) }
            submitTournament()
            return
        }

        _playState.update {
            it.copy(
                currentQuestionIndex = nextIndex,
                selectedOptionId = null,
                isConfirmed = false,
                isCorrect = null,
                correctOptionId = null
            )
        }
        loadCurrentQuestion()
    }

    fun pauseTournament() {
        _isPaused.value = true
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
    }

    fun resumeTournament() {
        _isPaused.value = false
        startTimer(_timeLeft.value)
    }

    /**
     * Resume an in-progress tournament entry.
     * Loads the existing entry, builds a quiz skipping already-answered questions,
     * and starts the timer from the remaining time.
     *
     * The lobby's `LaunchedEffect(entryState, tournamentQuiz)` will fire
     * `onStartQuiz` once both are set — same pattern as [startTournament].
     */
    fun resumeTournamentEntry(userId: String, tournamentId: String) {
        currentUserId = userId
        currentTournamentId = tournamentId
        answers.clear()

        viewModelScope.launch {
            _entryState.value = UiState.Loading
            if (AppConstants.USE_MOCK_DATA) {
                val entry = MockDataSource.mockResumeEntry()
                currentEntryId = entry.id
                startTimeSeconds = entry.timeRemainingSeconds ?: 0

                _entryState.value = UiState.Success(entry)

                // Build quiz with only the remaining (unanswered) questions
                // and set timeLimitSeconds to the remaining time so QuizViewModel's
                // timer starts from where the user left off.
                val fullQuiz = MockDataSource.mockTournamentQuiz()
                val remainingQuestions = fullQuiz.questions.drop(entry.questionsAnswered)
                val resumeQuiz = fullQuiz.copy(
                    questions = remainingQuestions,
                    questionCount = remainingQuestions.size,
                    timeLimitSeconds = entry.timeRemainingSeconds ?: fullQuiz.timeLimitSeconds,
                )
                _tournamentQuiz.value = resumeQuiz

                // Set play state preserving existing progress
                _playState.update {
                    it.copy(
                        currentQuestionIndex = 0,
                        totalQuestions = fullQuiz.questionCount,
                        questionsAnswered = entry.questionsAnswered,
                        score = entry.score,
                        isFinished = false,
                    )
                }
                startTimer(entry.timeRemainingSeconds ?: 0)
            }
            // Real API: call repository.resumeEntry(entryId) → questions + remaining time
        }
    }

    /**
     * Load tournament result data for direct navigation (from home banner "View Results").
     * Sets [entryState] and [playState] so [TournamentResultScreen] can render.
     */
    fun loadTournamentResult(userId: String, gradeId: String = "default") {
        viewModelScope.launch {
            _tournamentState.value = UiState.Loading
            _entryState.value = UiState.Loading
            when (val resource = getActiveTournamentUseCase(userId, gradeId)) {
                is Resource.Success -> {
                    val tournament = resource.data
                    if (tournament != null) {
                        _tournamentState.value = UiState.Success(tournament)
                        if (AppConstants.USE_MOCK_DATA) {
                            val entry = MockDataSource.mockCompletedTournamentEntry()
                            _entryState.value = UiState.Success(entry)
                            _playState.update {
                                it.copy(
                                    totalQuestions = tournament.questionCount,
                                    questionsAnswered = entry.questionsAnswered,
                                    score = entry.score,
                                    isFinished = true,
                                )
                            }
                        }
                        // Real API: call repository.getEntry(userId, tournamentId)
                    } else {
                        _tournamentState.value = UiState.Empty
                        _entryState.value = UiState.Empty
                    }
                }
                is Resource.Error -> {
                    _tournamentState.value = UiState.Error(resource.message)
                    _entryState.value = UiState.Error(resource.message)
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun submitTournament() {
        val entryId = currentEntryId ?: return

        timerJob?.cancel()
        autoAdvanceJob?.cancel()

        val timeTaken = startTimeSeconds - _timeLeft.value

        viewModelScope.launch {
            when (val resource = submitTournamentUseCase(entryId, answers.toList(), timeTaken)) {
                is Resource.Success -> {
                    _playState.update { it.copy(isFinished = true) }
                }
                is Resource.Error -> {
                    _entryState.value = UiState.Error(resource.message)
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    private fun startTimer(seconds: Int) {
        _timeLeft.value = seconds
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000L)
                if (!_isPaused.value) {
                    _timeLeft.update { it - 1 }
                }
            }
            if (_timeLeft.value <= 0 && !_playState.value.isFinished) {
                _playState.update { it.copy(isFinished = true) }
                submitTournament()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
    }
}
