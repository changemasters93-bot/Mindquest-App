package com.android.mindquest.presentation.quiz

import com.android.mindquest.domain.model.Question
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizConfig

/**
 * Singleton holder for sharing quiz state across navigation destinations.
 *
 * Stores the selected quiz + [QuizConfig] before starting, and the completed
 * answers/questions so the review screen can access them without ViewModel
 * sharing issues (Compose Navigation creates new ViewModel per route).
 *
 * Flow:
 * 1. Entry point calls [selectQuiz] with quiz data + config
 * 2. QuizPlayScreen reads via [QuizViewModel.startFromSession]
 * 3. On finish, [saveCompletedData] stores results for ReviewScreen
 * 4. [clear] resets when leaving the quiz flow
 */
object QuizSessionHolder {

    /** The quiz selected for play, consumed by QuizIntro and QuizPlay. */
    var currentQuiz: Quiz? = null
        private set

    /**
     * Configuration that drives quiz behavior (feedback, submission, nudges, etc.).
     * Set by the entry point (module, IQ, tournament) via [selectQuiz].
     */
    var config: QuizConfig? = null
        private set

    /** Module accent color hex (without #). */
    var moduleColor: String = ""
        private set

    /** Module emoji for display. */
    var moduleEmoji: String = ""
        private set

    /** Module title for display. */
    var moduleTitle: String = ""
        private set

    /** Questions from the completed quiz, for review. */
    var completedQuestions: List<Question> = emptyList()
        private set

    /** Answers from the completed quiz, for review. */
    var completedAnswers: List<QuizAnswer> = emptyList()
        private set

    /**
     * Prepare a quiz session with the given config.
     *
     * @param quiz       The quiz to play
     * @param config     Behavioral configuration for this session
     * @param moduleColor Accent color hex (falls back to config.accentColorHex)
     * @param moduleEmoji Module emoji for top bar display
     * @param moduleTitle Module title for top bar display
     */
    fun selectQuiz(
        quiz: Quiz,
        config: QuizConfig,
        moduleColor: String = config.accentColorHex ?: "",
        moduleEmoji: String = "",
        moduleTitle: String = "",
    ) {
        this.currentQuiz = quiz
        this.config = config
        this.moduleColor = moduleColor
        this.moduleEmoji = moduleEmoji
        this.moduleTitle = moduleTitle
        this.completedQuestions = emptyList()
        this.completedAnswers = emptyList()
    }

    fun saveCompletedData(
        questions: List<Question>,
        answers: List<QuizAnswer>,
    ) {
        this.completedQuestions = questions
        this.completedAnswers = answers
    }

    fun clear() {
        currentQuiz = null
        config = null
        moduleColor = ""
        moduleEmoji = ""
        moduleTitle = ""
        completedQuestions = emptyList()
        completedAnswers = emptyList()
    }
}
