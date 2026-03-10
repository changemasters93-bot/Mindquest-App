package com.android.mindquest.di

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.network.SupabaseClientProvider
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.data.repository.AuthRepositoryImpl
import com.android.mindquest.data.repository.ChapterRepositoryImpl
import com.android.mindquest.data.repository.DashboardRepositoryImpl
import com.android.mindquest.data.repository.LeaderboardRepositoryImpl
import com.android.mindquest.data.repository.ProfileRepositoryImpl
import com.android.mindquest.data.repository.QuizRepositoryImpl
import com.android.mindquest.data.repository.ReferenceDataRepositoryImpl
import com.android.mindquest.data.repository.StatsRepositoryImpl
import com.android.mindquest.data.repository.TournamentRepositoryImpl
import com.android.mindquest.domain.repository.AuthRepository
import com.android.mindquest.domain.repository.ChapterRepository
import com.android.mindquest.domain.repository.DashboardRepository
import com.android.mindquest.domain.repository.LeaderboardRepository
import com.android.mindquest.domain.repository.ProfileRepository
import com.android.mindquest.domain.repository.QuizRepository
import com.android.mindquest.domain.repository.ReferenceDataRepository
import com.android.mindquest.domain.repository.StatsRepository
import com.android.mindquest.domain.repository.TournamentRepository
import com.android.mindquest.domain.usecase.GetActiveTournamentUseCase
import com.android.mindquest.domain.usecase.GetChapterQuizzesUseCase
import com.android.mindquest.domain.usecase.GetDailyChallengesUseCase
import com.android.mindquest.domain.usecase.GetDashboardUseCase
import com.android.mindquest.domain.usecase.GetLeaderboardUseCase
import com.android.mindquest.domain.usecase.GetModuleFullUseCase
import com.android.mindquest.domain.usecase.GetProfileUseCase
import com.android.mindquest.domain.usecase.GetReferenceDataUseCase
import com.android.mindquest.domain.usecase.GetUserStatsUseCase
import com.android.mindquest.domain.usecase.StartTournamentUseCase
import com.android.mindquest.domain.usecase.SubmitQuizAttemptUseCase
import com.android.mindquest.domain.usecase.SubmitSingleAnswerUseCase
import com.android.mindquest.domain.usecase.SubmitTournamentUseCase
import com.android.mindquest.domain.usecase.UpdateProfileUseCase
import com.android.mindquest.presentation.auth.AuthViewModel
import com.android.mindquest.presentation.chapters.ChaptersViewModel
import com.android.mindquest.presentation.home.HomeViewModel
import com.android.mindquest.presentation.leaderboard.LeaderboardViewModel
import com.android.mindquest.presentation.profile.ProfileViewModel
import com.android.mindquest.presentation.quiz.QuizViewModel
import com.android.mindquest.presentation.stats.StatsViewModel
import com.android.mindquest.presentation.tournament.TournamentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // ── Supabase client ─────────────────────────────────────────────────
    single {
        SupabaseClientProvider.createClient(
            url = AppConstants.SUPABASE_URL,
            key = AppConstants.SUPABASE_ANON_KEY,
        )
    }

    // ── API service ─────────────────────────────────────────────────────
    single { ApiService(get()) }

    // ── Repositories ────────────────────────────────────────────────────
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    single<ChapterRepository> { ChapterRepositoryImpl(get()) }
    single<QuizRepository> { QuizRepositoryImpl(get()) }
    single<TournamentRepository> { TournamentRepositoryImpl(get()) }
    single<LeaderboardRepository> { LeaderboardRepositoryImpl(get()) }
    single<StatsRepository> { StatsRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<ReferenceDataRepository> { ReferenceDataRepositoryImpl(get()) }

    // ── Use cases ───────────────────────────────────────────────────────
    factory { GetDashboardUseCase(get()) }
    factory { GetDailyChallengesUseCase(get()) }
    factory { GetModuleFullUseCase(get()) }
    factory { GetChapterQuizzesUseCase(get()) }
    factory { SubmitQuizAttemptUseCase(get()) }
    factory { GetLeaderboardUseCase(get()) }
    factory { GetUserStatsUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { UpdateProfileUseCase(get()) }
    factory { GetActiveTournamentUseCase(get()) }
    factory { StartTournamentUseCase(get()) }
    factory { SubmitTournamentUseCase(get()) }
    factory { SubmitSingleAnswerUseCase(get()) }
    factory { GetReferenceDataUseCase(get()) }

    // ── ViewModels ──────────────────────────────────────────────────────
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ChaptersViewModel)
    viewModelOf(::QuizViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::TournamentViewModel)
}
