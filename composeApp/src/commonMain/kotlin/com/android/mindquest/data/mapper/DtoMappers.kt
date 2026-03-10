package com.android.mindquest.data.mapper

import com.android.mindquest.data.remote.dto.ChapterDto
import com.android.mindquest.data.remote.dto.ChapterProgressDto
import com.android.mindquest.data.remote.dto.CityDto
import com.android.mindquest.data.remote.dto.CompletedChapterDto
import com.android.mindquest.data.remote.dto.CountryDto
import com.android.mindquest.data.remote.dto.DailyChallengeDto
import com.android.mindquest.data.remote.dto.DailyActivityDto
import com.android.mindquest.data.remote.dto.DashboardResponseDto
import com.android.mindquest.data.remote.dto.GradeDto
import com.android.mindquest.data.remote.dto.LeaderboardEntryDto
import com.android.mindquest.data.remote.dto.LeaderboardResponseDto
import com.android.mindquest.data.remote.dto.MatchPairDto
import com.android.mindquest.data.remote.dto.ModuleDto
import com.android.mindquest.data.remote.dto.ModuleProgressDto
import com.android.mindquest.data.remote.dto.ModuleWithProgressDto
import com.android.mindquest.data.remote.dto.OptionDto
import com.android.mindquest.data.remote.dto.ProfileResponseDto
import com.android.mindquest.data.remote.dto.QuestionDto
import com.android.mindquest.data.remote.dto.QuizDto
import com.android.mindquest.data.remote.dto.QuizResultDto
import com.android.mindquest.data.remote.dto.StatsResponseDto
import com.android.mindquest.data.remote.dto.SubjectPerformanceDto
import com.android.mindquest.data.remote.dto.TournamentEntryDto
import com.android.mindquest.data.remote.dto.TournamentInfoDto
import com.android.mindquest.data.remote.dto.TournamentResultDto
import com.android.mindquest.data.remote.dto.UserBasicDto
import com.android.mindquest.data.remote.dto.UserProfileDto
import com.android.mindquest.data.remote.dto.UserRankDto
import com.android.mindquest.data.remote.dto.UserStatsDto
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
import kotlinx.datetime.Instant

// ── User ─────────────────────────────────────────────────────────────────

fun UserBasicDto.toDomain(): User = User(
    id = id,
    displayName = displayName,
    avatarId = avatarId,
    gradeId = gradeId,
    gradeLabel = "",
    authProvider = authProvider,
    isAnonymous = authProvider == "anonymous"
)

fun UserProfileDto.toDomain(): User = User(
    id = id,
    displayName = displayName,
    avatarId = avatarId,
    gradeId = gradeId,
    gradeLabel = gradeLabel,
    authProvider = authProvider,
    isAnonymous = authProvider == "anonymous",
    countryName = countryName,
    cityName = cityName,
    schoolName = schoolName
)

// ── Stats ────────────────────────────────────────────────────────────────

fun UserStatsDto.toDomain(): UserStats = UserStats(
    totalXp = totalXp,
    level = level,
    streakCurrent = streakCurrent,
    streakBest = streakBest,
    quizzesCompleted = quizzesCompleted,
    accuracyPct = accuracyPct,
    tournamentsPlayed = tournamentsPlayed,
    bestTournamentRank = bestTournamentRank
)

// ── Module ───────────────────────────────────────────────────────────────

fun ModuleWithProgressDto.toDomain(): Module = Module(
    id = id,
    title = title,
    emoji = emoji,
    accentColor = accentColor,
    displayOrder = sortOrder,
    progress = progress?.toDomain()
)

fun ModuleDto.toDomain(): Module = Module(
    id = id,
    title = title,
    subtitle = subtitle,
    emoji = emoji,
    accentColor = accentColor,
    displayOrder = displayOrder
)

fun ModuleProgressDto.toDomain(): ModuleProgress = ModuleProgress(
    currentChapterId = currentChapterId,
    currentQuizId = currentQuizId,
    bestScorePct = bestScorePct,
    isCompleted = isCompleted
)

// ── Dashboard ────────────────────────────────────────────────────────────

fun DashboardResponseDto.toDomain(): DashboardData = DashboardData(
    user = user.toDomain(),
    stats = stats.toDomain(),
    modules = modules.map { it.toDomain() },
    activeTournament = activeTournament?.toDomain()
)

// ── Chapter ──────────────────────────────────────────────────────────────

fun ChapterDto.toDomain(): Chapter = Chapter(
    id = id,
    title = title,
    chapterNumber = chapterNumber,
    quizCount = quizCount,
    progress = progress?.toDomain(),
    state = parseChapterState(state)
)

fun ChapterProgressDto.toDomain(): ChapterProgress = ChapterProgress(
    quizzesDone = quizzesDone,
    totalQuizzes = totalQuizzes,
    bestScorePct = bestScorePct,
    isCompleted = isCompleted
)

private fun parseChapterState(value: String): ChapterState = when (value.lowercase()) {
    "completed" -> ChapterState.COMPLETED
    "unlocked" -> ChapterState.UNLOCKED
    else -> ChapterState.LOCKED
}

// ── Quiz ─────────────────────────────────────────────────────────────────

fun QuizDto.toDomain(): Quiz = Quiz(
    id = id,
    title = title,
    quizType = quizType,
    questionCount = questionCount,
    timeLimitSeconds = timeLimitSecs,
    maxXp = maxXp,
    difficulty = difficulty,
    displayOrder = sortOrder,
    bestScore = bestScore,
    attemptCount = attemptCount,
    questions = questions.map { it.toDomain() }
)

// ── Question ─────────────────────────────────────────────────────────────

fun QuestionDto.toDomain(): Question = Question(
    id = id,
    questionType = QuestionType.fromString(questionType),
    title = title,
    prompt = prompt,
    explanation = explanation,
    difficulty = difficulty,
    timeLimitSeconds = timeLimitSecs,
    allowMultiple = allowMultiple,
    promptConfig = promptConfig?.let { jsonObjectToMap(it) },
    metadata = metadata?.let { jsonObjectToMap(it) },
    mediaUrl = mediaUrl,
    options = options.map { it.toDomain() },
    matchPairs = matchPairs?.map { it.toDomain() }
)

fun OptionDto.toDomain(): QuestionOption = QuestionOption(
    id = id,
    label = label,
    isCorrect = isCorrect,
    displayOrder = sortOrder,
    correctPosition = correctPosition,
    mediaUrl = mediaUrl,
    visualLabel = visualLabel
)

fun MatchPairDto.toDomain(): MatchPair = MatchPair(
    id = id,
    leftText = leftText,
    rightText = rightText,
    displayOrder = sortOrder
)

// ── Quiz Result ──────────────────────────────────────────────────────────

fun QuizResultDto.toDomain(): QuizResult = QuizResult(
    status = status,
    attemptId = attemptId,
    score = score,
    totalQuestions = totalQuestions,
    xpEarned = xpEarned,
    totalXp = totalXp,
    level = level,
    levelChanged = levelChanged,
    rankGlobal = rankGlobal,
    isReplay = isReplay,
    nextQuizId = nextQuizId
)

// ── Leaderboard ──────────────────────────────────────────────────────────

fun LeaderboardEntryDto.toDomain(): LeaderboardEntry = LeaderboardEntry(
    userId = userId,
    displayName = displayName,
    avatarId = avatarId,
    totalXp = totalXp,
    rank = rank
)

fun UserRankDto.toDomain(): UserRank = UserRank(
    rankGlobal = rankGlobal,
    rankCountry = rankCountry,
    rankCity = rankCity,
    xpGapToNext = xpGap
)

fun LeaderboardResponseDto.toDomain(): LeaderboardData = LeaderboardData(
    rankedUsers = rankedUsers.map { it.toDomain() },
    userRank = userRank.toDomain()
)

// ── Stats ────────────────────────────────────────────────────────────────

fun DailyActivityDto.toDomain(): DailyActivity = DailyActivity(
    date = date,
    quizzes = quizzes,
    xp = xp,
    minutesSpent = timeSpentMinutes
)

fun SubjectPerformanceDto.toDomain(): SubjectPerformance = SubjectPerformance(
    moduleId = moduleId,
    title = title,
    emoji = emoji,
    bestScorePct = bestScorePct,
    accuracyPct = accuracyPct,
    chaptersCompleted = chaptersCompleted,
    totalChapters = totalChapters
)

fun StatsResponseDto.toDomain(): StatsData = StatsData(
    stats = stats.toDomain(),
    accuracyPct = accuracyPct,
    dailyActivity = dailyActivity.map { it.toDomain() },
    subjectPerformance = subjectPerformance.map { it.toDomain() }
)

// ── Profile ──────────────────────────────────────────────────────────────

fun CompletedChapterDto.toDomain(): CompletedChapter = CompletedChapter(
    chapterId = chapterId,
    chapterTitle = chapterTitle,
    moduleTitle = moduleTitle,
    moduleEmoji = moduleEmoji,
    completedAt = completedAt
)

fun TournamentResultDto.toDomain(): TournamentResult = TournamentResult(
    tournamentId = tournamentId,
    title = title,
    score = score,
    totalQuestions = totalQuestions,
    rank = rank,
    participantCount = participantCount,
    certificateUrl = certificateUrl,
    date = date
)

fun ProfileResponseDto.toDomain(): ProfileData = ProfileData(
    user = user.toDomain(),
    stats = stats.toDomain(),
    completedChapters = completedChapters.map { it.toDomain() },
    tournamentResults = tournamentResults.map { it.toDomain() }
)

// ── Daily Challenge ──────────────────────────────────────────────────────

fun DailyChallengeDto.toDomain(): DailyChallenge = DailyChallenge(
    quizId = quizId,
    title = title,
    description = description,
    questionCount = questionCount,
    timeInMinutes = timeInMinutes,
    moduleId = moduleId,
    chapterId = chapterId,
    isDone = isDone
)

// ── Reference Data ───────────────────────────────────────────────────────

fun GradeDto.toDomain(): Grade = Grade(
    id = id,
    code = code,
    label = label,
    sortOrder = sortOrder
)

fun CountryDto.toDomain(): Country = Country(
    id = id,
    name = name,
    code = code
)

fun CityDto.toDomain(): City = City(
    id = id,
    countryId = countryId,
    name = name
)

// ── Tournament ───────────────────────────────────────────────────────────

fun TournamentInfoDto.toDomain(): Tournament = Tournament(
    id = id,
    title = title,
    gradeId = "",
    questionCount = questionCount,
    timeLimitSeconds = timeLimitSeconds,
    startsAt = parseIsoToEpochMillis(startsAt),
    endsAt = parseIsoToEpochMillis(endsAt),
    status = TournamentStatus.fromString(status),
    participantCount = participantCount,
    userEntryStatus = userEntryStatus?.let { TournamentEntryStatus.fromString(it) }
)

fun TournamentEntryDto.toDomain(): TournamentEntry = TournamentEntry(
    id = id,
    tournamentId = tournamentId,
    userId = userId,
    status = TournamentEntryStatus.fromString(status),
    score = score,
    timeTakenSeconds = timeTakenSeconds,
    rank = rank,
    timeRemainingSeconds = timeRemainingSecs
)

// ── Helpers ──────────────────────────────────────────────────────────────

/**
 * Parses an ISO-8601 datetime string (e.g. "2026-03-15T10:00:00Z") to epoch
 * milliseconds. Returns 0 if the string cannot be parsed.
 */
private fun parseIsoToEpochMillis(iso: String): Long {
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (_: Exception) {
        0L
    }
}

/**
 * Converts a [kotlinx.serialization.json.JsonObject] to a simple
 * `Map<String, Any>` by extracting primitive values as strings.
 */
private fun jsonObjectToMap(json: kotlinx.serialization.json.JsonObject): Map<String, Any> {
    return json.entries.associate { (key, value) ->
        key to (value as Any)
    }
}
