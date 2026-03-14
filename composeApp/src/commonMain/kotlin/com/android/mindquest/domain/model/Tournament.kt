package com.android.mindquest.domain.model

data class Tournament(
    val id: String,
    val title: String,
    val gradeId: String,
    val subjectIds: List<String>? = null,
    val questionCount: Int,
    val timeLimitSeconds: Int,
    val startsAt: Long, // epoch millis
    val endsAt: Long,
    val status: TournamentStatus,
    val participantCount: Int = 0,
    val userEntryStatus: TournamentEntryStatus? = null
)

enum class TournamentStatus {
    DRAFT, SCHEDULED, LIVE, CLOSED, FINALIZED;

    companion object {
        fun fromString(s: String) = entries.find { it.name.equals(s, true) } ?: DRAFT
    }
}

data class TournamentEntry(
    val id: String,
    val tournamentId: String,
    val userId: String,
    val status: TournamentEntryStatus,
    val score: Int = 0,
    val timeTakenSeconds: Int? = null,
    val rank: Int? = null,
    val questionsAnswered: Int = 0,
    val timeRemainingSeconds: Int? = null,
    val certificateId: String? = null
)

/**
 * Result of starting a tournament — bundles the entry with tournament quiz questions.
 */
data class TournamentStartResult(
    val entry: TournamentEntry,
    val quiz: Quiz,
)

enum class TournamentEntryStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, AUTO_SUBMITTED;

    companion object {
        fun fromString(s: String) = entries.find { it.name.equals(s.replace("-", "_"), true) } ?: NOT_STARTED
    }
}
