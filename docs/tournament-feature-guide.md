# Mindquest V2 - Tournament Feature Guide

This document covers the tournament module as a **standalone, separable feature**. It includes all database tables, RPC functions, frontend components, and DI bindings related to tournaments. Use this guide to understand, modify, or remove the tournament feature independently.

## Table of Contents
1. [Feature Overview](#feature-overview)
2. [Database Schema](#database-schema)
3. [RPC Functions](#rpc-functions)
4. [Frontend Architecture](#frontend-architecture)
5. [Quiz Behavior: TOURNAMENT Mode](#quiz-behavior-tournament-mode)
6. [Entry Lifecycle](#entry-lifecycle)
7. [Submission Strategy](#submission-strategy)
8. [Ranking Algorithm](#ranking-algorithm)
9. [How to Remove/Disable Tournaments](#how-to-removedisable-tournaments)

---

## Feature Overview

Tournaments are **time-limited, grade-based competitions** where users answer questions under a shared timer. Key characteristics:

- **Grade-scoped**: Each tournament targets a specific grade (G1-G8)
- **Time-limited**: Global timer (default 600s) counts down during play
- **Pausable**: Users can pause and resume (timer preserves remaining time)
- **Ranked**: Leaderboard sorted by score DESC, time taken ASC (tiebreaker)
- **Per-question submission**: Answers saved in real-time (fire-and-forget) plus batch at end
- **No feedback**: Users don't see correct/incorrect during play

---

## Database Schema

### `tournaments`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT uuid_generate_v4() | |
| title | TEXT | NOT NULL | Tournament name |
| description | TEXT | | Optional description |
| grade_id | UUID | FK → grades(id), NOT NULL | Target grade |
| question_count | INT | NOT NULL, DEFAULT 10 | Number of questions |
| time_limit_seconds | INT | NOT NULL, DEFAULT 600 | Total time in seconds |
| max_participants | INT | | Capacity limit (NULL = unlimited) |
| starts_at | TIMESTAMPTZ | NOT NULL | When users can start joining |
| ends_at | TIMESTAMPTZ | NOT NULL | When tournament closes |
| status | TEXT | NOT NULL, DEFAULT 'draft' | CHECK: draft/scheduled/live/closed/finalized |
| created_by | UUID | | Admin who created |
| updated_by | UUID | | Last admin who modified |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Status Lifecycle:**
```
draft → scheduled → live → closed → finalized
```

### `tournament_questions`

Links questions to tournaments. Questions can come from any quiz.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| tournament_id | UUID | FK → tournaments(id) ON DELETE CASCADE, NOT NULL |
| question_id | UUID | FK → questions(id) ON DELETE CASCADE, NOT NULL |
| sort_order | INT | NOT NULL, DEFAULT 0 |

UNIQUE(tournament_id, question_id)

### `tournament_entries`

One entry per user per tournament.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Entry ID (used for all operations) |
| tournament_id | UUID | FK → tournaments(id) ON DELETE CASCADE, NOT NULL | |
| user_id | UUID | FK → users(id) ON DELETE CASCADE, NOT NULL | |
| status | TEXT | NOT NULL, DEFAULT 'not_started' | See Entry Lifecycle |
| score | INT | NOT NULL, DEFAULT 0 | Questions correct |
| time_taken_seconds | INT | | Total time used |
| rank | INT | | Final rank (set on submission) |
| time_remaining_secs | INT | | Saved on pause |
| answers_so_far | JSONB | DEFAULT '[]' | Running answer log |
| started_at | TIMESTAMPTZ | | When user started |
| completed_at | TIMESTAMPTZ | | When user finished |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

UNIQUE(tournament_id, user_id) — one entry per user per tournament

---

## RPC Functions

### App-Facing Functions

#### `get_active_tournament(p_user_id UUID, p_grade_id UUID) → JSON`
Returns the next live or scheduled tournament for the user's grade. Includes:
- Tournament details (title, question count, timer, dates)
- Participant count
- User's entry status (if they've already started)

#### `start_tournament(p_user_id UUID, p_tournament_id UUID) → JSON`
Starts (or retrieves) a tournament entry:
1. Checks tournament is `live`
2. Enforces `max_participants` limit
3. Creates entry with status `in_progress` (or returns existing)
4. Loads questions via `tournament_questions` join
5. Returns: `{entry_id, questions[], time_limit_secs}`

**Idempotent**: If user already has an entry, returns the existing one.

#### `pause_tournament(p_entry_id UUID) → JSON`
Sets entry status to `paused`. Saves `time_remaining_secs`.

#### `resume_tournament(p_entry_id UUID) → JSON`
Sets entry status back to `in_progress`. Returns updated entry with remaining time.

#### `submit_tournament_answer(p_entry_id UUID, p_answer JSON) → VOID`
Fire-and-forget per-question save. Appends to `answers_so_far` JSONB array:
```json
{"question_id": "uuid", "selected": "option_id", "is_correct": true, "time_ms": 5200}
```

#### `submit_tournament(p_entry_id UUID, p_answers JSON, p_time_taken INT) → JSON`
Final batch submission:
1. Sets status to `completed`
2. Calculates score from answers
3. Calculates rank (score DESC, time ASC)
4. Awards XP (10 per correct answer)
5. Updates user's `total_xp`, `level`, `streak`
6. Returns: `{score, total_questions, xp_earned, rank}`

#### `get_tournament_leaderboard(p_tournament_id UUID, p_user_id UUID, p_limit INT, p_offset INT) → JSON`
Returns ranked entries:
- Entries sorted by score DESC, time_taken ASC
- Includes user avatars, names, scores
- Includes current user's rank and entry

**Note:** The app uses a separate `TournamentLeaderboardResponseDto` (with `ranked_users`, `my_entry`, `total_participants` fields) rather than the shared `LeaderboardResponseDto`, because the tournament leaderboard response has a different shape (uses `score` instead of `total_xp`, and `my_entry` instead of `user_rank`).

### Admin Functions

#### `admin_list_tournaments(p_status TEXT, p_limit INT, p_offset INT) → JSON`
Lists tournaments with participant counts.

#### `admin_upsert_tournament(p_data JSON) → JSON`
Creates or updates a tournament.

#### `admin_set_tournament_questions(p_tournament_id UUID, p_question_ids JSON) → JSON`
Assigns questions to a tournament. Replaces all existing assignments.

#### `admin_tournament_lifecycle(p_tournament_id UUID, p_action TEXT) → JSON`
Changes tournament status:

| Action | From → To |
|--------|-----------|
| `schedule` | draft → scheduled |
| `go_live` | scheduled → live |
| `close` | live → closed |
| `finalize` | closed → finalized |

---

## Frontend Architecture

### Kotlin Files

| File | Package | Purpose |
|------|---------|---------|
| `TournamentViewModel.kt` | `presentation/tournament/` | Tournament state, timer, lifecycle |
| `TournamentLobbyScreen.kt` | `presentation/tournament/` | Lobby UI (join/resume/countdown) |
| `TournamentPlayScreen.kt` | `presentation/tournament/` | Simplified quiz play UI |
| `TournamentPauseScreen.kt` | `presentation/tournament/` | Pause overlay |
| `TournamentResultScreen.kt` | `presentation/tournament/` | Score, rank, leaderboard |
| `TournamentHomeBanner.kt` | `presentation/home/` | Home screen tournament card |
| `Tournament.kt` | `domain/model/` | Domain models (Tournament, TournamentEntry, TournamentStartResult) |
| `TournamentRepository.kt` | `domain/repository/` | Repository interface |
| `TournamentRepositoryImpl.kt` | `data/repository/` | Repository implementation |
| `TournamentEntryDto.kt` | `data/remote/dto/` | DTO for entry |
| `TournamentLeaderboardDto.kt` | `data/remote/dto/` | DTOs for tournament leaderboard (separate from app leaderboard) |
| `GetActiveTournamentUseCase.kt` | `domain/usecase/` | Fetch active tournament |
| `GetTournamentEntryUseCase.kt` | `domain/usecase/` | Fetch user's entry |
| `StartTournamentUseCase.kt` | `domain/usecase/` | Start tournament |
| `SubmitTournamentUseCase.kt` | `domain/usecase/` | Submit final answers |
| `SubmitSingleAnswerUseCase.kt` | `domain/usecase/` | Per-question fire-and-forget |

### Navigation Routes (NavRoutes.kt)

```kotlin
const val TOURNAMENT_LOBBY = "tournament_lobby"
const val TOURNAMENT_PLAY = "tournament_play"
const val TOURNAMENT_PAUSE = "tournament_pause"
const val TOURNAMENT_RESULT = "tournament_result"
```

### DI Bindings (AppModule.kt)

```kotlin
// Repository
single<TournamentRepository> { TournamentRepositoryImpl(get()) }

// Use Cases
factory { GetActiveTournamentUseCase(get()) }
factory { GetTournamentEntryUseCase(get()) }
factory { StartTournamentUseCase(get()) }
factory { SubmitTournamentUseCase(get()) }
factory { SubmitSingleAnswerUseCase(get()) }

// ViewModel
viewModelOf(::TournamentViewModel)
```

### Navigation Flow

```
HomeScreen (TournamentHomeBanner)
    |
    v
TournamentLobbyScreen
    |
    ├── [SCHEDULED] → Countdown, "Coming Soon" (disabled)
    ├── [LIVE + NOT_STARTED] → "Start Tournament" button
    ├── [IN_PROGRESS] → "Resume" button
    └── [COMPLETED] → Redirect to TournamentResultScreen
    |
    v (on start/resume)
QuizPlayScreen (with tournament QuizConfig)
    |
    ├── [Pause] → TournamentPauseScreen → Resume / Quit
    └── [Finish] → TournamentResultScreen
```

### TournamentViewModel State

```kotlin
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
```

**Key StateFlows:**
- `tournamentState: StateFlow<UiState<Tournament>>` - active tournament info
- `entryState: StateFlow<UiState<TournamentEntry>>` - user's entry
- `playState: StateFlow<TournamentPlayState>` - quiz play state
- `timeLeft: StateFlow<Int>` - countdown timer (seconds)
- `isPaused: StateFlow<Boolean>` - pause flag

---

## Quiz Behavior: TOURNAMENT Mode

### QuizConfig.tournament() Factory

```kotlin
QuizConfig.tournament(
    tournamentId = "uuid",
    entryId = "uuid",
    allowPause = true
)
```

**Behavioral flags vs other modes:**

| Flag | MODULE | IQ_TEST | TOURNAMENT |
|------|--------|---------|-----------|
| showAnswerFeedback | true | false | **false** |
| showExplanation | true | false | **false** |
| enableNudges | true | false | **false** |
| autoAdvanceDelayMs | null | 300ms | **300ms** |
| allowPause | false | false | **true** |
| submissionMode | BATCH_AT_END | BATCH_AT_END | **PER_QUESTION_NON_BLOCKING** |
| showReviewButton | true | false | **false** |
| showRetryButton | true | false | **false** |
| accentColor | module color | green | **indigo (#4F46E5)** |
| topBarLabel | module title | "IQ Test" | **"Tournament"** |

### QuizBehavior.TOURNAMENT in QuizViewModel

1. **Per-question submission**: After each `confirmAnswer()`, if `submissionMode == PER_QUESTION_NON_BLOCKING`, calls `submitSingleAnswer(entryId, answer)` in a background coroutine. No blocking.

2. **Final submission**: `finishQuiz()` detects `TOURNAMENT` behavior and calls `submitTournament(entryId, answers, timeTaken)` instead of `submitQuizAttempt()`.

3. **Timer expiry auto-submit**: If the timer runs out, auto-submits partial answers.

---

## Entry Lifecycle

```
                    ┌──────────────┐
                    │ NOT_STARTED  │  (entry created, user hasn't begun)
                    └──────┬───────┘
                           │ start_tournament()
                           v
                    ┌──────────────┐
               ┌────│ IN_PROGRESS  │────┐
               │    └──────┬───────┘    │
               │           │            │
     pause_tournament()    │    submit_tournament()
               │           │            │
               v           │            v
        ┌──────────┐       │    ┌──────────────┐
        │  PAUSED  │       │    │  COMPLETED   │
        └────┬─────┘       │    └──────────────┘
             │             │
    resume_tournament()    │ (timer expires)
             │             │
             v             v
        ┌──────────────┐  ┌────────────────┐
        │ IN_PROGRESS  │  │ AUTO_SUBMITTED │
        └──────────────┘  └────────────────┘
```

**Valid status values:** `not_started`, `in_progress`, `paused`, `completed`, `auto_submitted`

---

## Submission Strategy

Tournaments use a **dual submission strategy**:

### 1. Per-Question Fire-and-Forget

After each answer confirmation, the app calls `submit_tournament_answer()` in the background:
- Non-blocking (user doesn't wait for response)
- Appends to `answers_so_far` JSONB array
- Provides crash recovery (partial answers saved even if app closes)

### 2. Batch at End

When the user finishes all questions (or timer expires), `submit_tournament()` sends the complete answer set:
- Calculates final score
- Sets rank
- Awards XP
- Marks entry as `completed` or `auto_submitted`

**Why both?** Per-question saves provide data durability. The final batch is authoritative for scoring.

---

## Ranking Algorithm

### Score Calculation

```
score = COUNT(answers WHERE is_correct = true)
```

### Rank Calculation

```sql
ROW_NUMBER() OVER (
    ORDER BY score DESC, time_taken_seconds ASC
)
```

**Rules:**
- Higher score = better rank
- Same score → faster time wins
- Only `completed` and `auto_submitted` entries are ranked

### XP Award

```
xp = score * 10  (10 XP per correct answer)
```

XP is added to user's `total_xp` and triggers level recalculation.

---

## How to Remove/Disable Tournaments

### Option A: Disable (Recommended)

Hide tournament UI without removing code:

1. **Hide home banner**: In `HomeScreen.kt`, remove or comment out the `TournamentHomeBanner` composable
2. **Remove nav routes**: In `MindquestNavGraph.kt`, remove the tournament composable destinations
3. **Skip dashboard tournament**: In `get_user_dashboard` RPC, remove the active tournament query

### Option B: Full Removal

Remove all tournament code and data:

#### Database (SQL)

```sql
-- Drop tables (in order due to foreign keys)
DROP TABLE IF EXISTS public.tournament_entries CASCADE;
DROP TABLE IF EXISTS public.tournament_questions CASCADE;
DROP TABLE IF EXISTS public.tournaments CASCADE;

-- Drop RPC functions
DROP FUNCTION IF EXISTS get_active_tournament(UUID, UUID);
DROP FUNCTION IF EXISTS start_tournament(UUID, UUID);
DROP FUNCTION IF EXISTS pause_tournament(UUID);
DROP FUNCTION IF EXISTS resume_tournament(UUID);
DROP FUNCTION IF EXISTS submit_tournament(UUID, JSON, INT);
DROP FUNCTION IF EXISTS submit_tournament_answer(UUID, JSON);
DROP FUNCTION IF EXISTS get_tournament_leaderboard(UUID, UUID, INT, INT);
DROP FUNCTION IF EXISTS admin_list_tournaments(TEXT, INT, INT);
DROP FUNCTION IF EXISTS admin_upsert_tournament(JSON);
DROP FUNCTION IF EXISTS admin_set_tournament_questions(UUID, JSON);
DROP FUNCTION IF EXISTS admin_tournament_lifecycle(UUID, TEXT);
```

#### Kotlin Code

**Delete these files:**
- `presentation/tournament/TournamentViewModel.kt`
- `presentation/tournament/TournamentLobbyScreen.kt`
- `presentation/tournament/TournamentPlayScreen.kt`
- `presentation/tournament/TournamentPauseScreen.kt`
- `presentation/tournament/TournamentResultScreen.kt`
- `presentation/home/TournamentHomeBanner.kt`
- `domain/usecase/GetActiveTournamentUseCase.kt`
- `domain/usecase/GetTournamentEntryUseCase.kt`
- `domain/usecase/StartTournamentUseCase.kt`
- `domain/usecase/SubmitTournamentUseCase.kt`
- `domain/usecase/SubmitSingleAnswerUseCase.kt`
- `data/repository/TournamentRepositoryImpl.kt`
- `domain/repository/TournamentRepository.kt`

**Edit these files:**

| File | What to Remove |
|------|---------------|
| `di/AppModule.kt` | `TournamentRepository` single, 5 tournament use case factories, `TournamentViewModel` viewModelOf |
| `presentation/navigation/NavRoutes.kt` | TOURNAMENT_LOBBY, TOURNAMENT_PLAY, TOURNAMENT_PAUSE, TOURNAMENT_RESULT constants |
| `presentation/navigation/MindquestNavGraph.kt` | Tournament composable destinations, tournament navigation callbacks |
| `presentation/home/HomeScreen.kt` | `TournamentHomeBanner` usage |
| `data/remote/ApiService.kt` | Tournament-related methods (getActiveTournament, startTournament, pauseTournament, resumeTournament, submitTournament, submitTournamentAnswer, getTournamentLeaderboard, getTournamentEntry) |
| `data/mapper/DtoMappers.kt` | Tournament-related mapping extensions |
| `domain/model/Tournament.kt` | Can delete entirely OR keep if other code references TournamentStatus |
| `domain/model/QuizConfig.kt` | Remove `tournament()` factory method, `QuizBehavior.TOURNAMENT` enum value |
| `presentation/quiz/QuizViewModel.kt` | Remove TOURNAMENT case in `finishQuiz()`, remove per-question submission logic |

**DTOs to remove:**
- `TournamentEntryDto.kt`, `TournamentLeaderboardDto.kt`, and any tournament-related DTOs in `data/remote/dto/`

#### Dashboard RPC

In `get_user_dashboard`, remove the active tournament subquery that populates `active_tournament` in the response.

---

### Verification After Removal

1. Build the project: `./gradlew composeApp:assembleDebug`
2. Search for "tournament" (case-insensitive) across all `.kt` files — should have zero hits
3. Verify home screen loads without tournament banner
4. Verify navigation doesn't reference tournament routes
5. Verify DI graph resolves without tournament dependencies
