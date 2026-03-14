# Mindquest V2 - Android Developer Guide

## Table of Contents
1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Dependency Injection](#dependency-injection)
5. [Navigation](#navigation)
6. [Authentication Flow](#authentication-flow)
7. [Data Layer](#data-layer)
8. [Quiz System](#quiz-system)
9. [Network & API](#network--api)
10. [Error Handling](#error-handling)
11. [Theme System](#theme-system)
12. [Key Files Reference](#key-files-reference)

---

## Project Overview

Mindquest V2 is a **Kotlin Multiplatform (KMP)** educational quiz app targeting Android and iOS. It features module-based quizzes, IQ tests, daily challenges, live tournaments, leaderboards, and gamification (XP, levels, streaks). The backend is powered by **Supabase** (PostgreSQL + Auth + Realtime).

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin Multiplatform | 2.1.0 |
| UI | Compose Multiplatform | 1.7.3 |
| Design System | Material 3 | Latest |
| Backend | Supabase SDK (KMP) | 3.0.3 |
| HTTP | Ktor Client | 3.0.3 |
| DI | Koin | 4.0.0 |
| Serialization | kotlinx.serialization | 1.7.3 |
| Persistence | multiplatform-settings | 1.3.0 |
| Local Cache | SQLDelight | 2.2.1 |
| Image Loading | Coil 3 | 3.0.4 |
| Async | kotlinx.coroutines | 1.9.0 |

**Build Config:**
- `minSdk`: 26 (Android 8.0+)
- `targetSdk`: 35 (Android 15)
- `compileSdk`: 35
- Multiplatform targets: Android, iOS (arm64, x64, simulatorArm64)

Supabase credentials are injected via `gradle.properties` → `BuildConfig`:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
```

---

## Project Structure

All source code lives under `composeApp/src/commonMain/kotlin/com/android/mindquest/`.

```
mindquest/
├── core/                          # Infrastructure & utilities
│   ├── constants/AppConstants.kt  # Feature flags, Supabase URLs, XP rules
│   ├── network/
│   │   ├── SupabaseClientProvider.kt  # Singleton Supabase client builder
│   │   └── NetworkMonitor.kt         # Connectivity monitoring
│   ├── prefs/SessionPrefs.kt     # Persistent key-value session storage
│   ├── session/SessionProvider.kt # Extracts current user ID from auth session
│   ├── theme/
│   │   ├── Colors.kt             # Color palette (MindquestColors)
│   │   ├── Dimensions.kt         # Spacing, radius, sizing tokens
│   │   ├── Typography.kt         # Text styles (M3 scale)
│   │   └── AppTheme.kt           # MindquestTheme composable
│   └── util/
│       ├── Resource.kt           # Sealed class for repo results
│       ├── UiState.kt            # Sealed class for UI rendering
│       ├── ErrorMapper.kt        # Exception → user-facing messages
│       ├── AppLogger.kt          # Logging utility
│       └── RetryUtil.kt          # withRetry exponential backoff helper
│
├── cache/                         # SQLDelight offline cache (infrastructure)
│   └── MindquestDatabase.sq      # 7 cache tables (dashboard, modules, chapters, quizzes, stats, grades, pending_ops)
│
├── data/                          # Data layer (Repository implementations)
│   ├── remote/
│   │   ├── ApiService.kt         # All Supabase RPC + table operations
│   │   └── dto/                   # Data Transfer Objects (18+ files)
│   │       ├── DashboardDto.kt, ChapterDto.kt, QuizResultDto.kt
│   │       ├── LeaderboardDto.kt, ProfileDto.kt, StatsDto.kt
│   │       ├── TournamentEntryDto.kt, DailyChallengeDto.kt
│   │       └── ReferenceDto.kt, ...
│   ├── repository/                # 9 repository implementations
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── DashboardRepositoryImpl.kt
│   │   ├── ChapterRepositoryImpl.kt
│   │   ├── QuizRepositoryImpl.kt
│   │   ├── TournamentRepositoryImpl.kt
│   │   ├── LeaderboardRepositoryImpl.kt
│   │   ├── StatsRepositoryImpl.kt
│   │   ├── ProfileRepositoryImpl.kt
│   │   └── ReferenceDataRepositoryImpl.kt
│   ├── mapper/DtoMappers.kt      # DTO → Domain model extensions
│   └── mock/MockDataSource.kt    # Dev/offline mock data
│
├── domain/                        # Domain layer (pure Kotlin)
│   ├── model/                     # 16 domain models
│   │   ├── User.kt, Module.kt, Chapter.kt, Quiz.kt
│   │   ├── Question.kt           # QuestionType enum (15 types)
│   │   ├── QuizConfig.kt         # MODULE/IQ_TEST/TOURNAMENT behavior
│   │   ├── QuizResult.kt, Tournament.kt, DashboardData.kt
│   │   ├── LeaderboardEntry.kt, StatsData.kt, ProfileData.kt
│   │   ├── DailyChallenge.kt, ReferenceData.kt, UserStats.kt
│   │   └── QuizAnswer.kt
│   ├── repository/                # 9 repository interfaces
│   └── usecase/                   # 17 use cases
│       ├── GetDashboardUseCase.kt
│       ├── GetQuizWithQuestionsUseCase.kt
│       ├── SubmitQuizAttemptUseCase.kt
│       ├── StartTournamentUseCase.kt
│       └── ...
│
├── presentation/                  # UI layer (Compose)
│   ├── navigation/
│   │   ├── NavRoutes.kt          # Route string constants + helpers
│   │   ├── MindquestNavGraph.kt  # Unified NavHost setup
│   │   └── MainScreen.kt         # Root scaffold with bottom nav
│   ├── auth/                      # Authentication (5 files)
│   │   ├── AuthViewModel.kt      # States: MAIN/PHONE/OTP/VERIFIED/LINKING
│   │   ├── AuthScreen.kt         # Google/Phone/Anonymous buttons
│   │   ├── LoginJourneyScreen.kt # Onboarding profile setup
│   │   ├── PhoneOtpScreen.kt     # Phone + OTP verification
│   │   └── AccountLinkingSheet.kt
│   ├── home/                      # Home screen (7 files)
│   │   ├── HomeViewModel.kt, HomeScreen.kt
│   │   ├── HeroBanner.kt, SubjectCard.kt
│   │   ├── DailyChallengeCard.kt, IqTestCard.kt
│   │   └── TournamentHomeBanner.kt
│   ├── chapters/                  # Module browser
│   │   ├── ChaptersViewModel.kt, ChaptersScreen.kt
│   │   └── QuizIntroScreen.kt
│   ├── quiz/                      # Unified quiz system (7 files)
│   │   ├── QuizViewModel.kt      # State machine + answer evaluation
│   │   ├── QuizPlayScreen.kt     # All 15 question type UIs
│   │   ├── QuizResultScreen.kt   # Results + stars
│   │   ├── QuizReviewScreen.kt   # Answer review
│   │   ├── QuizSessionHolder.kt  # Singleton session bridge
│   │   └── visual/               # Visual question support
│   ├── tournament/                # Tournament flow (5 screens)
│   │   ├── TournamentViewModel.kt
│   │   ├── TournamentLobbyScreen.kt
│   │   ├── TournamentPlayScreen.kt
│   │   ├── TournamentPauseScreen.kt
│   │   └── TournamentResultScreen.kt
│   ├── leaderboard/
│   │   ├── LeaderboardViewModel.kt, LeaderboardScreen.kt
│   ├── stats/
│   │   ├── StatsViewModel.kt, StatsScreen.kt
│   ├── profile/
│   │   ├── ProfileViewModel.kt, ProfileScreen.kt
│   └── components/                # 13 reusable UI components
│       ├── BottomNavBar.kt        # 4-tab bottom nav
│       ├── MindquestButton.kt, MindquestTextField.kt
│       ├── AvatarView.kt, XpPill.kt, StarRating.kt
│       ├── ProgressBar.kt, LoadingView.kt, ErrorView.kt
│       ├── EmptyStateView.kt, CertificateFullViewDialog.kt
│       └── LinkAccountDialog.kt
│
└── di/AppModule.kt                # Koin DI container
```

---

## Dependency Injection

All dependencies are wired in `di/AppModule.kt` using **Koin**.

### Registration Patterns

| Pattern | Usage | Lifecycle |
|---------|-------|-----------|
| `single { ... }` | Infrastructure, repositories | App-scoped singleton |
| `factory { ... }` | Use cases | New instance per injection |
| `viewModelOf(::Class)` | ViewModels | Scoped to navigation destination |

### Registered Components

**Singletons (10):**
- `SupabaseClient` - via `SupabaseClientProvider.createClient()`
- `SessionPrefs` - persistent key-value storage
- `SessionProvider` - extracts `auth.uid()` from Supabase session
- `ApiService` - all backend calls
- 9 repository implementations (Auth, Dashboard, Chapter, Quiz, Tournament, Leaderboard, Stats, Profile, ReferenceData)

**Factories (16):**
- `GetDashboardUseCase`, `GenerateDailyChallengesUseCase`, `GetModuleFullUseCase`
- `GetChapterQuizzesUseCase`, `SubmitQuizAttemptUseCase`, `GetQuizWithQuestionsUseCase`
- `GetLeaderboardUseCase`, `GetUserStatsUseCase`, `GetProfileUseCase`, `UpdateProfileUseCase`
- `GetActiveTournamentUseCase`, `GetTournamentEntryUseCase`, `StartTournamentUseCase`
- `SubmitTournamentUseCase`, `SubmitSingleAnswerUseCase`, `GetReferenceDataUseCase`

**ViewModels (8):**
- `AuthViewModel`, `HomeViewModel`, `ChaptersViewModel`, `QuizViewModel`
- `LeaderboardViewModel`, `StatsViewModel`, `ProfileViewModel`, `TournamentViewModel`

### Injection in Composables

```kotlin
@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.dashboardState.collectAsState()
    // ...
}
```

---

## Navigation

### Route Definitions (NavRoutes.kt)

```
AUTH                    → "auth"
LOGIN_JOURNEY           → "login_journey"
HOME                    → "home"
LEADERBOARD             → "leaderboard"
STATS                   → "stats"
PROFILE                 → "profile"
CHAPTERS                → "chapters/{moduleId}/{moduleTitle}/{moduleEmoji}/{moduleColor}"
QUIZ_INTRO              → "quiz_intro/{moduleColor}"
QUIZ_PLAY               → "quiz_play/{moduleColor}"
QUIZ_RESULT             → "quiz_result/{moduleColor}"
QUIZ_REVIEW             → "quiz_review"
TOURNAMENT_LOBBY        → "tournament_lobby"
TOURNAMENT_PLAY         → "tournament_play"
TOURNAMENT_PAUSE        → "tournament_pause"
TOURNAMENT_RESULT       → "tournament_result"
```

### Navigation Flow

```
AUTH / LOGIN_JOURNEY (onboarding)
    |
    v
HOME (main hub) ────────────────────────────────────
    |                    |              |            |
    v                    v              v            v
 CHAPTERS          LEADERBOARD       STATS       PROFILE
    |                                   |
    v                                   v
 QUIZ_INTRO                        IQ Test entry
    |                                   |
    v                                   v
 QUIZ_PLAY  <───────────────────── QUIZ_PLAY
    |
    v
 QUIZ_RESULT
    |
    v
 QUIZ_REVIEW

HOME
    |
    v
 TOURNAMENT_LOBBY
    |
    v
 QUIZ_PLAY (tournament config)
    |         |
    v         v
 TOURNAMENT_RESULT   TOURNAMENT_PAUSE
```

### Bottom Navigation

4 tabs: **Home**, **Leaderboard**, **Stats**, **Profile**

Bottom nav is visible only on `NavRoutes.BOTTOM_NAV_ROUTES`. Navigation uses `popUpTo(HOME) { saveState = true }` + `launchSingleTop` + `restoreState` for proper back stack management.

### Unified Quiz System

A single `QUIZ_PLAY` route handles all quiz modes. The `QuizSessionHolder` singleton bridges data between screens:

1. Entry point calls `QuizSessionHolder.selectQuiz(quiz, config)` or `selectQuizById(id, config)`
2. `QuizPlayScreen` reads from `QuizViewModel.startFromSession()`
3. On finish, `saveCompletedData()` stores results for `QuizReviewScreen`
4. Exit quiz flow calls `QuizSessionHolder.clear()`

---

## Authentication Flow

### Auth Providers

| Provider | Method | Implementation |
|----------|--------|---------------|
| Google | OAuth 2.0 | `supabaseClient.auth.signInWith(Google)` |
| Phone | OTP (SMS) | `signInWith(Phone)` + `verifyPhoneOtp()` |
| Anonymous | No credentials | `signInAnonymously()` |

### AuthViewModel States

```
MAIN ──→ PHONE ──→ OTP ──→ VERIFIED
  |                           |
  └─────────────────── LINKING (account merge)
```

| State | Description |
|-------|-------------|
| `MAIN` | Google/Phone/Anonymous buttons |
| `PHONE` | Phone number input |
| `OTP` | 6-digit OTP code entry |
| `VERIFIED` | Post-OTP success |
| `LINKING` | Account provider linking |

### Session Management

`SessionPrefs` wraps **multiplatform-settings** (SharedPreferences on Android, NSUserDefaults on iOS):

- `isLoggedIn: Boolean` - persisted login flag
- `dailyChallengeDate: String` - cached daily challenge date
- `dailyChallengesJson: String` - cached challenge data
- `clear()` - wipe all session data on sign out

**App Launch Flow:**
1. `AuthViewModel.checkSession()` on app start
2. If `SessionPrefs.isLoggedIn` and Supabase session valid → navigate to HOME
3. Otherwise → show AUTH screen

### Onboarding Profile

New users go through `LoginJourneyScreen` to set:
- Display name, avatar ID, grade
- Optional: country, city, school name

Reference data (grades, countries, cities) is fetched from Supabase via `AuthViewModel`.

---

## Data Layer

### Repository Pattern

Every feature follows: **Interface (domain) → Implementation (data) → Use Case → ViewModel**

```
domain/repository/QuizRepository.kt         # Interface
data/repository/QuizRepositoryImpl.kt        # Implementation
domain/usecase/GetQuizWithQuestionsUseCase.kt # Use Case
presentation/quiz/QuizViewModel.kt           # Consumer
```

### DTO Mapping

All DTOs are `@Serializable` data classes in `data/remote/dto/`. Extensions in `DtoMappers.kt` convert them to domain models:

```kotlin
fun QuestionDto.toDomain(): Question = Question(
    id = id,
    questionType = QuestionType.fromString(questionType),
    title = title,
    prompt = prompt,
    explanation = explanation,
    options = options.map { it.toDomain() },
    matchPairs = matchPairs?.map { it.toDomain() },
    promptConfig = promptConfig?.let { jsonObjectToMap(it) },
    metadata = metadata?.let { jsonObjectToMap(it) }
)
```

### ApiService

Central class wrapping all Supabase operations. Uses PostgREST RPC calls:

```kotlin
class ApiService(private val client: SupabaseClient) {
    suspend fun getUserDashboard(userId: String): DashboardResponseDto
    suspend fun getQuizWithQuestions(quizId: String, userId: String): QuizDto
    suspend fun submitQuizAttempt(payload: JsonObject): QuizResultDto
    suspend fun startTournament(userId: String, tournamentId: String): TournamentStartResponseDto
    // ... 20+ methods
}
```

**RPC Call Pattern:**
```kotlin
suspend fun getUserDashboard(userId: String): DashboardResponseDto {
    return client.postgrest.rpc("get_user_dashboard", buildJsonObject {
        put("p_user_id", userId)
    }).decodeAs()
}
```

---

## Quiz System

### QuizConfig - Behavioral Control

Three factory methods produce different quiz behaviors:

| Config | `QuizConfig.module()` | `QuizConfig.iqTest()` | `QuizConfig.tournament()` |
|--------|----------------------|----------------------|--------------------------|
| **Feedback** | Green/red + explanation | None | None |
| **Nudges** | Enabled | Disabled | Disabled |
| **Auto-advance** | Manual (Next button) | 300ms delay | 300ms delay |
| **Submission** | Batch at end | Batch at end | Per-question (async) |
| **Pause** | No | No | Optional |
| **Review** | Yes | No | No |
| **Retry** | Yes | No | No |
| **Accent** | Module color | Green (#10B981) | Indigo (#4F46E5) |

### QuizViewModel State Machine

**QuizPlayState** holds per-type answer fields:
- `selectedOptionId: String?` - MCQ, True/False, Matrix, Grid, Visual, Statement/Reason
- `orderedOptionIds: List<String>` - Ordering
- `matchedPairs: Map<String, String>` - Match
- `fillBlankAnswer: String` - Fill Blank
- `selectedWordIds: Set<String>` - Select Word
- `sequenceTapIds: List<String>` - Sequence Tap

**Flow:** `loadQuiz() → displayQuestion → userInteraction → confirmAnswer → evaluateAnswer → showFeedback → nextQuestion → finishQuiz`

### 15 Question Types

| Type | UI Component | Answer State | Evaluation |
|------|-------------|-------------|------------|
| MULTIPLE_CHOICE | `McqOptionsGrid` | `selectedOptionId` | Compare to `isCorrect` option |
| TRUE_FALSE | `TrueFalseOptions` | `selectedOptionId` | Compare to `isCorrect` option |
| ORDERING | `OrderingSection` | `orderedOptionIds` | List equality vs `correctPosition` order |
| MATCH | `MatchSection` | `matchedPairs` | All pairs must match `rightText` |
| FILL_BLANK | `FillBlankSection` | `fillBlankAnswer` | Case-insensitive string compare |
| SELECT_WORD | `SelectWordSection` | `selectedWordIds` | Set equality of correct option IDs |
| MATRIX | `MatrixSection` | `selectedOptionId` | MCQ (grid + options below) |
| GRID_PATTERN | `GridPatternSection` | `selectedOptionId` | MCQ (grid + options below) |
| STATEMENT_REASON | `StatementReasonSection` | `selectedOptionId` | MCQ (statement/reason cards + options) |
| TABLE_DATA | `TableDataSection` | `selectedOptionId` | MCQ (table + options below) |
| MEMORY | `MemorySection` | `selectedOptionId` | MCQ (memory grid + options) |
| VISUAL_SINGLE_CHOICE | `VisualChoiceGrid` | `selectedOptionId` | MCQ (visual cards with images) |
| GRID_CELL_SELECT | `GridCellSelectSection` | `selectedOptionId` | MCQ (grid with "?" cell + options) |
| GRID_PATTERN_BOOLEAN | `GridPatternBooleanSection` | `selectedOptionId` | True/False (grid + property badge) |
| SEQUENCE_TAP | `SequenceTapSection` | `sequenceTapIds` | List equality vs `correctPosition` order |

### Data Structures for Special Types

**Grid-based types** (Matrix, GridPattern, GridCellSelect, TableData, Memory):
```json
// promptConfig
{"grid": [["A","B","C"],["D","?","F"],["G","H","I"]]}
// or
{"rows": [["15","20","25"],["10","30","?"]]}
```

**StatementReason:**
```json
// metadata
{"statement": "Water boils at 100C", "reason": "Atmospheric pressure at sea level"}
```

**GridPatternBoolean:**
```json
// metadata
{"property": "symmetry", "axis": "vertical"}
```

**SequenceTap:**
```json
// promptConfig
{"show_duration_ms": 3000}
```

---

## Network & API

### SupabaseClientProvider

Lazy singleton with 3 plugins:

```kotlin
object SupabaseClientProvider {
    fun createClient(url: String, key: String): SupabaseClient {
        return createSupabaseClient(url, key) {
            install(Auth)        // Authentication
            install(Postgrest)   // Database queries/RPCs
            install(Realtime)    // Real-time subscriptions
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }
}
```

### JSON Handling

Tolerant parsing for schema evolution:
- `ignoreUnknownKeys = true` - new backend fields don't break app
- `isLenient = true` - accepts quoted numbers, unquoted strings
- `coerceInputValues = true` - null → default for non-nullable fields

### JSONB Fields

`promptConfig` and `metadata` are stored as `JSONB` in Supabase and parsed via `jsonObjectToMap()` into `Map<String, Any>`. Values can be `JsonElement`, `Number`, or `String` - always handle with type-safe extraction:

```kotlin
val grid = promptConfig?.get("grid")
when (grid) {
    is JsonArray -> // parse as 2D array
    is String -> // parse as JSON string
    else -> // fallback
}
```

---

## Error Handling

### Two-Layer Pattern

**Repository Layer - `Resource<T>`:**
```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data class Loading<T>(val data: T? = null) : Resource<T>()
}
```

**UI Layer - `UiState<T>`:**
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
    data object Offline : UiState<Nothing>()
}
```

**ViewModel Conversion:**
```kotlin
viewModelScope.launch {
    _uiState.value = UiState.Loading
    when (val result = getQuizUseCase(quizId, userId)) {
        is Resource.Success -> _uiState.value = UiState.Success(result.data)
        is Resource.Error -> _uiState.value = UiState.Error(result.message)
    }
}
```

**Composable Rendering:**
```kotlin
when (val state = uiState.collectAsState().value) {
    is UiState.Loading -> LoadingView()
    is UiState.Success -> ContentView(state.data)
    is UiState.Error -> ErrorView(state.message, onRetry = { viewModel.retry() })
    is UiState.Empty -> EmptyStateView()
    is UiState.Offline -> OfflineBanner()
}
```

### Retry & Exception Safety

**Exponential Backoff (withRetry):**

All read repositories use `withRetry` for automatic retry with exponential backoff:
```kotlin
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500,
    block: suspend () -> T
): T
```

**CoroutineExceptionHandler:**

All 8 ViewModels include a `CoroutineExceptionHandler` to prevent unhandled exceptions from canceling sibling coroutines:
```kotlin
private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    AppLogger.e("ViewModel", "Unhandled coroutine exception", throwable as? Exception)
}

viewModelScope.launch(exceptionHandler) {
    // safe from unhandled exceptions
}
```

---

## Theme System

### Colors (MindquestColors)

**Primary:** Indigo (#4F46E5) / Dark (#4338CA) / Light (#6366F1) / Container (#E0E7FF)
**Secondary:** Purple (#7C3AED)
**Success:** Green (#22C55E) | **Warning:** Orange (#F59E0B) | **Error:** Red (#EF4444)

**Subject Colors:**
- Math: Indigo (#6366F1), Science: Green (#10B981), English: Orange (#F59E0B)
- Logic: Purple (#8B5CF6), Social Studies: Blue (#0EA5E9)

**Leaderboard:** Gold (#FFB800), Silver (#9BA8B8), Bronze (#C0784A)

### Dimensions

Access via `MaterialTheme.dimens`:
- **Spacing:** XXS (2dp) through Huge (32dp)
- **Radius:** Small (8dp) through Round (50dp)
- **Component:** cardRadius (20dp), buttonRadius (14dp), bottomNavHeight (80dp), buttonHeight (50dp)
- **Avatar:** Small (34dp), Medium (44dp), Large (72dp)

### Typography

M3 scale from Display (34sp/Black) down to Label (10sp/SemiBold). Extra: `MindquestExtraTypography.Caption` (9sp).

### Usage

```kotlin
@Composable
fun MindquestTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(
        LocalDimensions provides DefaultDimensions,
        LocalMindquestColors provides if (isSystemInDarkTheme()) darkMindquestColors else lightMindquestColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MindquestTypography,
            content = content,
        )
    }
}
```

---

## Key Files Reference

| File | Location | Purpose |
|------|----------|---------|
| `AppModule.kt` | `di/` | All Koin DI wiring |
| `NavRoutes.kt` | `presentation/navigation/` | Route string constants |
| `MindquestNavGraph.kt` | `presentation/navigation/` | NavHost + composable destinations |
| `MainScreen.kt` | `presentation/navigation/` | Root scaffold with bottom nav |
| `AuthViewModel.kt` | `presentation/auth/` | Session check, auth state machine |
| `QuizViewModel.kt` | `presentation/quiz/` | Quiz state, answer evaluation, submission |
| `QuizPlayScreen.kt` | `presentation/quiz/` | All 15 question type UIs |
| `QuizConfig.kt` | `domain/model/` | MODULE/IQ_TEST/TOURNAMENT behavior flags |
| `QuizSessionHolder.kt` | `presentation/quiz/` | Singleton quiz session bridge |
| `Question.kt` | `domain/model/` | QuestionType enum (15 types), QuestionOption, MatchPair |
| `ApiService.kt` | `data/remote/` | All Supabase RPC/table operations |
| `SupabaseClientProvider.kt` | `core/network/` | Supabase client builder |
| `SessionPrefs.kt` | `core/prefs/` | Persistent login state |
| `DtoMappers.kt` | `data/mapper/` | DTO → Domain model conversions |
| `Resource.kt` | `core/util/` | Repository result wrapper |
| `UiState.kt` | `core/util/` | UI rendering state wrapper |
| `Colors.kt` | `core/theme/` | Color palette |
| `Dimensions.kt` | `core/theme/` | Spacing and sizing tokens |
| `AppTheme.kt` | `core/theme/` | MindquestTheme composable |
| `TournamentViewModel.kt` | `presentation/tournament/` | Tournament lifecycle management |
| `HomeViewModel.kt` | `presentation/home/` | Dashboard data + daily challenges |
| `AppConstants.kt` | `core/constants/` | Feature flags, XP rules, URLs |
| `RetryUtil.kt` | `core/util/` | withRetry exponential backoff helper |
| `QuizStateManager.kt` | `presentation/quiz/` | Persists quiz state for process death recovery |
| `MindquestDatabase.sq` | `commonMain/sqldelight/` | SQLDelight offline cache schema (7 tables) |
| `TournamentLeaderboardDto.kt` | `data/remote/dto/` | Tournament-specific leaderboard response DTO |
| `build.gradle.kts` | `composeApp/` | Dependencies, SDK versions, build config |

---

## Conventions

1. **All ViewModels** expose `StateFlow<UiState<T>>` for consistent UI rendering
2. **All repositories** return `Resource<T>` for type-safe error propagation
3. **All use cases** have a single `suspend operator fun invoke()` method
4. **Navigation** uses `QuizSessionHolder` singleton to pass quiz data between screens
5. **Platform-specific** code goes in `androidMain/` and `iosMain/` source sets
6. **Supabase config** in `gradle.properties` (never committed to git)
7. **All read repositories** use `withRetry` for exponential backoff on transient failures
8. **All ViewModels** include `CoroutineExceptionHandler` to prevent coroutine cancellation cascades
9. **Dark mode** is system-aware via `isSystemInDarkTheme()` with separate light/dark color palettes
