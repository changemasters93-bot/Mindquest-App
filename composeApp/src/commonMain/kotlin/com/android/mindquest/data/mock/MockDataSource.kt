package com.android.mindquest.data.mock

import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.ChapterProgress
import com.android.mindquest.domain.model.ChapterState
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.CompletedChapter
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.DailyActivity
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.DashboardData
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardEntry
import com.android.mindquest.domain.model.MatchPair
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.ModuleProgress
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.model.Question
import com.android.mindquest.domain.model.QuestionOption
import com.android.mindquest.domain.model.QuestionType
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.model.SubjectPerformance
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.model.TournamentEntryStatus
import com.android.mindquest.domain.model.TournamentResult
import com.android.mindquest.domain.model.TournamentStatus
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.model.UserRank
import com.android.mindquest.domain.model.UserStats
import kotlinx.datetime.Clock

/**
 * Dev-toggle for which tournament scenario the mock data represents.
 * Change this constant and recompile to test different banner/lobby states.
 */
enum class MockTournamentScenario {
    PLAY,            // LIVE + no entry (NOT_STARTED)
    RESUME,          // LIVE + IN_PROGRESS entry
    COMPLETED,       // LIVE + COMPLETED entry
    UPCOMING,        // SCHEDULED tournament, no entry
    CLOSED,          // CLOSED tournament + COMPLETED entry
    CLOSED_NO_ENTRY, // CLOSED tournament + no entry (user never played)
}

/**
 * Provides complete mock data for offline development and UI previews.
 *
 * All data is deterministic and matches the reference JSX designs, using
 * the same names, scores, and progression state so that UI screenshots
 * look consistent during development.
 */
object MockDataSource {

    // ── Shared constants ─────────────────────────────────────────────────

    private const val MOCK_USER_ID = "mock-user-001"
    private val nowMillis = Clock.System.now().toEpochMilliseconds()

    /** Toggle this to test different tournament states on the home screen. */
    val MOCK_TOURNAMENT_SCENARIO = MockTournamentScenario.CLOSED_NO_ENTRY

    // ── Dashboard ────────────────────────────────────────────────────────

    fun mockDashboard(): DashboardData {
        val (tournament, entry) = mockTournamentForScenario(MOCK_TOURNAMENT_SCENARIO)
        return DashboardData(
            user = mockUser(),
            stats = mockUserStats(),
            modules = mockModules(),
            activeTournament = tournament,
            activeTournamentEntry = entry,
        )
    }

    fun mockUser(): User = User(
        id = MOCK_USER_ID,
        displayName = "Neha",
        avatarId = 3,
        gradeId = "grade-5",
        gradeLabel = "Grade 5",
        authProvider = "google",
        isAnonymous = false,
        countryId = "country-in",
        countryName = "India",
        cityId = "city-del",
        cityName = "Delhi",
        schoolName = "Delhi Public School"
    )

    fun mockUserStats(): UserStats = UserStats(
        totalXp = 2850L,
        level = 5,
        streakCurrent = 7,
        streakBest = 14,
        quizzesCompleted = 42,
        accuracyPct = 78.5,
        tournamentsPlayed = 3,
        bestTournamentRank = 5
    )

    fun mockModules(): List<Module> = listOf(
        // ── In-Progress state ─────────────────────────────────────────
        Module(
            id = "mod-math",
            title = "Mathematics",
            subtitle = "Numbers, Shapes & Logic",
            emoji = "\uD83D\uDCD0",
            accentColor = "#6366F1",
            displayOrder = 1,
            progress = ModuleProgress(
                currentChapterId = "ch-math-3",
                currentQuizId = "quiz-math-3-1",
                bestScorePct = 65,
                isCompleted = false,
            ),
        ),
        // ── Completed state ───────────────────────────────────────────
        Module(
            id = "mod-english",
            title = "English",
            subtitle = "Grammar, Vocabulary & Comprehension",
            emoji = "\uD83D\uDCD6",
            accentColor = "#F59E0B",
            displayOrder = 2,
            progress = ModuleProgress(
                currentChapterId = null,
                currentQuizId = null,
                bestScorePct = 100,
                isCompleted = true,
            ),
        ),
        // ── In-Progress state ─────────────────────────────────────────
        Module(
            id = "mod-science",
            title = "Science",
            subtitle = "Explore the Natural World",
            emoji = "\uD83D\uDD2C",
            accentColor = "#10B981",
            displayOrder = 3,
            progress = ModuleProgress(
                currentChapterId = "ch-sci-2",
                currentQuizId = null,
                bestScorePct = 40,
                isCompleted = false,
            ),
        ),
        // ── Not Started state (initial) ───────────────────────────────
        Module(
            id = "mod-gk",
            title = "General Knowledge",
            subtitle = "Current Affairs & Trivia",
            emoji = "\uD83D\uDCA1",
            accentColor = "#EC4899",
            displayOrder = 4,
            progress = null,
        ),
    )

    // ── Daily Challenges ─────────────────────────────────────────────────

    fun mockDailyChallenges(): List<DailyChallenge> = listOf(
        DailyChallenge(
            quizId = "dc-quiz-1",
            title = "Quick Math Sprint",
            description = "Solve 5 arithmetic problems in 3 minutes",
            questionCount = 5,
            timeInMinutes = 3,
            moduleId = "mod-math",
            chapterId = "ch-math-1",
            isDone = false
        ),
        DailyChallenge(
            quizId = "dc-quiz-2",
            title = "Science Snapshot",
            description = "Test your knowledge of the solar system",
            questionCount = 5,
            timeInMinutes = 3,
            moduleId = "mod-science",
            chapterId = "ch-sci-1",
            isDone = true
        ),
        DailyChallenge(
            quizId = "dc-quiz-3",
            title = "Word Wizard",
            description = "Find the correct meanings of 5 words",
            questionCount = 5,
            timeInMinutes = 3,
            moduleId = "mod-english",
            chapterId = "ch-eng-1",
            isDone = false
        )
    )

    // ── Module Full (Chapters) ───────────────────────────────────────────

    fun mockModuleFull(moduleId: String): Pair<Module, List<Chapter>> {
        val module = mockModules().find { it.id == moduleId } ?: mockModules().first()
        val chapters = when (moduleId) {
            "mod-math" -> mockMathChapters()
            "mod-science" -> mockScienceChapters()
            "mod-english" -> mockEnglishChapters()
            "mod-gk" -> mockGkChapters()
            else -> mockMathChapters()
        }
        return module to chapters
    }

    private fun mockMathChapters(): List<Chapter> = listOf(
        Chapter(
            id = "ch-math-1",
            title = "Number Systems",
            chapterNumber = 1,
            quizCount = 3,
            state = ChapterState.COMPLETED,
            progress = ChapterProgress(quizzesDone = 3, totalQuizzes = 3, bestScorePct = 90, isCompleted = true)
        ),
        Chapter(
            id = "ch-math-2",
            title = "Fractions & Decimals",
            chapterNumber = 2,
            quizCount = 3,
            state = ChapterState.COMPLETED,
            progress = ChapterProgress(quizzesDone = 3, totalQuizzes = 3, bestScorePct = 85, isCompleted = true)
        ),
        Chapter(
            id = "ch-math-3",
            title = "Geometry Basics",
            chapterNumber = 3,
            quizCount = 3,
            state = ChapterState.UNLOCKED,
            progress = ChapterProgress(quizzesDone = 1, totalQuizzes = 3, bestScorePct = 80, isCompleted = false)
        ),
        Chapter(
            id = "ch-math-4",
            title = "Data Handling",
            chapterNumber = 4,
            quizCount = 3,
            state = ChapterState.LOCKED,
            progress = null
        )
    )

    private fun mockScienceChapters(): List<Chapter> = listOf(
        Chapter(
            id = "ch-sci-1",
            title = "The Solar System",
            chapterNumber = 1,
            quizCount = 3,
            state = ChapterState.COMPLETED,
            progress = ChapterProgress(quizzesDone = 3, totalQuizzes = 3, bestScorePct = 72, isCompleted = true)
        ),
        Chapter(
            id = "ch-sci-2",
            title = "Living Things",
            chapterNumber = 2,
            quizCount = 3,
            state = ChapterState.UNLOCKED,
            progress = ChapterProgress(quizzesDone = 0, totalQuizzes = 3, bestScorePct = null, isCompleted = false)
        ),
        Chapter(
            id = "ch-sci-3",
            title = "Matter & Materials",
            chapterNumber = 3,
            quizCount = 3,
            state = ChapterState.LOCKED,
            progress = null
        )
    )

    private fun mockEnglishChapters(): List<Chapter> = listOf(
        Chapter(
            id = "ch-eng-1",
            title = "Parts of Speech",
            chapterNumber = 1,
            quizCount = 3,
            state = ChapterState.UNLOCKED,
            progress = ChapterProgress(quizzesDone = 2, totalQuizzes = 3, bestScorePct = 90, isCompleted = false)
        ),
        Chapter(
            id = "ch-eng-2",
            title = "Sentence Structure",
            chapterNumber = 2,
            quizCount = 3,
            state = ChapterState.LOCKED,
            progress = null
        ),
        Chapter(
            id = "ch-eng-3",
            title = "Reading Comprehension",
            chapterNumber = 3,
            quizCount = 3,
            state = ChapterState.LOCKED,
            progress = null
        )
    )

    private fun mockGkChapters(): List<Chapter> = listOf(
        Chapter(
            id = "ch-gk-1",
            title = "Countries & Capitals",
            chapterNumber = 1,
            quizCount = 3,
            state = ChapterState.UNLOCKED,
            progress = null
        ),
        Chapter(
            id = "ch-gk-2",
            title = "Famous Inventions",
            chapterNumber = 2,
            quizCount = 3,
            state = ChapterState.LOCKED,
            progress = null
        )
    )

    // ── Chapter Quizzes (progressive unlock) ────────────────────────────

    fun mockChapterQuizzes(chapterId: String): List<Quiz> {
        // Progressive unlock logic per chapter
        return when {
            // Completed chapters: all quizzes completed
            chapterId == "ch-math-1" || chapterId == "ch-math-2" || chapterId == "ch-sci-1" -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Practice Quiz 1", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = 90, attemptCount = 2,
                    questions = mockMcqQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "Practice Quiz 2", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "medium",
                    displayOrder = 2, bestScore = 85, attemptCount = 1,
                    questions = mockTrueFalseQuestions("$chapterId-quiz-2")),
                Quiz(id = "$chapterId-quiz-3", title = "Chapter Test", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = 80, attemptCount = 1,
                    questions = mockFillBlankQuestions("$chapterId-quiz-3")),
            )
            // Math ch-3 (unlocked, in progress): quiz 1 done, quiz 2 = ALL TYPES DEMO (unlocked), quiz 3 locked
            chapterId == "ch-math-3" -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Shapes & Angles", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = 80, attemptCount = 1,
                    questions = mockVisualChoiceQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "All Question Types", quizType = "practice",
                    questionCount = 60, timeLimitSeconds = 1800, maxXp = 500, difficulty = "medium",
                    displayOrder = 2, bestScore = null, attemptCount = 0, isLocked = false,
                    questions = mockAllQuestionTypes()),
                Quiz(id = "$chapterId-quiz-3", title = "Pattern Challenge", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockGridPatternQuestions("$chapterId-quiz-3")),
            )
            // English ch-1 (unlocked, 2 done): quiz 1 & 2 done, quiz 3 unlocked
            chapterId == "ch-eng-1" -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Nouns & Pronouns", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = 95, attemptCount = 1,
                    questions = mockMcqQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "Verbs & Adverbs", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "medium",
                    displayOrder = 2, bestScore = 90, attemptCount = 1,
                    questions = mockSelectWordQuestions("$chapterId-quiz-2")),
                Quiz(id = "$chapterId-quiz-3", title = "Grammar Test", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = null, attemptCount = 0, isLocked = false,
                    questions = mockStatementReasonQuestions("$chapterId-quiz-3")),
            )
            // Science ch-2 (unlocked, not started): quiz 1 unlocked (table data), rest locked
            chapterId == "ch-sci-2" -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Data Analysis", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = null, attemptCount = 0, isLocked = false,
                    questions = mockTableDataQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "Memory Challenge", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "medium",
                    displayOrder = 2, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockMemoryQuestions("$chapterId-quiz-2")),
                Quiz(id = "$chapterId-quiz-3", title = "Living Things Test", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockMatchQuestions("$chapterId-quiz-3")),
            )
            // GK ch-1 (unlocked, fresh): quiz 1 unlocked (visual), rest locked (grid pattern, memory)
            chapterId == "ch-gk-1" -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Visual Quiz", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = null, attemptCount = 0, isLocked = false,
                    questions = mockVisualChoiceQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "Pattern Puzzles", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "medium",
                    displayOrder = 2, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockGridPatternQuestions("$chapterId-quiz-2")),
                Quiz(id = "$chapterId-quiz-3", title = "Memory Master", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockMemoryQuestions("$chapterId-quiz-3")),
            )
            // Default for locked chapters: all quizzes locked
            else -> listOf(
                Quiz(id = "$chapterId-quiz-1", title = "Practice Quiz 1", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "easy",
                    displayOrder = 1, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockMcqQuestions("$chapterId-quiz-1")),
                Quiz(id = "$chapterId-quiz-2", title = "Practice Quiz 2", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 300, maxXp = 50, difficulty = "medium",
                    displayOrder = 2, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockTrueFalseQuestions("$chapterId-quiz-2")),
                Quiz(id = "$chapterId-quiz-3", title = "Chapter Test", quizType = "practice",
                    questionCount = 5, timeLimitSeconds = 600, maxXp = 100, difficulty = "hard",
                    displayOrder = 3, bestScore = null, attemptCount = 0, isLocked = true,
                    questions = mockFillBlankQuestions("$chapterId-quiz-3")),
            )
        }
    }

    // ── ALL Question Types Demo (multiple variants per type) ────────────
    //
    // Supabase question_type mapping:
    //   multiple_choice       → QuestionType.MULTIPLE_CHOICE
    //   true_false             → QuestionType.TRUE_FALSE
    //   fill_blank             → QuestionType.FILL_BLANK
    //   ordering               → QuestionType.ORDERING
    //   match                  → QuestionType.MATCH
    //   select_word            → QuestionType.SELECT_WORD
    //   statement_reason       → QuestionType.STATEMENT_REASON
    //   visual_single_choice   → QuestionType.VISUAL_SINGLE_CHOICE
    //   matrix                 → QuestionType.MATRIX
    //   grid_pattern           → QuestionType.GRID_PATTERN
    //   table_data             → QuestionType.TABLE_DATA
    //   memory                 → QuestionType.MEMORY

    private fun mockAllQuestionTypes(): List<Question> = listOf(
        // ── MCQ: Text (standard 4-option) ─────────────────────────────────
        Question(
            id = "demo-q1", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is 15 + 27?",
            explanation = "15 + 27 = 42.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q1-a", label = "42", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q1-b", label = "41", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q1-c", label = "43", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q1-d", label = "40", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── MCQ: Concept (longer options) ─────────────────────────────────
        Question(
            id = "demo-q2", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which of the following best describes photosynthesis?",
            explanation = "Photosynthesis converts sunlight, water and CO2 into glucose and oxygen.",
            difficulty = "medium", timeLimitSeconds = 40,
            options = listOf(
                QuestionOption(id = "demo-q2-a", label = "Plants convert sunlight into food using chlorophyll", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q2-b", label = "Plants absorb food from the soil through roots", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q2-c", label = "Plants release carbon dioxide during the day", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q2-d", label = "Plants grow taller in complete darkness", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── TRUE/FALSE ────────────────────────────────────────────────────
        Question(
            id = "demo-q3", questionType = QuestionType.TRUE_FALSE,
            title = "The Earth revolves around the Sun.",
            explanation = "True! The Earth orbits the Sun once every 365.25 days.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q3-a", label = "True", isCorrect = true, displayOrder = 1),
                QuestionOption(id = "demo-q3-b", label = "False", isCorrect = false, displayOrder = 2),
            ),
        ),
        // ── TRUE/FALSE: Science ───────────────────────────────────────────
        Question(
            id = "demo-q4", questionType = QuestionType.TRUE_FALSE,
            title = "Sound travels faster in air than in water.",
            explanation = "False! Sound travels about 4x faster in water than in air.",
            difficulty = "medium", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q4-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "demo-q4-b", label = "False", isCorrect = true, displayOrder = 2),
            ),
        ),
        // ── FILL BLANK: Geography ─────────────────────────────────────────
        Question(
            id = "demo-q5", questionType = QuestionType.FILL_BLANK,
            title = "The capital of India is ____.",
            prompt = "Type the correct answer",
            explanation = "New Delhi is the capital of India.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q5-a", label = "New Delhi", isCorrect = true, displayOrder = 1),
            ),
        ),
        // ── FILL BLANK: Math ──────────────────────────────────────────────
        Question(
            id = "demo-q6", questionType = QuestionType.FILL_BLANK,
            title = "The square root of 144 is ____.",
            prompt = "Enter the number",
            explanation = "12 x 12 = 144, so the square root of 144 is 12.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q6-a", label = "12", isCorrect = true, displayOrder = 1),
            ),
        ),
        // ── ORDERING: Numbers ─────────────────────────────────────────────
        Question(
            id = "demo-q7", questionType = QuestionType.ORDERING,
            title = "Arrange these numbers in ascending order:",
            prompt = "Tap two items to swap their positions",
            explanation = "Correct order: 12, 25, 38, 47.",
            difficulty = "easy", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "demo-q7-a", label = "38", isCorrect = false, displayOrder = 1, correctPosition = 3),
                QuestionOption(id = "demo-q7-b", label = "12", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "demo-q7-c", label = "47", isCorrect = false, displayOrder = 3, correctPosition = 4),
                QuestionOption(id = "demo-q7-d", label = "25", isCorrect = false, displayOrder = 4, correctPosition = 2),
            ),
        ),
        // ── ORDERING: Historical Events ───────────────────────────────────
        Question(
            id = "demo-q8", questionType = QuestionType.ORDERING,
            title = "Arrange these events in chronological order:",
            prompt = "Earliest first, latest last",
            explanation = "Independence (1947) \u2192 Republic Day (1950) \u2192 Moon Landing (1969) \u2192 Internet (1991).",
            difficulty = "medium", timeLimitSeconds = 60,
            options = listOf(
                QuestionOption(id = "demo-q8-a", label = "\uD83C\uDF0D Internet launched (1991)", isCorrect = false, displayOrder = 1, correctPosition = 4),
                QuestionOption(id = "demo-q8-b", label = "\uD83C\uDDEE\uD83C\uDDF3 India's Independence (1947)", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "demo-q8-c", label = "\uD83C\uDF19 Moon landing (1969)", isCorrect = false, displayOrder = 3, correctPosition = 3),
                QuestionOption(id = "demo-q8-d", label = "\uD83C\uDDEE\uD83C\uDDF3 Republic Day (1950)", isCorrect = false, displayOrder = 4, correctPosition = 2),
            ),
        ),
        // ── MATCH: Countries \u2194 Capitals ───────────────────────────────────
        Question(
            id = "demo-q9", questionType = QuestionType.MATCH,
            title = "Match the countries with their capitals:",
            prompt = "Tap a country, then tap its capital",
            explanation = "India-New Delhi, Japan-Tokyo, France-Paris, Egypt-Cairo",
            difficulty = "medium", timeLimitSeconds = 60,
            matchPairs = listOf(
                MatchPair(id = "demo-q9-m1", leftText = "\uD83C\uDDEE\uD83C\uDDF3 India", rightText = "New Delhi", displayOrder = 1),
                MatchPair(id = "demo-q9-m2", leftText = "\uD83C\uDDEF\uD83C\uDDF5 Japan", rightText = "Tokyo", displayOrder = 2),
                MatchPair(id = "demo-q9-m3", leftText = "\uD83C\uDDEB\uD83C\uDDF7 France", rightText = "Paris", displayOrder = 3),
                MatchPair(id = "demo-q9-m4", leftText = "\uD83C\uDDEA\uD83C\uDDEC Egypt", rightText = "Cairo", displayOrder = 4),
            ),
        ),
        // ── MATCH: Science Symbols ────────────────────────────────────────
        Question(
            id = "demo-q10", questionType = QuestionType.MATCH,
            title = "Match the elements with their symbols:",
            prompt = "Tap an element, then tap its symbol",
            explanation = "Oxygen-O, Gold-Au, Iron-Fe, Sodium-Na",
            difficulty = "hard", timeLimitSeconds = 60,
            matchPairs = listOf(
                MatchPair(id = "demo-q10-m1", leftText = "Oxygen", rightText = "O", displayOrder = 1),
                MatchPair(id = "demo-q10-m2", leftText = "Gold", rightText = "Au", displayOrder = 2),
                MatchPair(id = "demo-q10-m3", leftText = "Iron", rightText = "Fe", displayOrder = 3),
                MatchPair(id = "demo-q10-m4", leftText = "Sodium", rightText = "Na", displayOrder = 4),
            ),
        ),
        // ── SELECT WORD: Verb ─────────────────────────────────────────────
        Question(
            id = "demo-q11", questionType = QuestionType.SELECT_WORD,
            title = "Select the VERB in the sentence:",
            prompt = "The cat jumped over the fence.",
            explanation = "'Jumped' is the verb (action word).",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q11-a", label = "The", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "demo-q11-b", label = "cat", isCorrect = false, displayOrder = 2),
                QuestionOption(id = "demo-q11-c", label = "jumped", isCorrect = true, displayOrder = 3),
                QuestionOption(id = "demo-q11-d", label = "over", isCorrect = false, displayOrder = 4),
                QuestionOption(id = "demo-q11-e", label = "the", isCorrect = false, displayOrder = 5),
                QuestionOption(id = "demo-q11-f", label = "fence", isCorrect = false, displayOrder = 6),
            ),
        ),
        // ── SELECT WORD: Adjectives (multi-select) ────────────────────────
        Question(
            id = "demo-q12", questionType = QuestionType.SELECT_WORD,
            title = "Select ALL adjectives in the sentence:",
            prompt = "The tall, beautiful girl wore a bright red dress.",
            explanation = "Tall, beautiful, bright, and red are all adjectives.",
            difficulty = "medium", timeLimitSeconds = 40,
            allowMultiple = true,
            options = listOf(
                QuestionOption(id = "demo-q12-a", label = "The", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "demo-q12-b", label = "tall", isCorrect = true, displayOrder = 2),
                QuestionOption(id = "demo-q12-c", label = "beautiful", isCorrect = true, displayOrder = 3),
                QuestionOption(id = "demo-q12-d", label = "girl", isCorrect = false, displayOrder = 4),
                QuestionOption(id = "demo-q12-e", label = "wore", isCorrect = false, displayOrder = 5),
                QuestionOption(id = "demo-q12-f", label = "bright", isCorrect = true, displayOrder = 6),
                QuestionOption(id = "demo-q12-g", label = "red", isCorrect = true, displayOrder = 7),
                QuestionOption(id = "demo-q12-h", label = "dress", isCorrect = false, displayOrder = 8),
            ),
        ),
        // ── STATEMENT & REASON ────────────────────────────────────────────
        Question(
            id = "demo-q13", questionType = QuestionType.STATEMENT_REASON,
            title = "Statement: All squares are rectangles.\nReason: A rectangle has four right angles.",
            prompt = "Are both correct? Is the reason the correct explanation?",
            explanation = "Both are true, and the reason correctly explains why.",
            difficulty = "hard", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "demo-q13-a", label = "Both true, reason is correct explanation", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q13-b", label = "Both true, reason is NOT correct explanation", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q13-c", label = "Statement true, reason false", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q13-d", label = "Both false", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── VISUAL: Shapes (Canvas-rendered tokens) ───────────────────────
        Question(
            id = "demo-q14", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which shape has exactly 4 equal sides?",
            explanation = "A square has 4 equal sides and 4 right angles.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q14-a", label = "Triangle", isCorrect = false, displayOrder = 1, visualLabel = "shape:triangle"),
                QuestionOption(id = "demo-q14-b", label = "Square", isCorrect = true, displayOrder = 2, visualLabel = "shape:square"),
                QuestionOption(id = "demo-q14-c", label = "Circle", isCorrect = false, displayOrder = 3, visualLabel = "shape:circle"),
                QuestionOption(id = "demo-q14-d", label = "Star", isCorrect = false, displayOrder = 4, visualLabel = "shape:star"),
            ),
        ),
        // ── VISUAL: Combo tokens (colored shapes) ─────────────────────────
        Question(
            id = "demo-q15", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which colored shape is a blue diamond?",
            explanation = "A diamond is a rotated square shape, colored blue.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q15-a", label = "Red Circle", isCorrect = false, displayOrder = 1, visualLabel = "combo:red_circle"),
                QuestionOption(id = "demo-q15-b", label = "Blue Diamond", isCorrect = true, displayOrder = 2, visualLabel = "combo:blue_diamond"),
                QuestionOption(id = "demo-q15-c", label = "Green Triangle", isCorrect = false, displayOrder = 3, visualLabel = "combo:green_triangle"),
                QuestionOption(id = "demo-q15-d", label = "Purple Star", isCorrect = false, displayOrder = 4, visualLabel = "combo:purple_star"),
            ),
        ),
        // ── VISUAL: Emoji-based (animals) ─────────────────────────────────
        Question(
            id = "demo-q16", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which animal can fly?",
            explanation = "Eagles are birds of prey that can fly.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "demo-q16-a", label = "Dog", isCorrect = false, displayOrder = 1, visualLabel = "\uD83D\uDC36"),
                QuestionOption(id = "demo-q16-b", label = "Fish", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDC1F"),
                QuestionOption(id = "demo-q16-c", label = "Eagle", isCorrect = true, displayOrder = 3, visualLabel = "\uD83E\uDD85"),
                QuestionOption(id = "demo-q16-d", label = "Cat", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDC31"),
            ),
        ),
        // ── MATRIX ────────────────────────────────────────────────────────
        Question(
            id = "demo-q17", questionType = QuestionType.MATRIX,
            title = "Find the missing number in the matrix",
            prompt = "2  4  6\n8  ?  12\n14 16 18",
            explanation = "The numbers increase by 2 in each cell. Missing = 10.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q17-a", label = "10", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q17-b", label = "11", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q17-c", label = "9", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q17-d", label = "13", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── GRID PATTERN ──────────────────────────────────────────────────
        Question(
            id = "demo-q18", questionType = QuestionType.GRID_PATTERN,
            title = "What comes next in the pattern?",
            prompt = "\uD83D\uDD34 \uD83D\uDD35 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDD34 ?",
            explanation = "Red and blue alternate, so the next is blue.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q18-a", label = "Blue", isCorrect = true, displayOrder = 1, visualLabel = "\uD83D\uDD35"),
                QuestionOption(id = "demo-q18-b", label = "Red", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDD34"),
                QuestionOption(id = "demo-q18-c", label = "Green", isCorrect = false, displayOrder = 3, visualLabel = "\uD83D\uDFE2"),
                QuestionOption(id = "demo-q18-d", label = "Yellow", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDFE1"),
            ),
        ),
        // ── TABLE DATA ────────────────────────────────────────────────────
        Question(
            id = "demo-q19", questionType = QuestionType.TABLE_DATA,
            title = "Look at the table and answer: Who scored the highest marks?",
            prompt = "Student | Marks\nAarav | 85\nPriya | 92\nRohan | 78\nAnanya | 95",
            explanation = "Ananya scored 95, which is the highest.",
            difficulty = "easy", timeLimitSeconds = 25,
            options = listOf(
                QuestionOption(id = "demo-q19-a", label = "Aarav", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q19-b", label = "Priya", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q19-c", label = "Rohan", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q19-d", label = "Ananya", isCorrect = true, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── MEMORY ────────────────────────────────────────────────────────
        Question(
            id = "demo-q20", questionType = QuestionType.MEMORY,
            title = "Which item was NOT in the group shown?",
            prompt = "\uD83C\uDF4E \uD83C\uDF3A \uD83D\uDCDA \u2B50 \uD83C\uDFB5",
            explanation = "The trophy was not in the displayed group.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q20-a", label = "Apple \uD83C\uDF4E", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q20-b", label = "Flower \uD83C\uDF3A", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q20-c", label = "Trophy \uD83C\uDFC6", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q20-d", label = "Star \u2B50", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ═══════════════════════════════════════════════════════════════════
        // ── VARIETY QUESTIONS: Pattern, Memory, Cognitive, Grid ──────────
        // ═══════════════════════════════════════════════════════════════════

        // ── PATTERN: Color Sequence ─────────────────────────────────────
        // Supabase: question_type = "grid_pattern", promptConfig.type = "color_sequence"
        Question(
            id = "demo-q21", questionType = QuestionType.GRID_PATTERN,
            title = "What color comes next in the pattern?",
            prompt = "\uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE2 \uD83D\uDD34 \uD83D\uDD35 ?",
            explanation = "The pattern repeats Red-Blue-Green. After Blue, Green comes next.",
            difficulty = "easy", timeLimitSeconds = 20,
            promptConfig = mapOf("type" to "color_sequence", "pattern" to "R,B,G"),
            options = listOf(
                QuestionOption(id = "demo-q21-a", label = "Green", isCorrect = true, displayOrder = 1, visualLabel = "\uD83D\uDFE2"),
                QuestionOption(id = "demo-q21-b", label = "Red", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDD34"),
                QuestionOption(id = "demo-q21-c", label = "Blue", isCorrect = false, displayOrder = 3, visualLabel = "\uD83D\uDD35"),
                QuestionOption(id = "demo-q21-d", label = "Yellow", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDFE1"),
            ),
        ),
        // ── PATTERN: Shape Sequence ─────────────────────────────────────
        // Supabase: question_type = "grid_pattern", promptConfig.type = "shape_sequence"
        Question(
            id = "demo-q22", questionType = QuestionType.GRID_PATTERN,
            title = "Which shape continues this sequence?",
            prompt = "\u25B3 \u25CB \u25A1 \u25B3 \u25CB ?",
            explanation = "Triangle, Circle, Square repeats. After Circle, Square is next.",
            difficulty = "easy", timeLimitSeconds = 25,
            promptConfig = mapOf("type" to "shape_sequence"),
            options = listOf(
                QuestionOption(id = "demo-q22-a", label = "Square", isCorrect = true, displayOrder = 1, visualLabel = "shape:square"),
                QuestionOption(id = "demo-q22-b", label = "Triangle", isCorrect = false, displayOrder = 2, visualLabel = "shape:triangle"),
                QuestionOption(id = "demo-q22-c", label = "Circle", isCorrect = false, displayOrder = 3, visualLabel = "shape:circle"),
                QuestionOption(id = "demo-q22-d", label = "Star", isCorrect = false, displayOrder = 4, visualLabel = "shape:star"),
            ),
        ),
        // ── PATTERN: Number Sequence ────────────────────────────────────
        // Supabase: question_type = "grid_pattern", promptConfig.type = "number_sequence"
        Question(
            id = "demo-q23", questionType = QuestionType.GRID_PATTERN,
            title = "What number comes next?",
            prompt = "2 \u2192 4 \u2192 8 \u2192 16 \u2192 ?",
            explanation = "Each number doubles. 16 \u00D7 2 = 32.",
            difficulty = "medium", timeLimitSeconds = 25,
            promptConfig = mapOf("type" to "number_sequence", "rule" to "multiply_2"),
            options = listOf(
                QuestionOption(id = "demo-q23-a", label = "32", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q23-b", label = "24", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q23-c", label = "20", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q23-d", label = "18", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── PATTERN: Letter Sequence ────────────────────────────────────
        // Supabase: question_type = "grid_pattern", promptConfig.type = "letter_sequence"
        Question(
            id = "demo-q24", questionType = QuestionType.GRID_PATTERN,
            title = "Which letter comes next?",
            prompt = "A \u2192 C \u2192 E \u2192 G \u2192 ?",
            explanation = "Alternating letters (skip one): A, C, E, G, I.",
            difficulty = "medium", timeLimitSeconds = 25,
            promptConfig = mapOf("type" to "letter_sequence", "rule" to "skip_1"),
            options = listOf(
                QuestionOption(id = "demo-q24-a", label = "I", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q24-b", label = "H", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q24-c", label = "J", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q24-d", label = "K", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── PATTERN: Fibonacci ──────────────────────────────────────────
        Question(
            id = "demo-q25", questionType = QuestionType.GRID_PATTERN,
            title = "Complete the Fibonacci sequence:",
            prompt = "1 \u2192 1 \u2192 2 \u2192 3 \u2192 5 \u2192 8 \u2192 ?",
            explanation = "Each number is the sum of the two before it. 5 + 8 = 13.",
            difficulty = "hard", timeLimitSeconds = 30,
            promptConfig = mapOf("type" to "number_sequence", "rule" to "fibonacci"),
            options = listOf(
                QuestionOption(id = "demo-q25-a", label = "13", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q25-b", label = "11", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q25-c", label = "10", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q25-d", label = "14", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── MEMORY: Sequence Recall ─────────────────────────────────────
        // Supabase: question_type = "memory", promptConfig.type = "sequence_recall"
        Question(
            id = "demo-q26", questionType = QuestionType.MEMORY,
            title = "Remember the sequence, then pick the correct order:",
            prompt = "\uD83D\uDFE2 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE1",
            explanation = "The shown sequence was Green, Red, Blue, Yellow.",
            difficulty = "medium", timeLimitSeconds = 15,
            promptConfig = mapOf("type" to "sequence_recall", "display_time_ms" to 3000),
            options = listOf(
                QuestionOption(id = "demo-q26-a", label = "\uD83D\uDFE2 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE1", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q26-b", label = "\uD83D\uDD34 \uD83D\uDFE2 \uD83D\uDD35 \uD83D\uDFE1", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q26-c", label = "\uD83D\uDFE2 \uD83D\uDD35 \uD83D\uDD34 \uD83D\uDFE1", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q26-d", label = "\uD83D\uDFE1 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE2", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── MEMORY: Shape Recall ────────────────────────────────────────
        Question(
            id = "demo-q27", questionType = QuestionType.MEMORY,
            title = "Which shape was shown third in the sequence?",
            prompt = "\u2B50 \u25B3 \u2665 \u25A1 \u25CB",
            explanation = "The third shape was Heart (\u2665).",
            difficulty = "medium", timeLimitSeconds = 15,
            promptConfig = mapOf("type" to "position_recall", "target_position" to 3),
            options = listOf(
                QuestionOption(id = "demo-q27-a", label = "Heart", isCorrect = true, displayOrder = 1, visualLabel = "shape:heart"),
                QuestionOption(id = "demo-q27-b", label = "Star", isCorrect = false, displayOrder = 2, visualLabel = "shape:star"),
                QuestionOption(id = "demo-q27-c", label = "Triangle", isCorrect = false, displayOrder = 3, visualLabel = "shape:triangle"),
                QuestionOption(id = "demo-q27-d", label = "Circle", isCorrect = false, displayOrder = 4, visualLabel = "shape:circle"),
            ),
        ),
        // ── MEMORY: Count Recall ────────────────────────────────────────
        Question(
            id = "demo-q28", questionType = QuestionType.MEMORY,
            title = "How many BLUE items were shown?",
            prompt = "\uD83D\uDD35 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE2 \uD83D\uDD35 \uD83D\uDFE1 \uD83D\uDD34 \uD83D\uDD35",
            explanation = "There are 4 blue circles in the sequence.",
            difficulty = "hard", timeLimitSeconds = 15,
            promptConfig = mapOf("type" to "count_recall", "target" to "blue"),
            options = listOf(
                QuestionOption(id = "demo-q28-a", label = "4", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q28-b", label = "3", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q28-c", label = "5", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q28-d", label = "2", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── COGNITIVE: Odd One Out ──────────────────────────────────────
        // Supabase: question_type = "multiple_choice", metadata.cognitive_type = "odd_one_out"
        Question(
            id = "demo-q29", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which one does NOT belong in this group?",
            explanation = "Car, Bus, and Train are vehicles. Piano is a musical instrument.",
            difficulty = "easy", timeLimitSeconds = 20,
            metadata = mapOf("cognitive_type" to "odd_one_out"),
            options = listOf(
                QuestionOption(id = "demo-q29-a", label = "\uD83D\uDE97 Car", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q29-b", label = "\uD83D\uDE8C Bus", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q29-c", label = "\uD83C\uDFB9 Piano", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q29-d", label = "\uD83D\uDE86 Train", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── COGNITIVE: Odd One Out (Shapes) ─────────────────────────────
        Question(
            id = "demo-q30", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which shape is the odd one out?",
            explanation = "Triangle, Square, and Pentagon have straight sides. Circle has none.",
            difficulty = "easy", timeLimitSeconds = 20,
            metadata = mapOf("cognitive_type" to "odd_one_out"),
            options = listOf(
                QuestionOption(id = "demo-q30-a", label = "Triangle", isCorrect = false, displayOrder = 1, visualLabel = "shape:triangle"),
                QuestionOption(id = "demo-q30-b", label = "Square", isCorrect = false, displayOrder = 2, visualLabel = "shape:square"),
                QuestionOption(id = "demo-q30-c", label = "Circle", isCorrect = true, displayOrder = 3, visualLabel = "shape:circle"),
                QuestionOption(id = "demo-q30-d", label = "Pentagon", isCorrect = false, displayOrder = 4, visualLabel = "shape:pentagon"),
            ),
        ),

        // ── COGNITIVE: Analogy ──────────────────────────────────────────
        // Supabase: question_type = "multiple_choice", metadata.cognitive_type = "analogy"
        Question(
            id = "demo-q31", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Complete the analogy:\nHot : Cold :: Day : ?",
            explanation = "Hot is the opposite of Cold. Day is the opposite of Night.",
            difficulty = "medium", timeLimitSeconds = 25,
            metadata = mapOf("cognitive_type" to "analogy"),
            options = listOf(
                QuestionOption(id = "demo-q31-a", label = "Night", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q31-b", label = "Morning", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q31-c", label = "Sun", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q31-d", label = "Light", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── COGNITIVE: Analogy (Visual) ─────────────────────────────────
        Question(
            id = "demo-q32", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "\uD83D\uDD34 is to \u25CB as \uD83D\uDD35 is to ?",
            prompt = "Red fills Circle. Blue fills what?",
            explanation = "Red Circle \u2192 Red fills Circle shape. Blue fills Square shape.",
            difficulty = "medium", timeLimitSeconds = 25,
            metadata = mapOf("cognitive_type" to "visual_analogy"),
            options = listOf(
                QuestionOption(id = "demo-q32-a", label = "Blue Square", isCorrect = true, displayOrder = 1, visualLabel = "combo:blue_square"),
                QuestionOption(id = "demo-q32-b", label = "Red Square", isCorrect = false, displayOrder = 2, visualLabel = "combo:red_square"),
                QuestionOption(id = "demo-q32-c", label = "Blue Diamond", isCorrect = false, displayOrder = 3, visualLabel = "combo:blue_diamond"),
                QuestionOption(id = "demo-q32-d", label = "Green Circle", isCorrect = false, displayOrder = 4, visualLabel = "combo:green_circle"),
            ),
        ),

        // ── COGNITIVE: Coding-Decoding ──────────────────────────────────
        // Supabase: question_type = "multiple_choice", metadata.cognitive_type = "coding"
        Question(
            id = "demo-q33", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "If APPLE is coded as 1-16-16-12-5, how is CAT coded?",
            prompt = "A=1, B=2, C=3 ... Z=26",
            explanation = "C=3, A=1, T=20. So CAT = 3-1-20.",
            difficulty = "hard", timeLimitSeconds = 45,
            metadata = mapOf("cognitive_type" to "coding"),
            options = listOf(
                QuestionOption(id = "demo-q33-a", label = "3-1-20", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q33-b", label = "3-2-20", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q33-c", label = "4-1-20", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q33-d", label = "3-1-19", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── COGNITIVE: Direction Sense ──────────────────────────────────
        Question(
            id = "demo-q34", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "You face North. Turn right. Turn right again. Which direction do you face?",
            explanation = "North \u2192 right = East \u2192 right = South.",
            difficulty = "medium", timeLimitSeconds = 30,
            metadata = mapOf("cognitive_type" to "direction"),
            options = listOf(
                QuestionOption(id = "demo-q34-a", label = "South", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q34-b", label = "East", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q34-c", label = "West", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q34-d", label = "North", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── COGNITIVE: Blood Relation ───────────────────────────────────
        Question(
            id = "demo-q35", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "A is the father of B. B is the sister of C. How is A related to C?",
            explanation = "If B is C's sister, they share parents. A (B's father) is also C's father.",
            difficulty = "hard", timeLimitSeconds = 40,
            metadata = mapOf("cognitive_type" to "blood_relation"),
            options = listOf(
                QuestionOption(id = "demo-q35-a", label = "Father", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q35-b", label = "Uncle", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q35-c", label = "Brother", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q35-d", label = "Grandfather", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── STROOP TEST ─────────────────────────────────────────────────
        // Supabase: question_type = "visual_single_choice", metadata.cognitive_type = "stroop"
        Question(
            id = "demo-q36", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "What COLOR is the word shown in? (Ignore the text!)",
            prompt = "The word 'RED' displayed in BLUE color",
            explanation = "The word says RED but is displayed in BLUE. The answer is Blue.",
            difficulty = "medium", timeLimitSeconds = 10,
            metadata = mapOf("cognitive_type" to "stroop", "word" to "RED", "display_color" to "blue"),
            options = listOf(
                QuestionOption(id = "demo-q36-a", label = "Blue", isCorrect = true, displayOrder = 1, visualLabel = "color:blue"),
                QuestionOption(id = "demo-q36-b", label = "Red", isCorrect = false, displayOrder = 2, visualLabel = "color:red"),
                QuestionOption(id = "demo-q36-c", label = "Green", isCorrect = false, displayOrder = 3, visualLabel = "color:green"),
                QuestionOption(id = "demo-q36-d", label = "Yellow", isCorrect = false, displayOrder = 4, visualLabel = "color:yellow"),
            ),
        ),
        // ── STROOP TEST: Variant 2 ─────────────────────────────────────
        Question(
            id = "demo-q37", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "What COLOR is the word displayed in?",
            prompt = "The word 'GREEN' displayed in ORANGE color",
            explanation = "The word says GREEN but is colored in ORANGE.",
            difficulty = "hard", timeLimitSeconds = 8,
            metadata = mapOf("cognitive_type" to "stroop", "word" to "GREEN", "display_color" to "orange"),
            options = listOf(
                QuestionOption(id = "demo-q37-a", label = "Orange", isCorrect = true, displayOrder = 1, visualLabel = "color:orange"),
                QuestionOption(id = "demo-q37-b", label = "Green", isCorrect = false, displayOrder = 2, visualLabel = "color:green"),
                QuestionOption(id = "demo-q37-c", label = "Blue", isCorrect = false, displayOrder = 3, visualLabel = "color:blue"),
                QuestionOption(id = "demo-q37-d", label = "Red", isCorrect = false, displayOrder = 4, visualLabel = "color:red"),
            ),
        ),

        // ── MATRIX: 3x3 Pattern with shapes ────────────────────────────
        // Supabase: question_type = "matrix", promptConfig.type = "shape_matrix"
        Question(
            id = "demo-q38", questionType = QuestionType.MATRIX,
            title = "Complete the 3\u00D73 pattern grid:",
            prompt = "\u25B3 \u25CB \u25A1\n\u25CB \u25A1 \u25B3\n\u25A1 \u25B3 ?",
            explanation = "Each row has Triangle, Circle, Square. Missing = Circle.",
            difficulty = "medium", timeLimitSeconds = 30,
            promptConfig = mapOf("type" to "shape_matrix", "grid_size" to 3),
            options = listOf(
                QuestionOption(id = "demo-q38-a", label = "Circle", isCorrect = true, displayOrder = 1, visualLabel = "shape:circle"),
                QuestionOption(id = "demo-q38-b", label = "Square", isCorrect = false, displayOrder = 2, visualLabel = "shape:square"),
                QuestionOption(id = "demo-q38-c", label = "Triangle", isCorrect = false, displayOrder = 3, visualLabel = "shape:triangle"),
                QuestionOption(id = "demo-q38-d", label = "Star", isCorrect = false, displayOrder = 4, visualLabel = "shape:star"),
            ),
        ),
        // ── MATRIX: Color Matrix ────────────────────────────────────────
        Question(
            id = "demo-q39", questionType = QuestionType.MATRIX,
            title = "Find the missing color in the matrix:",
            prompt = "\uD83D\uDD34 \uD83D\uDD35 \uD83D\uDFE2\n\uD83D\uDD35 \uD83D\uDFE2 \uD83D\uDD34\n\uD83D\uDFE2 ? \uD83D\uDD35",
            explanation = "Each row and column has Red, Blue, Green exactly once. Missing = Red.",
            difficulty = "hard", timeLimitSeconds = 35,
            promptConfig = mapOf("type" to "color_matrix"),
            options = listOf(
                QuestionOption(id = "demo-q39-a", label = "Red", isCorrect = true, displayOrder = 1, visualLabel = "\uD83D\uDD34"),
                QuestionOption(id = "demo-q39-b", label = "Blue", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDD35"),
                QuestionOption(id = "demo-q39-c", label = "Green", isCorrect = false, displayOrder = 3, visualLabel = "\uD83D\uDFE2"),
                QuestionOption(id = "demo-q39-d", label = "Yellow", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDFE1"),
            ),
        ),

        // ── GRID: Cell Selection ────────────────────────────────────────
        // Supabase: question_type = "grid_pattern", promptConfig.type = "grid_select"
        Question(
            id = "demo-q40", questionType = QuestionType.GRID_PATTERN,
            title = "Which cell completes the diagonal pattern?",
            prompt = "\u2B1B \u2B1C \u2B1C\n\u2B1C \u2B1B \u2B1C\n\u2B1C \u2B1C ?",
            explanation = "The pattern fills the main diagonal (top-left to bottom-right).",
            difficulty = "easy", timeLimitSeconds = 20,
            promptConfig = mapOf("type" to "grid_select", "grid" to "3x3"),
            options = listOf(
                QuestionOption(id = "demo-q40-a", label = "\u2B1B (filled)", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q40-b", label = "\u2B1C (empty)", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q40-c", label = "\uD83D\uDFE6 (blue)", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q40-d", label = "\uD83D\uDFE5 (red)", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── TABLE DATA: Bar Chart Reading ───────────────────────────────
        // Supabase: question_type = "table_data", promptConfig.type = "chart_reading"
        Question(
            id = "demo-q41", questionType = QuestionType.TABLE_DATA,
            title = "From the sales data, which month had the highest growth?",
            prompt = "Month | Sales (\u20B9)\nJan | 10,000\nFeb | 15,000\nMar | 12,000\nApr | 22,000",
            explanation = "April had the highest sales at \u20B922,000.",
            difficulty = "easy", timeLimitSeconds = 25,
            promptConfig = mapOf("type" to "table_reading"),
            options = listOf(
                QuestionOption(id = "demo-q41-a", label = "April", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q41-b", label = "February", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q41-c", label = "March", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q41-d", label = "January", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── TABLE DATA: Average Calculation ─────────────────────────────
        Question(
            id = "demo-q42", questionType = QuestionType.TABLE_DATA,
            title = "Calculate the average score from the data:",
            prompt = "Subject | Score\nMath | 80\nScience | 90\nEnglish | 70\nHindi | 60",
            explanation = "Average = (80 + 90 + 70 + 60) / 4 = 300 / 4 = 75.",
            difficulty = "medium", timeLimitSeconds = 35,
            promptConfig = mapOf("type" to "calculation"),
            options = listOf(
                QuestionOption(id = "demo-q42-a", label = "75", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q42-b", label = "70", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q42-c", label = "80", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q42-d", label = "85", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── STATEMENT & REASON: Science ─────────────────────────────────
        Question(
            id = "demo-q43", questionType = QuestionType.STATEMENT_REASON,
            title = "Statement: Ice floats on water.\nReason: The density of ice is less than that of water.",
            prompt = "Evaluate the statement and its reason:",
            explanation = "Both are true, and the reason correctly explains why ice floats.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q43-a", label = "Both true, reason is correct explanation", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q43-b", label = "Both true, reason is NOT correct explanation", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q43-c", label = "Statement true, reason false", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q43-d", label = "Statement false, reason true", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
        // ── STATEMENT & REASON: Math ────────────────────────────────────
        Question(
            id = "demo-q44", questionType = QuestionType.STATEMENT_REASON,
            title = "Statement: Zero is a natural number.\nReason: Natural numbers start from 1.",
            prompt = "Evaluate the statement and its reason:",
            explanation = "Statement is false (0 is not a natural number). Reason is true.",
            difficulty = "hard", timeLimitSeconds = 35,
            options = listOf(
                QuestionOption(id = "demo-q44-a", label = "Statement false, reason true", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q44-b", label = "Both true", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q44-c", label = "Both false", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q44-d", label = "Statement true, reason false", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── VISUAL: Rotation Match ──────────────────────────────────────
        // Supabase: question_type = "visual_single_choice", metadata.cognitive_type = "rotation"
        Question(
            id = "demo-q45", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "If this arrow \u2B06 is rotated 90\u00B0 clockwise, what does it become?",
            explanation = "Rotating \u2B06 by 90\u00B0 clockwise gives \u27A1 (arrow right).",
            difficulty = "easy", timeLimitSeconds = 15,
            metadata = mapOf("cognitive_type" to "rotation", "degrees" to 90),
            options = listOf(
                QuestionOption(id = "demo-q45-a", label = "Arrow Right", isCorrect = true, displayOrder = 1, visualLabel = "shape:arrow_right"),
                QuestionOption(id = "demo-q45-b", label = "Arrow Up", isCorrect = false, displayOrder = 2, visualLabel = "shape:arrow_up"),
                QuestionOption(id = "demo-q45-c", label = "Diamond", isCorrect = false, displayOrder = 3, visualLabel = "shape:diamond"),
                QuestionOption(id = "demo-q45-d", label = "Semicircle", isCorrect = false, displayOrder = 4, visualLabel = "shape:semicircle"),
            ),
        ),

        // ── VISUAL: Mirror Image ────────────────────────────────────────
        Question(
            id = "demo-q46", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the mirror image of the word 'AMBULANCE'?",
            prompt = "Imagine looking at the word in a mirror",
            explanation = "In a mirror, text appears reversed. AMBULANCE becomes ECNALUBMA.",
            difficulty = "hard", timeLimitSeconds = 30,
            metadata = mapOf("cognitive_type" to "mirror_image"),
            options = listOf(
                QuestionOption(id = "demo-q46-a", label = "ECNALUBMA", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q46-b", label = "AMBULANCE", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q46-c", label = "ACNALUBME", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q46-d", label = "ECNALUBAM", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── ORDERING: Process Steps ─────────────────────────────────────
        Question(
            id = "demo-q47", questionType = QuestionType.ORDERING,
            title = "Arrange the water cycle steps in order:",
            prompt = "Tap two items to swap",
            explanation = "Evaporation \u2192 Condensation \u2192 Precipitation \u2192 Collection.",
            difficulty = "medium", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "demo-q47-a", label = "\uD83D\uDCA7 Precipitation", isCorrect = false, displayOrder = 1, correctPosition = 3),
                QuestionOption(id = "demo-q47-b", label = "\u2600\uFE0F Evaporation", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "demo-q47-c", label = "\uD83C\uDF0A Collection", isCorrect = false, displayOrder = 3, correctPosition = 4),
                QuestionOption(id = "demo-q47-d", label = "\u2601\uFE0F Condensation", isCorrect = false, displayOrder = 4, correctPosition = 2),
            ),
        ),

        // ── ORDERING: Size (Smallest to Largest) ────────────────────────
        Question(
            id = "demo-q48", questionType = QuestionType.ORDERING,
            title = "Arrange from smallest to largest:",
            prompt = "Tap two items to swap",
            explanation = "Ant < Cat < Horse < Elephant.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "demo-q48-a", label = "\uD83D\uDC18 Elephant", isCorrect = false, displayOrder = 1, correctPosition = 4),
                QuestionOption(id = "demo-q48-b", label = "\uD83D\uDC1C Ant", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "demo-q48-c", label = "\uD83D\uDC34 Horse", isCorrect = false, displayOrder = 3, correctPosition = 3),
                QuestionOption(id = "demo-q48-d", label = "\uD83D\uDC31 Cat", isCorrect = false, displayOrder = 4, correctPosition = 2),
            ),
        ),

        // ── MATCH: Animal Babies ────────────────────────────────────────
        Question(
            id = "demo-q49", questionType = QuestionType.MATCH,
            title = "Match each animal to its baby:",
            prompt = "Tap an animal, then tap its baby name",
            explanation = "Dog\u2192Puppy, Cat\u2192Kitten, Cow\u2192Calf, Horse\u2192Foal.",
            difficulty = "easy", timeLimitSeconds = 45,
            matchPairs = listOf(
                MatchPair(id = "demo-q49-m1", leftText = "\uD83D\uDC15 Dog", rightText = "Puppy", displayOrder = 1),
                MatchPair(id = "demo-q49-m2", leftText = "\uD83D\uDC08 Cat", rightText = "Kitten", displayOrder = 2),
                MatchPair(id = "demo-q49-m3", leftText = "\uD83D\uDC04 Cow", rightText = "Calf", displayOrder = 3),
                MatchPair(id = "demo-q49-m4", leftText = "\uD83D\uDC34 Horse", rightText = "Foal", displayOrder = 4),
            ),
        ),

        // ── MATCH: Math Operations ──────────────────────────────────────
        Question(
            id = "demo-q50", questionType = QuestionType.MATCH,
            title = "Match each expression to its result:",
            prompt = "Tap an expression, then tap the answer",
            explanation = "5\u00D76=30, 8+7=15, 20-8=12, 36\u00F74=9.",
            difficulty = "medium", timeLimitSeconds = 50,
            matchPairs = listOf(
                MatchPair(id = "demo-q50-m1", leftText = "5 \u00D7 6", rightText = "30", displayOrder = 1),
                MatchPair(id = "demo-q50-m2", leftText = "8 + 7", rightText = "15", displayOrder = 2),
                MatchPair(id = "demo-q50-m3", leftText = "20 \u2212 8", rightText = "12", displayOrder = 3),
                MatchPair(id = "demo-q50-m4", leftText = "36 \u00F7 4", rightText = "9", displayOrder = 4),
            ),
        ),

        // ── FILL BLANK: Analogy Completion ──────────────────────────────
        Question(
            id = "demo-q51", questionType = QuestionType.FILL_BLANK,
            title = "Pen is to Write as Knife is to ____.",
            prompt = "Complete the analogy",
            explanation = "A pen is used to write, a knife is used to cut.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "demo-q51-a", label = "Cut", isCorrect = true, displayOrder = 1),
            ),
        ),

        // ── FILL BLANK: Number Pattern ──────────────────────────────────
        Question(
            id = "demo-q52", questionType = QuestionType.FILL_BLANK,
            title = "3, 6, 12, 24, ____",
            prompt = "Type the next number in the pattern",
            explanation = "Each number doubles: 24 \u00D7 2 = 48.",
            difficulty = "medium", timeLimitSeconds = 25,
            options = listOf(
                QuestionOption(id = "demo-q52-a", label = "48", isCorrect = true, displayOrder = 1),
            ),
        ),

        // ── SELECT WORD: Nouns ──────────────────────────────────────────
        Question(
            id = "demo-q53", questionType = QuestionType.SELECT_WORD,
            title = "Select ALL the NOUNS in the sentence:",
            prompt = "The brave soldier crossed the wide river at dawn.",
            explanation = "Soldier, river, and dawn are nouns.",
            difficulty = "medium", timeLimitSeconds = 30,
            allowMultiple = true,
            options = listOf(
                QuestionOption(id = "demo-q53-a", label = "The", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "demo-q53-b", label = "brave", isCorrect = false, displayOrder = 2),
                QuestionOption(id = "demo-q53-c", label = "soldier", isCorrect = true, displayOrder = 3),
                QuestionOption(id = "demo-q53-d", label = "crossed", isCorrect = false, displayOrder = 4),
                QuestionOption(id = "demo-q53-e", label = "the", isCorrect = false, displayOrder = 5),
                QuestionOption(id = "demo-q53-f", label = "wide", isCorrect = false, displayOrder = 6),
                QuestionOption(id = "demo-q53-g", label = "river", isCorrect = true, displayOrder = 7),
                QuestionOption(id = "demo-q53-h", label = "at", isCorrect = false, displayOrder = 8),
                QuestionOption(id = "demo-q53-i", label = "dawn", isCorrect = true, displayOrder = 9),
            ),
        ),

        // ── TRUE/FALSE: Cognitive ───────────────────────────────────────
        Question(
            id = "demo-q54", questionType = QuestionType.TRUE_FALSE,
            title = "All birds can fly.",
            explanation = "False! Penguins, ostriches, and kiwis are birds that cannot fly.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "demo-q54-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "demo-q54-b", label = "False", isCorrect = true, displayOrder = 2),
            ),
        ),

        // ── VISUAL: Color Combo Pattern ─────────────────────────────────
        Question(
            id = "demo-q55", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which colored shape completes: \uD83D\uDD34\u25B3 \uD83D\uDD35\u25A1 \uD83D\uDFE2\u25CB ?",
            prompt = "Red Triangle, Blue Square, Green Circle... What follows?",
            explanation = "The pattern cycles colors with shapes. Next would be Yellow Diamond.",
            difficulty = "hard", timeLimitSeconds = 25,
            metadata = mapOf("cognitive_type" to "pattern_combo"),
            options = listOf(
                QuestionOption(id = "demo-q55-a", label = "Yellow Diamond", isCorrect = true, displayOrder = 1, visualLabel = "combo:yellow_diamond"),
                QuestionOption(id = "demo-q55-b", label = "Red Star", isCorrect = false, displayOrder = 2, visualLabel = "combo:red_star"),
                QuestionOption(id = "demo-q55-c", label = "Blue Diamond", isCorrect = false, displayOrder = 3, visualLabel = "combo:blue_diamond"),
                QuestionOption(id = "demo-q55-d", label = "Purple Hexagon", isCorrect = false, displayOrder = 4, visualLabel = "combo:purple_hexagon"),
            ),
        ),

        // ── COGNITIVE: Logical Deduction ────────────────────────────────
        Question(
            id = "demo-q56", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "All roses are flowers. Some flowers fade quickly. Therefore:",
            explanation = "We can only conclude that some roses may fade quickly.",
            difficulty = "hard", timeLimitSeconds = 40,
            metadata = mapOf("cognitive_type" to "syllogism"),
            options = listOf(
                QuestionOption(id = "demo-q56-a", label = "Some roses may fade quickly", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q56-b", label = "All roses fade quickly", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q56-c", label = "No roses fade quickly", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q56-d", label = "Flowers are roses", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── PATTERN: Alternating Increase ───────────────────────────────
        Question(
            id = "demo-q57", questionType = QuestionType.GRID_PATTERN,
            title = "What comes next?",
            prompt = "1 \u2192 3 \u2192 2 \u2192 4 \u2192 3 \u2192 5 \u2192 4 \u2192 ?",
            explanation = "Two interleaved sequences: 1,2,3,4 and 3,4,5,6. Next = 6.",
            difficulty = "hard", timeLimitSeconds = 35,
            promptConfig = mapOf("type" to "interleaved_sequence"),
            options = listOf(
                QuestionOption(id = "demo-q57-a", label = "6", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q57-b", label = "5", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q57-c", label = "7", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q57-d", label = "3", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── MATRIX: Arithmetic Grid ─────────────────────────────────────
        Question(
            id = "demo-q58", questionType = QuestionType.MATRIX,
            title = "Find the missing number:",
            prompt = "3   5   7\n6   10  14\n9   ?   21",
            explanation = "Column 1: 3,6,9 (+3). Column 2: 5,10,? (+5) \u2192 15. Column 3: 7,14,21 (+7).",
            difficulty = "hard", timeLimitSeconds = 40,
            options = listOf(
                QuestionOption(id = "demo-q58-a", label = "15", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q58-b", label = "12", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q58-c", label = "14", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q58-d", label = "18", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── MCQ: Lateral Thinking ───────────────────────────────────────
        Question(
            id = "demo-q59", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "A man pushes his car to a hotel and loses all his money. What happened?",
            explanation = "He was playing Monopoly!",
            difficulty = "hard", timeLimitSeconds = 30,
            metadata = mapOf("cognitive_type" to "lateral_thinking"),
            options = listOf(
                QuestionOption(id = "demo-q59-a", label = "He was playing Monopoly", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q59-b", label = "He had an accident", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q59-c", label = "He was robbed", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q59-d", label = "His car broke down", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),

        // ── MEMORY: Emoji Pair Recall ───────────────────────────────────
        Question(
            id = "demo-q60", questionType = QuestionType.MEMORY,
            title = "Study these pairs, then answer: What was paired with \uD83C\uDF1F?",
            prompt = "\uD83C\uDF1F\u2194\uD83C\uDF88  \uD83C\uDF19\u2194\uD83C\uDFB5  \uD83C\uDF08\u2194\uD83C\uDFAF  \uD83D\uDD14\u2194\uD83C\uDFAA",
            explanation = "Star (\uD83C\uDF1F) was paired with Balloon (\uD83C\uDF88).",
            difficulty = "hard", timeLimitSeconds = 20,
            promptConfig = mapOf("type" to "pair_recall", "display_time_ms" to 5000),
            options = listOf(
                QuestionOption(id = "demo-q60-a", label = "\uD83C\uDF88 Balloon", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "demo-q60-b", label = "\uD83C\uDFB5 Music", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "demo-q60-c", label = "\uD83C\uDFAF Target", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "demo-q60-d", label = "\uD83C\uDFAA Circus", isCorrect = false, displayOrder = 4, visualLabel = "D"),
            ),
        ),
    )

    // ── Question types: Multiple Choice ────────────────────────────────

    private fun mockMcqQuestions(quizId: String): List<Question> = listOf(
        Question(
            id = "$quizId-q1", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is 15 + 27?",
            explanation = "15 + 27 = 42.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "42", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "41", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "43", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "40", isCorrect = false, displayOrder = 4, visualLabel = "D")
            )
        ),
        Question(
            id = "$quizId-q2", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which planet is known as the Red Planet?",
            explanation = "Mars is called the Red Planet due to iron oxide on its surface.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "Venus", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "Mars", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "Jupiter", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "Saturn", isCorrect = false, displayOrder = 4, visualLabel = "D")
            )
        ),
        Question(
            id = "$quizId-q3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the largest ocean on Earth?",
            explanation = "The Pacific Ocean is the largest ocean.", difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Atlantic Ocean", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "Indian Ocean", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "Pacific Ocean", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "Arctic Ocean", isCorrect = false, displayOrder = 4, visualLabel = "D")
            )
        ),
        Question(
            id = "$quizId-q4", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "How many sides does a hexagon have?",
            explanation = "A hexagon has 6 sides.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "5", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "6", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "7", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "8", isCorrect = false, displayOrder = 4, visualLabel = "D")
            )
        ),
        Question(
            id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is 3/4 expressed as a decimal?",
            explanation = "3 / 4 = 0.75.", difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "0.25", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "0.50", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "0.75", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "0.34", isCorrect = false, displayOrder = 4, visualLabel = "D")
            )
        )
    )

    // ── Question types: True/False ─────────────────────────────────────

    private fun mockTrueFalseQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.TRUE_FALSE,
            title = "The Earth revolves around the Sun.",
            explanation = "True! The Earth orbits the Sun once every 365.25 days.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "True", isCorrect = true, displayOrder = 1),
                QuestionOption(id = "$quizId-q1-b", label = "False", isCorrect = false, displayOrder = 2))),
        Question(id = "$quizId-q2", questionType = QuestionType.TRUE_FALSE,
            title = "Water boils at 50 degrees Celsius.",
            explanation = "False! Water boils at 100 degrees Celsius.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q2-b", label = "False", isCorrect = true, displayOrder = 2))),
        Question(id = "$quizId-q3", questionType = QuestionType.TRUE_FALSE,
            title = "A triangle has 4 sides.",
            explanation = "False! A triangle has 3 sides.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q3-b", label = "False", isCorrect = true, displayOrder = 2))),
        Question(id = "$quizId-q4", questionType = QuestionType.TRUE_FALSE,
            title = "The chemical symbol for Gold is Au.",
            explanation = "True! Au comes from the Latin word 'Aurum'.",
            difficulty = "medium", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "True", isCorrect = true, displayOrder = 1),
                QuestionOption(id = "$quizId-q4-b", label = "False", isCorrect = false, displayOrder = 2))),
        Question(id = "$quizId-q5", questionType = QuestionType.TRUE_FALSE,
            title = "The Amazon is the longest river in the world.",
            explanation = "False! The Nile is generally considered the longest river.",
            difficulty = "medium", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q5-b", label = "False", isCorrect = true, displayOrder = 2)))
    )

    // ── Question types: Fill in the Blank ──────────────────────────────

    private fun mockFillBlankQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.FILL_BLANK,
            title = "The capital of India is ____.",
            prompt = "Select the correct answer", explanation = "New Delhi is the capital of India.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "New Delhi", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "Mumbai", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "Kolkata", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "Chennai", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q2", questionType = QuestionType.FILL_BLANK,
            title = "7 x 8 = ____",
            prompt = "Calculate the product", explanation = "7 x 8 = 56.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "56", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "54", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "48", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "63", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.FILL_BLANK,
            title = "The Sun rises in the ____.",
            prompt = "Fill in the direction", explanation = "The Sun rises in the East.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "East", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "West", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "North", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "South", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.FILL_BLANK,
            title = "H2O is the chemical formula for ____.",
            prompt = "What substance?", explanation = "H2O is the chemical formula for Water.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "Water", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "Oxygen", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "Hydrogen", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "Carbon", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.FILL_BLANK,
            title = "There are ____ continents on Earth.",
            prompt = "How many?", explanation = "There are 7 continents.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "7", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "5", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "6", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "8", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Ordering ───────────────────────────────────────

    private fun mockOrderingQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.ORDERING,
            title = "Arrange these numbers in ascending order:",
            prompt = "Drag to reorder from smallest to largest",
            explanation = "Correct order: 12, 25, 38, 47.",
            difficulty = "easy", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "38", isCorrect = false, displayOrder = 1, correctPosition = 3),
                QuestionOption(id = "$quizId-q1-b", label = "12", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "$quizId-q1-c", label = "47", isCorrect = false, displayOrder = 3, correctPosition = 4),
                QuestionOption(id = "$quizId-q1-d", label = "25", isCorrect = false, displayOrder = 4, correctPosition = 2))),
        Question(id = "$quizId-q2", questionType = QuestionType.ORDERING,
            title = "Arrange planets by distance from the Sun (nearest first):",
            prompt = "Drag to reorder",
            explanation = "Mercury, Venus, Earth, Mars.",
            difficulty = "medium", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "Earth", isCorrect = false, displayOrder = 1, correctPosition = 3),
                QuestionOption(id = "$quizId-q2-b", label = "Mercury", isCorrect = false, displayOrder = 2, correctPosition = 1),
                QuestionOption(id = "$quizId-q2-c", label = "Mars", isCorrect = false, displayOrder = 3, correctPosition = 4),
                QuestionOption(id = "$quizId-q2-d", label = "Venus", isCorrect = false, displayOrder = 4, correctPosition = 2))),
        Question(id = "$quizId-q3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is 0.5 + 0.25?", explanation = "0.5 + 0.25 = 0.75",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "0.75", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "0.70", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "0.80", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "1.00", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which fraction equals 0.5?", explanation = "0.5 = 1/2",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "1/3", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "1/2", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "1/4", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "2/3", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Convert 3/5 to a decimal.", explanation = "3 / 5 = 0.6",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "0.35", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "0.6", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "0.53", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "0.65", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Match ──────────────────────────────────────────

    private fun mockMatchQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.MATCH,
            title = "Match the countries with their capitals:",
            prompt = "Draw lines to match",
            explanation = "India-New Delhi, Japan-Tokyo, France-Paris, Egypt-Cairo",
            difficulty = "medium", timeLimitSeconds = 60,
            matchPairs = listOf(
                MatchPair(id = "$quizId-q1-m1", leftText = "India", rightText = "New Delhi", displayOrder = 1),
                MatchPair(id = "$quizId-q1-m2", leftText = "Japan", rightText = "Tokyo", displayOrder = 2),
                MatchPair(id = "$quizId-q1-m3", leftText = "France", rightText = "Paris", displayOrder = 3),
                MatchPair(id = "$quizId-q1-m4", leftText = "Egypt", rightText = "Cairo", displayOrder = 4))),
        Question(id = "$quizId-q2", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the capital of Australia?",
            explanation = "Canberra is the capital of Australia.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "Sydney", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "Melbourne", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "Canberra", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "Perth", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which gas do plants absorb?",
            explanation = "Plants absorb carbon dioxide (CO2) during photosynthesis.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Oxygen", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "Nitrogen", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "Carbon Dioxide", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "Hydrogen", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.TRUE_FALSE,
            title = "Photosynthesis occurs in the roots of plants.",
            explanation = "False! Photosynthesis mainly occurs in the leaves.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q4-b", label = "False", isCorrect = true, displayOrder = 2))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which vitamin is produced from sunlight exposure?",
            explanation = "Vitamin D is produced when skin is exposed to sunlight.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "Vitamin A", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "Vitamin B", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "Vitamin C", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "Vitamin D", isCorrect = true, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Select Word ────────────────────────────────────

    private fun mockSelectWordQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.SELECT_WORD,
            title = "Select the VERB: 'The cat jumped over the fence.'",
            prompt = "Tap the correct word",
            explanation = "'Jumped' is the verb.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "cat", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q1-b", label = "jumped", isCorrect = true, displayOrder = 2),
                QuestionOption(id = "$quizId-q1-c", label = "over", isCorrect = false, displayOrder = 3),
                QuestionOption(id = "$quizId-q1-d", label = "fence", isCorrect = false, displayOrder = 4))),
        Question(id = "$quizId-q2", questionType = QuestionType.SELECT_WORD,
            title = "Select the ADJECTIVE: 'She wore a beautiful dress.'",
            prompt = "Tap the correct word",
            explanation = "'Beautiful' is the adjective.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "She", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q2-b", label = "wore", isCorrect = false, displayOrder = 2),
                QuestionOption(id = "$quizId-q2-c", label = "beautiful", isCorrect = true, displayOrder = 3),
                QuestionOption(id = "$quizId-q2-d", label = "dress", isCorrect = false, displayOrder = 4))),
        Question(id = "$quizId-q3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which is a NOUN?", explanation = "'Garden' is a noun.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "quickly", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "run", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "garden", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "happily", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which word is an ADVERB?", explanation = "'Quickly' is an adverb.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "happy", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "quickly", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "book", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "the", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Identify the PREPOSITION: 'The book is on the table.'",
            explanation = "'On' is a preposition.", difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "book", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "is", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "on", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "table", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Statement & Reason ─────────────────────────────

    private fun mockStatementReasonQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.STATEMENT_REASON,
            title = "Statement: All squares are rectangles.\nReason: A rectangle has four right angles.",
            prompt = "Are both correct? Is the reason the correct explanation?",
            explanation = "Both are true, and the reason correctly explains why.",
            difficulty = "hard", timeLimitSeconds = 45,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "Both true, reason is correct explanation", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "Both true, reason is NOT correct explanation", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "Statement true, reason false", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "Both false", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q2", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the plural of 'child'?", explanation = "'Children' is the plural.",
            difficulty = "easy", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "childs", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "children", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "childrens", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "child's", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Choose the correct sentence:",
            explanation = "'She doesn't have any books' is correct.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "She don't have no books", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "She doesn't have any books", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "She don't has any books", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "She doesn't has no books", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.TRUE_FALSE,
            title = "'Their', 'there', and 'they're' all mean the same thing.",
            explanation = "False! 'Their' = possession, 'there' = place, 'they're' = they are.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "True", isCorrect = false, displayOrder = 1),
                QuestionOption(id = "$quizId-q4-b", label = "False", isCorrect = true, displayOrder = 2))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which sentence uses the correct tense?",
            explanation = "'I have been waiting for an hour' is correct.",
            difficulty = "hard", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "I am waiting since an hour", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "I have been waiting for an hour", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "I was waiting from an hour", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "I waited since an hour", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Visual Single Choice ───────────────────────────

    private fun mockVisualChoiceQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which shape has exactly 4 equal sides?",
            explanation = "A square has 4 equal sides and 4 right angles.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "Triangle", isCorrect = false, displayOrder = 1, visualLabel = "shape:triangle"),
                QuestionOption(id = "$quizId-q1-b", label = "Square", isCorrect = true, displayOrder = 2, visualLabel = "shape:square"),
                QuestionOption(id = "$quizId-q1-c", label = "Circle", isCorrect = false, displayOrder = 3, visualLabel = "shape:circle"),
                QuestionOption(id = "$quizId-q1-d", label = "Star", isCorrect = false, displayOrder = 4, visualLabel = "shape:star"))),
        Question(id = "$quizId-q2", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which animal can fly?",
            explanation = "Birds like eagles can fly using their wings.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "Dog", isCorrect = false, displayOrder = 1, visualLabel = "\uD83D\uDC36"),
                QuestionOption(id = "$quizId-q2-b", label = "Fish", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDC1F"),
                QuestionOption(id = "$quizId-q2-c", label = "Eagle", isCorrect = true, displayOrder = 3, visualLabel = "\uD83E\uDD85"),
                QuestionOption(id = "$quizId-q2-d", label = "Cat", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDC31"))),
        Question(id = "$quizId-q3", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which is a musical instrument?",
            explanation = "The guitar is a stringed musical instrument.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Book", isCorrect = false, displayOrder = 1, visualLabel = "\uD83D\uDCD6"),
                QuestionOption(id = "$quizId-q3-b", label = "Guitar", isCorrect = true, displayOrder = 2, visualLabel = "\uD83C\uDFB8"),
                QuestionOption(id = "$quizId-q3-c", label = "Cup", isCorrect = false, displayOrder = 3, visualLabel = "\u2615"),
                QuestionOption(id = "$quizId-q3-d", label = "Pencil", isCorrect = false, displayOrder = 4, visualLabel = "\u270F\uFE0F"))),
        Question(id = "$quizId-q4", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which season comes after summer?",
            explanation = "Autumn (fall) comes after summer in the seasonal cycle.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "Winter", isCorrect = false, displayOrder = 1, visualLabel = "\u2744\uFE0F"),
                QuestionOption(id = "$quizId-q4-b", label = "Spring", isCorrect = false, displayOrder = 2, visualLabel = "\uD83C\uDF38"),
                QuestionOption(id = "$quizId-q4-c", label = "Autumn", isCorrect = true, displayOrder = 3, visualLabel = "\uD83C\uDF42"),
                QuestionOption(id = "$quizId-q4-d", label = "Monsoon", isCorrect = false, displayOrder = 4, visualLabel = "\uD83C\uDF27\uFE0F"))),
        Question(id = "$quizId-q5", questionType = QuestionType.VISUAL_SINGLE_CHOICE,
            title = "Which planet is closest to the Sun?",
            explanation = "Mercury is the closest planet to the Sun.",
            difficulty = "medium", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "Venus", isCorrect = false, displayOrder = 1, visualLabel = "\uD83C\uDF11"),
                QuestionOption(id = "$quizId-q5-b", label = "Earth", isCorrect = false, displayOrder = 2, visualLabel = "\uD83C\uDF0D"),
                QuestionOption(id = "$quizId-q5-c", label = "Mercury", isCorrect = true, displayOrder = 3, visualLabel = "\u2B50"),
                QuestionOption(id = "$quizId-q5-d", label = "Mars", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDD34")))
    )

    // ── Question types: Matrix ──────────────────────────────────────────

    private fun mockMatrixQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.MATRIX,
            title = "Find the missing number in the matrix",
            prompt = "2  4  6\n8  ?  12\n14 16 18",
            explanation = "The numbers increase by 2 in each row. So the missing number is 10.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "10", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "11", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "9", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "13", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q2", questionType = QuestionType.MATRIX,
            title = "What number replaces the question mark?",
            prompt = "1  3  5\n7  9  11\n13 ?  17",
            explanation = "All numbers are odd. 13 + 2 = 15.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "14", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "15", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "16", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "12", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.MATRIX,
            title = "Complete the multiplication pattern",
            prompt = "3  6  9\n4  8  12\n5  10 ?",
            explanation = "Each row shows multiples: 5 x 3 = 15.",
            difficulty = "hard", timeLimitSeconds = 35,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "13", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "14", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "15", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "20", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is 25 x 4?",
            explanation = "25 x 4 = 100.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "90", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "100", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "110", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "125", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.MATRIX,
            title = "Find the missing value",
            prompt = "10 20 30\n40 50 60\n70 80 ?",
            explanation = "Numbers increase by 10. So the missing value is 90.",
            difficulty = "easy", timeLimitSeconds = 25,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "85", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "90", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "95", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "100", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Grid Pattern ────────────────────────────────────

    private fun mockGridPatternQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.GRID_PATTERN,
            title = "What comes next in the pattern?",
            prompt = "\uD83D\uDD34 \uD83D\uDD35 \uD83D\uDD34 \uD83D\uDD35 \uD83D\uDD34 ?",
            explanation = "Red and blue alternate, so the next is blue.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "Blue", isCorrect = true, displayOrder = 1, visualLabel = "\uD83D\uDD35"),
                QuestionOption(id = "$quizId-q1-b", label = "Red", isCorrect = false, displayOrder = 2, visualLabel = "\uD83D\uDD34"),
                QuestionOption(id = "$quizId-q1-c", label = "Green", isCorrect = false, displayOrder = 3, visualLabel = "\uD83D\uDFE2"),
                QuestionOption(id = "$quizId-q1-d", label = "Yellow", isCorrect = false, displayOrder = 4, visualLabel = "\uD83D\uDFE1"))),
        Question(id = "$quizId-q2", questionType = QuestionType.GRID_PATTERN,
            title = "Complete the number pattern",
            prompt = "2 4 8 16 ?",
            explanation = "Each number doubles: 16 x 2 = 32.",
            difficulty = "medium", timeLimitSeconds = 25,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "24", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "32", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "20", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "30", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.GRID_PATTERN,
            title = "What shape completes the pattern?",
            prompt = "\u25B2 \u25CF \u25A0 \u25B2 \u25CF ?",
            explanation = "Triangle, Circle, Square repeats. So next is Square.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Triangle", isCorrect = false, displayOrder = 1, visualLabel = "\u25B2"),
                QuestionOption(id = "$quizId-q3-b", label = "Circle", isCorrect = false, displayOrder = 2, visualLabel = "\u25CF"),
                QuestionOption(id = "$quizId-q3-c", label = "Square", isCorrect = true, displayOrder = 3, visualLabel = "\u25A0"),
                QuestionOption(id = "$quizId-q3-d", label = "Star", isCorrect = false, displayOrder = 4, visualLabel = "\u2605"))),
        Question(id = "$quizId-q4", questionType = QuestionType.GRID_PATTERN,
            title = "Find the next number in the series",
            prompt = "1 1 2 3 5 8 ?",
            explanation = "Fibonacci sequence: each number is sum of previous two. 5 + 8 = 13.",
            difficulty = "hard", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "11", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "13", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "10", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "15", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.GRID_PATTERN,
            title = "What emoji completes the pattern?",
            prompt = "\uD83C\uDF1E \uD83C\uDF19 \uD83C\uDF1E \uD83C\uDF19 ?",
            explanation = "Sun and Moon alternate, so next is Sun.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "Sun", isCorrect = true, displayOrder = 1, visualLabel = "\uD83C\uDF1E"),
                QuestionOption(id = "$quizId-q5-b", label = "Moon", isCorrect = false, displayOrder = 2, visualLabel = "\uD83C\uDF19"),
                QuestionOption(id = "$quizId-q5-c", label = "Star", isCorrect = false, displayOrder = 3, visualLabel = "\u2B50"),
                QuestionOption(id = "$quizId-q5-d", label = "Cloud", isCorrect = false, displayOrder = 4, visualLabel = "\u2601\uFE0F")))
    )

    // ── Question types: Table Data ──────────────────────────────────────

    private fun mockTableDataQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.TABLE_DATA,
            title = "Look at the table and answer: Who scored the highest marks?",
            prompt = "Student | Marks\nAarav | 85\nPriya | 92\nRohan | 78\nAnanya | 95",
            explanation = "Ananya scored 95, which is the highest.",
            difficulty = "easy", timeLimitSeconds = 25,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "Aarav", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "Priya", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "Rohan", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "Ananya", isCorrect = true, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q2", questionType = QuestionType.TABLE_DATA,
            title = "Using the table, find the total number of fruits",
            prompt = "Fruit | Count\nApple | 12\nBanana | 8\nOrange | 15\nMango | 5",
            explanation = "12 + 8 + 15 + 5 = 40 fruits in total.",
            difficulty = "medium", timeLimitSeconds = 30,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "35", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "40", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "45", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "38", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.TABLE_DATA,
            title = "Which city has the highest temperature?",
            prompt = "City | Temperature\nDelhi | 38\u00B0C\nMumbai | 33\u00B0C\nChennai | 36\u00B0C\nKolkata | 35\u00B0C",
            explanation = "Delhi has the highest temperature at 38\u00B0C.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Delhi", isCorrect = true, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "Mumbai", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "Chennai", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "Kolkata", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.TABLE_DATA,
            title = "What is the difference between the tallest and shortest student?",
            prompt = "Student | Height (cm)\nRahul | 145\nSneha | 138\nAmit | 152\nKavya | 141",
            explanation = "Tallest is Amit (152) and shortest is Sneha (138). Difference = 152 - 138 = 14 cm.",
            difficulty = "medium", timeLimitSeconds = 35,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "12 cm", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "14 cm", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "10 cm", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "16 cm", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "How many days are there in February in a leap year?",
            explanation = "A leap year has 29 days in February.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "28", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "29", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "30", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "31", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Question types: Memory ──────────────────────────────────────────

    private fun mockMemoryQuestions(quizId: String): List<Question> = listOf(
        Question(id = "$quizId-q1", questionType = QuestionType.MEMORY,
            title = "Which item was NOT in the group shown?",
            prompt = "\uD83C\uDF4E \uD83C\uDF3A \uD83D\uDCDA \u2B50 \uD83C\uDFB5",
            explanation = "The target emoji was not in the displayed group.",
            difficulty = "easy", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q1-a", label = "Apple \uD83C\uDF4E", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q1-b", label = "Flower \uD83C\uDF3A", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q1-c", label = "Trophy \uD83C\uDFC6", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q1-d", label = "Star \u2B50", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q2", questionType = QuestionType.MEMORY,
            title = "How many items were shown in the group?",
            prompt = "\uD83D\uDC36 \uD83D\uDC31 \uD83D\uDC30 \uD83D\uDC2F",
            explanation = "There were 4 animals: dog, cat, rabbit, and tiger.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q2-a", label = "3", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q2-b", label = "4", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q2-c", label = "5", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q2-d", label = "6", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q3", questionType = QuestionType.MEMORY,
            title = "Which color was in the group?",
            prompt = "\uD83D\uDD34 \uD83D\uDFE2 \uD83D\uDD35 \uD83D\uDFE1 \uD83D\uDFE0",
            explanation = "Red, Green, Blue, Yellow, and Orange were all in the group.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q3-a", label = "Purple \uD83D\uDFE3", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q3-b", label = "Brown \uD83D\uDFE4", isCorrect = false, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q3-c", label = "Green \uD83D\uDFE2", isCorrect = true, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q3-d", label = "Black \u26AB", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q4", questionType = QuestionType.MEMORY,
            title = "What was the FIRST item shown?",
            prompt = "\uD83C\uDF1E \uD83C\uDF08 \uD83C\uDF3B \uD83E\uDD8B",
            explanation = "The Sun was the first item in the sequence.",
            difficulty = "medium", timeLimitSeconds = 20,
            options = listOf(
                QuestionOption(id = "$quizId-q4-a", label = "Rainbow \uD83C\uDF08", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q4-b", label = "Sun \uD83C\uDF1E", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q4-c", label = "Flower \uD83C\uDF3B", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q4-d", label = "Butterfly \uD83E\uDD8B", isCorrect = false, displayOrder = 4, visualLabel = "D"))),
        Question(id = "$quizId-q5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the capital of India?",
            explanation = "New Delhi is the capital of India.",
            difficulty = "easy", timeLimitSeconds = 15,
            options = listOf(
                QuestionOption(id = "$quizId-q5-a", label = "Mumbai", isCorrect = false, displayOrder = 1, visualLabel = "A"),
                QuestionOption(id = "$quizId-q5-b", label = "New Delhi", isCorrect = true, displayOrder = 2, visualLabel = "B"),
                QuestionOption(id = "$quizId-q5-c", label = "Bangalore", isCorrect = false, displayOrder = 3, visualLabel = "C"),
                QuestionOption(id = "$quizId-q5-d", label = "Chennai", isCorrect = false, displayOrder = 4, visualLabel = "D")))
    )

    // ── Quiz Result ──────────────────────────────────────────────────────

    fun mockQuizResult(score: Int, total: Int): QuizResult = QuizResult(
        status = "success",
        attemptId = "attempt-mock-001",
        score = score,
        totalQuestions = total,
        xpEarned = score * 10,
        totalXp = 2850L + (score * 10),
        level = 5,
        levelChanged = false,
        rankGlobal = 42,
        isReplay = false,
        nextQuizId = null
    )

    // ── Leaderboard ──────────────────────────────────────────────────────

    fun mockLeaderboard(): LeaderboardData = LeaderboardData(
        rankedUsers = listOf(
            LeaderboardEntry(userId = "user-001", displayName = "Aarav", avatarId = 1, totalXp = 12500L, rank = 1, trend = "+2"),
            LeaderboardEntry(userId = "user-002", displayName = "Priya", avatarId = 5, totalXp = 11200L, rank = 2, trend = "+1"),
            LeaderboardEntry(userId = "user-003", displayName = "Rohan", avatarId = 2, totalXp = 10800L, rank = 3, trend = "-1"),
            LeaderboardEntry(userId = "user-004", displayName = "Ananya", avatarId = 7, totalXp = 9500L, rank = 4, trend = "0"),
            LeaderboardEntry(userId = MOCK_USER_ID, displayName = "Neha", avatarId = 3, totalXp = 2850L, rank = 5, trend = "+3"),
            LeaderboardEntry(userId = "user-005", displayName = "Vikram", avatarId = 4, totalXp = 2600L, rank = 6, trend = "-2"),
            LeaderboardEntry(userId = "user-006", displayName = "Meera", avatarId = 6, totalXp = 2400L, rank = 7, trend = "0"),
            LeaderboardEntry(userId = "user-007", displayName = "Arjun", avatarId = 8, totalXp = 2100L, rank = 8, trend = "+1")
        ),
        userRank = UserRank(
            rankGlobal = 5,
            rankCountry = 3,
            rankCity = 1,
            xpGapToNext = 6650L
        )
    )

    // ── Stats ────────────────────────────────────────────────────────────

    fun mockStats(): StatsData = StatsData(
        stats = mockUserStats(),
        accuracyPct = 78.5,
        dailyActivity = listOf(
            DailyActivity(date = "2026-03-02", quizzes = 3, xp = 120, minutesSpent = 15),
            DailyActivity(date = "2026-03-03", quizzes = 5, xp = 200, minutesSpent = 25),
            DailyActivity(date = "2026-03-04", quizzes = 2, xp = 80, minutesSpent = 10),
            DailyActivity(date = "2026-03-05", quizzes = 4, xp = 160, minutesSpent = 20),
            DailyActivity(date = "2026-03-06", quizzes = 6, xp = 240, minutesSpent = 30),
            DailyActivity(date = "2026-03-07", quizzes = 3, xp = 130, minutesSpent = 18),
            DailyActivity(date = "2026-03-08", quizzes = 1, xp = 50, minutesSpent = 8)
        ),
        subjectPerformance = listOf(
            SubjectPerformance(
                moduleId = "mod-math", title = "Mathematics", emoji = "\uD83D\uDCCA",
                bestScorePct = 85, accuracyPct = 82, chaptersCompleted = 2, totalChapters = 4
            ),
            SubjectPerformance(
                moduleId = "mod-science", title = "Science", emoji = "\uD83D\uDD2C",
                bestScorePct = 72, accuracyPct = 70, chaptersCompleted = 1, totalChapters = 3
            ),
            SubjectPerformance(
                moduleId = "mod-english", title = "English", emoji = "\uD83D\uDCDA",
                bestScorePct = 90, accuracyPct = 88, chaptersCompleted = 0, totalChapters = 3
            ),
            SubjectPerformance(
                moduleId = "mod-gk", title = "General Knowledge", emoji = "\uD83C\uDF0D",
                bestScorePct = 0, accuracyPct = 0, chaptersCompleted = 0, totalChapters = 2
            )
        )
    )

    // ── Profile ──────────────────────────────────────────────────────────

    fun mockProfile(): ProfileData = ProfileData(
        user = mockUser(),
        stats = mockUserStats(),
        completedChapters = listOf(
            CompletedChapter(
                chapterId = "ch-math-1",
                chapterTitle = "Number Systems",
                moduleTitle = "Mathematics",
                moduleEmoji = "\uD83D\uDCCA",
                completedAt = "2026-02-20T14:30:00Z"
            ),
            CompletedChapter(
                chapterId = "ch-math-2",
                chapterTitle = "Fractions & Decimals",
                moduleTitle = "Mathematics",
                moduleEmoji = "\uD83D\uDCCA",
                completedAt = "2026-02-28T10:15:00Z"
            ),
            CompletedChapter(
                chapterId = "ch-sci-1",
                chapterTitle = "The Solar System",
                moduleTitle = "Science",
                moduleEmoji = "\uD83D\uDD2C",
                completedAt = "2026-03-05T16:45:00Z"
            )
        ),
        tournamentResults = listOf(
            TournamentResult(
                tournamentId = "tournament-001",
                title = "Science Quiz Bowl - March",
                score = 9,
                totalQuestions = 10,
                rank = 1,
                participantCount = 120,
                certificateUrl = null,
                date = "2026-03-01"
            ),
            TournamentResult(
                tournamentId = "tournament-002",
                title = "Math Olympiad Challenge",
                score = 8,
                totalQuestions = 10,
                rank = 3,
                participantCount = 85,
                certificateUrl = null,
                date = "2026-02-15"
            ),
            TournamentResult(
                tournamentId = "tournament-003",
                title = "English Literature Cup",
                score = 7,
                totalQuestions = 10,
                rank = 12,
                participantCount = 200,
                certificateUrl = null,
                date = "2026-01-20"
            )
        )
    )

    // ── Tournament ───────────────────────────────────────────────────────

    /** Returns tournament + optional entry for a given test scenario. */
    private fun mockTournamentForScenario(
        scenario: MockTournamentScenario,
    ): Pair<Tournament?, TournamentEntry?> = when (scenario) {
        MockTournamentScenario.PLAY -> mockTournament() to null
        MockTournamentScenario.RESUME -> {
            mockTournament().copy(userEntryStatus = TournamentEntryStatus.IN_PROGRESS) to
                mockResumeEntry()
        }
        MockTournamentScenario.COMPLETED -> {
            mockTournament().copy(userEntryStatus = TournamentEntryStatus.COMPLETED) to
                mockCompletedTournamentEntry()
        }
        MockTournamentScenario.UPCOMING -> mockUpcomingTournament() to null
        MockTournamentScenario.CLOSED -> {
            mockClosedTournament().copy(userEntryStatus = TournamentEntryStatus.COMPLETED) to
                mockCompletedTournamentEntry()
        }
        MockTournamentScenario.CLOSED_NO_ENTRY -> mockClosedTournament() to null
    }

    fun mockTournament(): Tournament = Tournament(
        id = "tournament-live-001",
        title = "Science Championship - March 2026",
        gradeId = "grade-5",
        subjectIds = listOf("mod-science"),
        questionCount = 10,
        timeLimitSeconds = 600,
        startsAt = nowMillis - (2 * 60 * 60 * 1000), // started 2 hours ago
        endsAt = nowMillis + (22 * 60 * 60 * 1000),   // ends in 22 hours
        status = TournamentStatus.LIVE,
        participantCount = 156,
        userEntryStatus = null
    )

    fun mockUpcomingTournament(): Tournament = Tournament(
        id = "tournament-upcoming-001",
        title = "Math Olympiad - April 2026",
        gradeId = "grade-5",
        subjectIds = listOf("mod-math"),
        questionCount = 10,
        timeLimitSeconds = 600,
        startsAt = nowMillis + (2 * 24 * 60 * 60 * 1000L), // 2 days from now
        endsAt = nowMillis + (3 * 24 * 60 * 60 * 1000L),
        status = TournamentStatus.SCHEDULED,
        participantCount = 42,
        userEntryStatus = null
    )

    fun mockClosedTournament(): Tournament = Tournament(
        id = "tournament-closed-001",
        title = "Science Championship - March 2026",
        gradeId = "grade-5",
        subjectIds = listOf("mod-science"),
        questionCount = 10,
        timeLimitSeconds = 600,
        startsAt = nowMillis - (48 * 60 * 60 * 1000L), // started 48h ago
        endsAt = nowMillis - (24 * 60 * 60 * 1000L),    // ended 24h ago
        status = TournamentStatus.CLOSED,
        participantCount = 156,
        userEntryStatus = null
    )

    fun mockTournamentEntry(): TournamentEntry = TournamentEntry(
        id = "entry-mock-001",
        tournamentId = "tournament-live-001",
        userId = MOCK_USER_ID,
        status = TournamentEntryStatus.IN_PROGRESS,
        score = 0,
        timeTakenSeconds = null,
        rank = null,
        questionsAnswered = 0,
        timeRemainingSeconds = 600
    )

    /** Entry for a user who is mid-quiz (answered 4 of 10, scored 3). */
    fun mockResumeEntry(): TournamentEntry = TournamentEntry(
        id = "entry-mock-resume",
        tournamentId = "tournament-live-001",
        userId = MOCK_USER_ID,
        status = TournamentEntryStatus.IN_PROGRESS,
        score = 3,
        timeTakenSeconds = 180,
        rank = null,
        questionsAnswered = 4,
        timeRemainingSeconds = 420
    )

    /** Entry for a user who has completed the tournament. */
    fun mockCompletedTournamentEntry(): TournamentEntry = TournamentEntry(
        id = "entry-mock-completed",
        tournamentId = "tournament-live-001",
        userId = MOCK_USER_ID,
        status = TournamentEntryStatus.COMPLETED,
        score = 7,
        timeTakenSeconds = 420,
        rank = 3,
        questionsAnswered = 10,
        timeRemainingSeconds = 0,
        certificateId = "cert-mock-001"
    )

    /**
     * Returns a [Quiz] built from mock tournament questions.
     * Uses MCQ questions to match typical tournament format.
     */
    fun mockTournamentQuiz(): Quiz {
        val tournament = mockTournament()
        return Quiz(
            id = "tournament-quiz-${tournament.id}",
            title = tournament.title,
            quizType = "tournament",
            questionCount = tournament.questionCount,
            timeLimitSeconds = tournament.timeLimitSeconds,
            maxXp = 100,
            displayOrder = 0,
            questions = mockTournamentQuestions(),
        )
    }

    private fun mockTournamentQuestions(): List<Question> = listOf(
        Question(
            id = "tq-1", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which planet is known as the Red Planet?",
            explanation = "Mars is called the Red Planet due to iron oxide on its surface.",
            options = listOf(
                QuestionOption("tq1a", "Venus", false, 1),
                QuestionOption("tq1b", "Mars", true, 2),
                QuestionOption("tq1c", "Jupiter", false, 3),
                QuestionOption("tq1d", "Saturn", false, 4),
            ),
        ),
        Question(
            id = "tq-2", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the chemical symbol for water?",
            explanation = "Water is H2O — two hydrogen atoms and one oxygen atom.",
            options = listOf(
                QuestionOption("tq2a", "CO2", false, 1),
                QuestionOption("tq2b", "H2O", true, 2),
                QuestionOption("tq2c", "NaCl", false, 3),
                QuestionOption("tq2d", "O2", false, 4),
            ),
        ),
        Question(
            id = "tq-3", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "How many continents are there on Earth?",
            explanation = "There are 7 continents: Asia, Africa, North America, South America, Antarctica, Europe, and Australia.",
            options = listOf(
                QuestionOption("tq3a", "5", false, 1),
                QuestionOption("tq3b", "6", false, 2),
                QuestionOption("tq3c", "7", true, 3),
                QuestionOption("tq3d", "8", false, 4),
            ),
        ),
        Question(
            id = "tq-4", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the largest organ in the human body?",
            explanation = "The skin is the largest organ, covering about 1.7 square meters in adults.",
            options = listOf(
                QuestionOption("tq4a", "Heart", false, 1),
                QuestionOption("tq4b", "Liver", false, 2),
                QuestionOption("tq4c", "Skin", true, 3),
                QuestionOption("tq4d", "Brain", false, 4),
            ),
        ),
        Question(
            id = "tq-5", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which gas do plants absorb from the atmosphere?",
            explanation = "Plants absorb carbon dioxide (CO2) during photosynthesis.",
            options = listOf(
                QuestionOption("tq5a", "Oxygen", false, 1),
                QuestionOption("tq5b", "Nitrogen", false, 2),
                QuestionOption("tq5c", "Carbon dioxide", true, 3),
                QuestionOption("tq5d", "Hydrogen", false, 4),
            ),
        ),
        Question(
            id = "tq-6", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the speed of light approximately?",
            explanation = "Light travels at approximately 300,000 km/s in a vacuum.",
            options = listOf(
                QuestionOption("tq6a", "150,000 km/s", false, 1),
                QuestionOption("tq6b", "300,000 km/s", true, 2),
                QuestionOption("tq6c", "500,000 km/s", false, 3),
                QuestionOption("tq6d", "1,000,000 km/s", false, 4),
            ),
        ),
        Question(
            id = "tq-7", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which element has the atomic number 1?",
            explanation = "Hydrogen has atomic number 1 — it is the lightest element.",
            options = listOf(
                QuestionOption("tq7a", "Helium", false, 1),
                QuestionOption("tq7b", "Hydrogen", true, 2),
                QuestionOption("tq7c", "Lithium", false, 3),
                QuestionOption("tq7d", "Carbon", false, 4),
            ),
        ),
        Question(
            id = "tq-8", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What is the boiling point of water at sea level?",
            explanation = "Water boils at 100 degrees Celsius (212 degrees Fahrenheit) at sea level.",
            options = listOf(
                QuestionOption("tq8a", "90°C", false, 1),
                QuestionOption("tq8b", "100°C", true, 2),
                QuestionOption("tq8c", "110°C", false, 3),
                QuestionOption("tq8d", "120°C", false, 4),
            ),
        ),
        Question(
            id = "tq-9", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "Which force keeps us on the ground?",
            explanation = "Gravity is the force that pulls objects toward the center of the Earth.",
            options = listOf(
                QuestionOption("tq9a", "Friction", false, 1),
                QuestionOption("tq9b", "Magnetism", false, 2),
                QuestionOption("tq9c", "Gravity", true, 3),
                QuestionOption("tq9d", "Tension", false, 4),
            ),
        ),
        Question(
            id = "tq-10", questionType = QuestionType.MULTIPLE_CHOICE,
            title = "What type of energy does the Sun primarily emit?",
            explanation = "The Sun primarily emits radiant (light) energy through nuclear fusion.",
            options = listOf(
                QuestionOption("tq10a", "Kinetic energy", false, 1),
                QuestionOption("tq10b", "Chemical energy", false, 2),
                QuestionOption("tq10c", "Radiant energy", true, 3),
                QuestionOption("tq10d", "Sound energy", false, 4),
            ),
        ),
    )

    /**
     * Returns a mock IQ-test [Quiz] with pattern-recognition MCQ questions.
     * No answer feedback or explanations are shown during IQ tests.
     */
    fun mockIqTestQuiz(): Quiz = Quiz(
        id = "iq-test-001",
        title = "IQ Challenge",
        quizType = "iq",
        questionCount = 5,
        timeLimitSeconds = 300,
        maxXp = 0,
        displayOrder = 0,
        questions = listOf(
            Question(
                id = "iq-1", questionType = QuestionType.MULTIPLE_CHOICE,
                title = "What comes next in the sequence: 2, 6, 12, 20, ?",
                explanation = "The differences are 4, 6, 8, so next difference is 10 → 30.",
                options = listOf(
                    QuestionOption("iq1a", "28", false, 1),
                    QuestionOption("iq1b", "30", true, 2),
                    QuestionOption("iq1c", "32", false, 3),
                    QuestionOption("iq1d", "26", false, 4),
                ),
            ),
            Question(
                id = "iq-2", questionType = QuestionType.MULTIPLE_CHOICE,
                title = "If all Bloops are Razzies and all Razzies are Lazzies, then all Bloops are definitely Lazzies?",
                explanation = "This is a classic syllogism — if A⊂B and B⊂C then A⊂C.",
                options = listOf(
                    QuestionOption("iq2a", "True", true, 1),
                    QuestionOption("iq2b", "False", false, 2),
                    QuestionOption("iq2c", "Cannot be determined", false, 3),
                    QuestionOption("iq2d", "Sometimes", false, 4),
                ),
            ),
            Question(
                id = "iq-3", questionType = QuestionType.MULTIPLE_CHOICE,
                title = "Which number is the odd one out: 3, 5, 11, 14, 17, 23?",
                explanation = "14 is the only even (non-prime) number in the list.",
                options = listOf(
                    QuestionOption("iq3a", "3", false, 1),
                    QuestionOption("iq3b", "11", false, 2),
                    QuestionOption("iq3c", "14", true, 3),
                    QuestionOption("iq3d", "23", false, 4),
                ),
            ),
            Question(
                id = "iq-4", questionType = QuestionType.MULTIPLE_CHOICE,
                title = "Complete the analogy: Book is to Reading as Fork is to ___",
                explanation = "A book is used for reading; a fork is used for eating.",
                options = listOf(
                    QuestionOption("iq4a", "Drawing", false, 1),
                    QuestionOption("iq4b", "Writing", false, 2),
                    QuestionOption("iq4c", "Eating", true, 3),
                    QuestionOption("iq4d", "Cooking", false, 4),
                ),
            ),
            Question(
                id = "iq-5", questionType = QuestionType.MULTIPLE_CHOICE,
                title = "What comes next: 1, 1, 2, 3, 5, 8, ?",
                explanation = "This is the Fibonacci sequence — each number is the sum of the two before it.",
                options = listOf(
                    QuestionOption("iq5a", "11", false, 1),
                    QuestionOption("iq5b", "13", true, 2),
                    QuestionOption("iq5c", "15", false, 3),
                    QuestionOption("iq5d", "10", false, 4),
                ),
            ),
        ),
    )

    /**
     * Returns a mock daily-challenge [Quiz] from the given [DailyChallenge] metadata.
     * Reuses a subset of math questions for demonstration.
     */
    fun mockDailyChallengeQuiz(challenge: DailyChallenge): Quiz = Quiz(
        id = challenge.quizId,
        title = challenge.title,
        quizType = "practice",
        questionCount = challenge.questionCount,
        timeLimitSeconds = challenge.timeInMinutes * 60,
        maxXp = 25,
        displayOrder = 0,
        questions = mockTournamentQuestions().take(challenge.questionCount),
    )

    // ── Reference Data ───────────────────────────────────────────────────

    fun mockGrades(): List<Grade> = listOf(
        Grade(id = "grade-1", code = "G1", label = "Grade 1", sortOrder = 1),
        Grade(id = "grade-2", code = "G2", label = "Grade 2", sortOrder = 2),
        Grade(id = "grade-3", code = "G3", label = "Grade 3", sortOrder = 3),
        Grade(id = "grade-4", code = "G4", label = "Grade 4", sortOrder = 4),
        Grade(id = "grade-5", code = "G5", label = "Grade 5", sortOrder = 5),
        Grade(id = "grade-6", code = "G6", label = "Grade 6", sortOrder = 6),
        Grade(id = "grade-7", code = "G7", label = "Grade 7", sortOrder = 7),
        Grade(id = "grade-8", code = "G8", label = "Grade 8", sortOrder = 8)
    )

    fun mockCountries(): List<Country> = listOf(
        Country(id = "country-in", name = "India", code = "IN")
    )

    fun mockCities(countryId: String): List<City> = when (countryId) {
        "country-in" -> listOf(
            City(id = "city-del", countryId = "country-in", name = "Delhi"),
            City(id = "city-mum", countryId = "country-in", name = "Mumbai"),
            City(id = "city-blr", countryId = "country-in", name = "Bangalore"),
            City(id = "city-chn", countryId = "country-in", name = "Chennai"),
            City(id = "city-hyd", countryId = "country-in", name = "Hyderabad"),
            City(id = "city-kol", countryId = "country-in", name = "Kolkata"),
            City(id = "city-pun", countryId = "country-in", name = "Pune"),
            City(id = "city-jai", countryId = "country-in", name = "Jaipur")
        )
        else -> emptyList()
    }
}
