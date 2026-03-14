# Mindquest - Architecture Documentation

## Overview

Mindquest is a **Compose Multiplatform (KMP)** educational quiz application targeting **Android** and **iOS**. Built for Indian students (Classes 1-8, CBSE curriculum), it features quizzes, tournaments, leaderboards, and gamification (XP/levels).

**Tech Stack:**
- Kotlin Multiplatform + Compose Multiplatform 1.7.3
- Kotlin 2.1.0
- Supabase (Auth, Postgrest RPCs, Realtime)
- Koin 4.0.0 (Dependency Injection)
- Ktor 3.0.3 (HTTP client - used by Supabase SDK)
- kotlinx-serialization (JSON)
- SQLDelight 2.2.1 (Local cache infrastructure)

---

## Project Structure

```
composeApp/src/
  commonMain/          # Shared code (95%+ of the app)
    kotlin/com/android/mindquest/
      App.kt                           # Root composable
      core/                            # Cross-cutting concerns
        constants/AppConstants.kt      # Config flags, Supabase keys
        network/
          NetworkMonitor.kt            # expect class
          SupabaseClientProvider.kt    # Thread-safe singleton
        theme/
          Colors.kt                    # MindquestColors object
          Typography.kt               # MindquestTypography
          Dimensions.kt               # Responsive dimensions
          AppTheme.kt                  # MindquestTheme composable
        util/
          Resource.kt                  # Success/Error/Loading (data layer)
          UiState.kt                   # Loading/Success/Error/Empty/Offline
          RetryUtil.kt                 # withRetry exponential backoff
          AppLogger.kt                 # Logging utility
    sqldelight/                        # SQLDelight offline cache
      com/android/mindquest/cache/
        MindquestDatabase.sq           # 7 cache tables
      domain/                          # Pure Kotlin, no framework deps
        model/                         # 16+ domain models
        repository/                    # 9 repository interfaces
        usecase/                       # 17 use cases
      data/                            # Implementation layer
        remote/
          ApiService.kt               # Supabase RPC calls
          dto/                         # 18+ DTO classes
        mapper/DtoMappers.kt          # DTO -> Domain mappers
        mock/MockDataSource.kt        # Realistic mock data
        repository/                    # 9 repository implementations
      di/AppModule.kt                  # Koin DI module
      presentation/                    # UI layer
        navigation/
          NavRoutes.kt                 # Route constants + helpers
          MindquestNavGraph.kt         # Root NavHost
          MainScreen.kt               # Tab host with bottom nav
        components/                    # 13+ reusable components
        auth/                          # AuthScreen, PhoneOtpScreen, etc.
        home/                          # HomeScreen, HeroBanner, SubjectCard
        chapters/                      # ChaptersScreen, QuizIntroScreen
        quiz/                          # QuizPlayScreen, ResultScreen, ReviewScreen
        leaderboard/                   # LeaderboardScreen
        stats/                         # StatsScreen
        profile/                       # ProfileScreen
        tournament/                    # Lobby, Play, Pause, Result screens
  androidMain/                         # Android-specific
    AndroidManifest.xml
    kotlin/.../
      MainActivity.kt                 # ComponentActivity
      MindquestApplication.kt         # Koin init
      core/network/NetworkMonitor.android.kt
  iosMain/                             # iOS-specific
    kotlin/.../
      MainViewController.kt           # ComposeUIViewController
      core/network/NetworkMonitor.ios.kt
```

**Total: 120+ source files** (Kotlin + 1 XML manifest + 1 SQLDelight schema + Gradle configs)

---

## Architecture Diagram

```
+----------------------------------------------------------+
|                    PRESENTATION LAYER                      |
|                                                            |
|  Screens (Composable)  <-->  ViewModels (StateFlow)       |
|  - HomeScreen                - HomeViewModel               |
|  - QuizPlayScreen            - QuizViewModel               |
|  - LeaderboardScreen         - LeaderboardViewModel        |
|  - TournamentPlayScreen      - TournamentViewModel         |
|  - StatsScreen               - StatsViewModel              |
|  - ProfileScreen             - ProfileViewModel            |
|  - ChaptersScreen            - ChaptersViewModel           |
|  - AuthScreen                - AuthViewModel               |
|                                                            |
|  Navigation: MindquestNavGraph + NavRoutes                 |
|  DI: Koin (koinViewModel<T>())                            |
+----------------------------------------------------------+
                           |
                    UiState<T> / StateFlow
                           |
+----------------------------------------------------------+
|                      DOMAIN LAYER                         |
|                                                            |
|  Use Cases (operator fun invoke)                          |
|  - GetDashboardUseCase     - SubmitQuizAttemptUseCase     |
|  - GetDailyChallengesUseCase - GetLeaderboardUseCase      |
|  - GetModuleFullUseCase    - GetUserStatsUseCase          |
|  - GetChapterQuizzesUseCase - GetProfileUseCase           |
|  - GetActiveTournamentUseCase - UpdateProfileUseCase      |
|  - StartTournamentUseCase  - SubmitTournamentUseCase      |
|  - SignInUseCase           - GetReferenceDataUseCase      |
|                                                            |
|  Repository Interfaces (9 contracts)                      |
|  Domain Models (15 data classes/enums)                    |
+----------------------------------------------------------+
                           |
                    Resource<T>
                           |
+----------------------------------------------------------+
|                       DATA LAYER                          |
|                                                            |
|  Repository Implementations (9)                           |
|    -> Check USE_MOCK_DATA flag                            |
|    -> If true: return MockDataSource data                 |
|    -> If false: call ApiService -> DTO -> Mapper          |
|                                                            |
|  ApiService (17+ Supabase RPCs via Postgrest)              |
|  DTOs (9 serializable classes)                            |
|  DtoMappers (extension functions)                         |
|  MockDataSource (realistic test data)                     |
+----------------------------------------------------------+
                           |
                    Supabase SDK
                           |
+----------------------------------------------------------+
|                   EXTERNAL SERVICES                       |
|                                                            |
|  Supabase Backend                                         |
|  - Auth: Google OAuth, Phone OTP, Anonymous               |
|  - Postgrest: 17+ RPC functions                            |
|  - Realtime: Tournament leaderboard subscriptions         |
+----------------------------------------------------------+
```

---

## API Integration

### Supabase RPC Functions

The app communicates with Supabase exclusively through **RPC functions** (never direct table queries, except 3 reference tables). All RPCs are called via `ApiService.kt`.

| RPC Function | Use Case | Description |
|---|---|---|
| `get_user_dashboard` | GetDashboardUseCase | User info, stats, modules, active tournament |
| `get_daily_challenges` | GetDailyChallengesUseCase | Today's challenge quizzes |
| `get_module_full` | GetModuleFullUseCase | Module + chapters with progress |
| `get_chapter_quizzes` | GetChapterQuizzesUseCase | Quizzes for a specific chapter |
| `submit_quiz_attempt` | SubmitQuizAttemptUseCase | Submit answers with idempotency key |
| `get_active_tournament` | GetActiveTournamentUseCase | Current live tournament |
| `start_tournament` | StartTournamentUseCase | Join and start a tournament |
| `submit_tournament` | SubmitTournamentUseCase | Submit tournament answers |
| `get_leaderboard` | GetLeaderboardUseCase | Ranked users by country/city/school |
| `get_user_stats` | GetUserStatsUseCase | Detailed user statistics |
| `get_profile` | GetProfileUseCase | User profile with history |
| `update_profile` | UpdateProfileUseCase | Update display name/avatar |
| `pause_tournament` | (TournamentViewModel) | Pause active tournament |
| `resume_tournament` | (TournamentViewModel) | Resume paused tournament |

### Database Schema (4 schemas)

- **public**: `users` table (auth-linked)
- **content**: `modules`, `chapters`, `quizzes`, `questions` (curriculum data)
- **user_raw**: `quiz_attempts`, `tournament_entries` (raw user actions)
- **user_derived**: `user_stats`, `module_progress`, `chapter_progress` (computed aggregates)

### Data Flow Pattern

```
Screen -> ViewModel.action()
  -> UseCase(params): Resource<T>
    -> Repository.method(params): Resource<T>
      -> if (USE_MOCK_DATA) MockDataSource.getData()
      -> else ApiService.rpc() -> DtoMapper.toDomain()
    <- Resource.Success(data) | Resource.Error(message)
  <- UiState.Success(data) | UiState.Error | UiState.Loading
Screen observes StateFlow<UiState<T>>
```

---

## Mock Data System

The app includes a complete mock data system controlled by a single flag:

```kotlin
// AppConstants.kt
const val USE_MOCK_DATA = true  // Toggle mock vs real API
```

### How It Works

1. Every `*RepositoryImpl` checks `AppConstants.USE_MOCK_DATA` at the top of each method
2. If `true`: returns data from `MockDataSource` wrapped in `Resource.Success`
3. If `false`: makes real Supabase RPC call via `ApiService`

### MockDataSource

Located at `data/mock/MockDataSource.kt`, provides realistic data for:
- Dashboard (user, stats, 8 subject modules, active tournament)
- Daily challenges
- Module chapters with progress states (LOCKED/UNLOCKED/COMPLETED)
- Quiz questions (multiple choice with 4 options)
- Leaderboard entries
- User stats (daily activity, subject performance)
- Profile data (completed chapters, tournament history)
- Tournament data

This enables full UI development and testing without a live backend.

---

## Key Patterns

### State Management
- **ViewModels** use `MutableStateFlow` internally, expose `StateFlow`
- **UiState<T>** sealed class: `Loading`, `Success<T>`, `Error(message)`, `Empty`, `Offline`
- **Resource<T>** sealed class (data layer): `Success<T>`, `Error(message)`, `Loading<T>`
- **Dark mode** system-aware via `isSystemInDarkTheme()` with light/dark color palettes
- **withRetry** exponential backoff on all read repositories for transient failure resilience
- **CoroutineExceptionHandler** on all 8 ViewModels to prevent coroutine cancellation cascades

### Dependency Injection
- **Koin 4.0.0** with `viewModelOf(::ClassName)` for auto-wiring
- All repos registered as `single<Interface> { Impl(get()) }`
- Use cases registered as `factory { UseCase(get()) }`
- ViewModels obtained in Compose via `koinViewModel<T>()`

### Navigation
- Single `NavHost` with route-based navigation
- Bottom nav visible only on 4 main tabs (Home, Leaderboard, Stats, Profile)
- Deep screen flows: Home -> Chapters -> Quiz Play -> Quiz Result -> Quiz Review
- Tournament flow: Lobby -> Play -> Pause/Result

### Platform Abstraction
- `expect/actual` pattern for `NetworkMonitor`
- Android: `ConnectivityManager` + `NetworkCallback`
- iOS: Stub (defaults to `isOnline = true`)

---

## Screen Inventory

| Screen | ViewModel | Key Features |
|---|---|---|
| AuthScreen | AuthViewModel | Google OAuth, Phone OTP, Anonymous auth |
| HomeScreen | HomeViewModel | Dashboard, daily challenges, subject grid |
| ChaptersScreen | ChaptersViewModel | Expandable chapter list with quiz cards |
| QuizPlayScreen | QuizViewModel | Timer, animated option cards, confirm flow |
| QuizResultScreen | QuizViewModel | Star rating, accuracy circle, XP earned |
| QuizReviewScreen | QuizViewModel | Answer review with correct/incorrect |
| LeaderboardScreen | LeaderboardViewModel | Top 3 podium, filter by scope |
| StatsScreen | StatsViewModel | Overview cards, activity chart, subjects |
| ProfileScreen | ProfileViewModel | Avatar, stats, history, edit profile |
| TournamentLobbyScreen | TournamentViewModel | Countdown, player count, start button |
| TournamentPlayScreen | TournamentViewModel | Live badge, timer, auto-advance |
| TournamentPauseScreen | TournamentViewModel | Time remaining, resume/quit |
| TournamentResultScreen | TournamentViewModel | Medal, rank, score breakdown |

---

## Reusable Components

| Component | File | Purpose |
|---|---|---|
| PrimaryButton / SecondaryButton | MindquestButton.kt | Gradient and outlined buttons |
| MindquestTextField | MindquestTextField.kt | Styled text input |
| LoadingView / ShimmerBox | LoadingView.kt | Loading indicators |
| ErrorView / OfflineView | ErrorView.kt | Error states with retry |
| EmptyStateView | EmptyStateView.kt | Empty content state |
| XpPill | XpPill.kt | XP + level indicator |
| AvatarView | AvatarView.kt | User avatar with gradient circle |
| MindquestBottomNav | BottomNavBar.kt | Bottom navigation bar |
| MindquestProgressBar | ProgressBar.kt | Animated progress bar |
| StarRating | StarRating.kt | Star emoji rating display |
