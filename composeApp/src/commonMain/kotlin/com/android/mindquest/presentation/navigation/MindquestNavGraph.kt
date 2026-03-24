package com.android.mindquest.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.session.SessionProvider
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.QuizBehavior
import com.android.mindquest.domain.model.QuizConfig
import com.android.mindquest.presentation.auth.AuthScreen
import com.android.mindquest.presentation.auth.AuthViewModel
import com.android.mindquest.presentation.auth.LoginJourneyScreen
import com.android.mindquest.presentation.chapters.ChaptersScreen
import com.android.mindquest.presentation.chapters.ChaptersViewModel
import com.android.mindquest.presentation.components.MindquestBottomNav
import com.android.mindquest.presentation.home.HomeScreen
import com.android.mindquest.presentation.home.HomeViewModel
import com.android.mindquest.presentation.leaderboard.LeaderboardScreen
import com.android.mindquest.presentation.leaderboard.LeaderboardViewModel
import com.android.mindquest.presentation.profile.ProfileScreen
import com.android.mindquest.presentation.profile.ProfileViewModel
import com.android.mindquest.presentation.quiz.QuizIntroScreen
import com.android.mindquest.presentation.quiz.QuizPlayScreen
import com.android.mindquest.presentation.quiz.QuizReviewScreen
import com.android.mindquest.presentation.quiz.QuizSessionHolder
import com.android.mindquest.presentation.quiz.QuizViewModel
import com.android.mindquest.presentation.stats.StatsScreen
import com.android.mindquest.presentation.stats.StatsViewModel
import com.android.mindquest.presentation.tournament.TournamentLobbyScreen
import com.android.mindquest.presentation.tournament.TournamentPauseScreen
import com.android.mindquest.presentation.tournament.TournamentPlayScreen
import com.android.mindquest.presentation.tournament.TournamentResultScreen
import com.android.mindquest.presentation.tournament.TournamentViewModel
import org.koin.compose.koinInject

@Composable
fun MindquestNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val sessionProvider = koinInject<SessionProvider>()
    val userId = sessionProvider.userId

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomNav = currentRoute in NavRoutes.BOTTOM_NAV_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                MindquestBottomNav(
                    currentRoute = currentRoute ?: NavRoutes.HOME,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.LOGIN_JOURNEY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Login Journey (onboarding + profile setup + auth) ────────
            composable(NavRoutes.LOGIN_JOURNEY) {
                val viewModel = koinInject<AuthViewModel>()
                LoginJourneyScreen(
                    viewModel = viewModel,
                    onComplete = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LOGIN_JOURNEY) { inclusive = true }
                        }
                    },
                )
            }

            // ── Auth ────────────────────────────────────────────────────
            composable(NavRoutes.AUTH) {
                val viewModel = koinInject<AuthViewModel>()
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    },
                    viewModel = viewModel,
                )
            }

            // ── Main tabs ───────────────────────────────────────────────
            composable(NavRoutes.HOME) { backStackEntry ->
                val viewModel = koinInject<HomeViewModel>()

                // ── Mark completed quiz as done locally ──────────────────
                val completedQuizId = backStackEntry.savedStateHandle
                    .getStateFlow("completedQuizId", "")
                    .collectAsState()
                LaunchedEffect(completedQuizId.value) {
                    val qid = completedQuizId.value
                    if (qid.isNotBlank()) {
                        viewModel.markChallengeDone(qid)
                        backStackEntry.savedStateHandle["completedQuizId"] = ""
                    }
                }

                // ── Refresh dashboard when returning from quiz ───────────
                val needsRefresh = backStackEntry.savedStateHandle
                    .getStateFlow("needsRefresh", false)
                    .collectAsState()
                LaunchedEffect(needsRefresh.value) {
                    if (needsRefresh.value) {
                        viewModel.refreshAll()
                        backStackEntry.savedStateHandle["needsRefresh"] = false
                    }
                }

                // ── Lifecycle-aware fallback: catch pending signals on resume ─
                // When returning from Chapters → HOME (chapters path),
                // the above LaunchedEffects may not re-trigger because HOME's
                // composable was STOPPED (not recomposed). This observer
                // reliably fires on every ON_RESUME, catching any pending
                // completedQuizId / needsRefresh that weren't consumed.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val pendingQuizId = backStackEntry.savedStateHandle
                                .get<String>("completedQuizId") ?: ""
                            if (pendingQuizId.isNotBlank()) {
                                viewModel.markChallengeDone(pendingQuizId)
                                backStackEntry.savedStateHandle["completedQuizId"] = ""
                            }
                            val pendingRefresh = backStackEntry.savedStateHandle
                                .get<Boolean>("needsRefresh") ?: false
                            if (pendingRefresh) {
                                viewModel.refreshAll()
                                backStackEntry.savedStateHandle["needsRefresh"] = false
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChapters = { module ->
                        navController.navigate(
                            NavRoutes.chapters(
                                moduleId = module.id,
                                title = module.title,
                                emoji = module.emoji,
                                color = module.accentColor.removePrefix("#"),
                            ),
                        )
                    },
                    // ── Daily Challenge entry point ──────────────────────
                    onNavigateToDailyChallenge = { challenge ->
                        val color = challenge.moduleColor.ifEmpty { "4F46E5" }
                        val emoji = challenge.moduleEmoji.ifEmpty { "\uD83C\uDFAF" }
                        QuizSessionHolder.selectQuizById(
                            quizId = challenge.quizId,
                            config = QuizConfig.module(
                                moduleColor = color,
                                moduleEmoji = emoji,
                                moduleTitle = challenge.title,
                            ),
                            moduleColor = color,
                            moduleEmoji = emoji,
                            moduleTitle = challenge.title,
                        )
                        navController.navigate(NavRoutes.quizPlay(color))
                    },
                    onNavigateToTournament = {
                        navController.navigate(NavRoutes.TOURNAMENT_LOBBY)
                    },
                    onNavigateToTournamentResult = {
                        navController.navigate(NavRoutes.TOURNAMENT_RESULT)
                    },
                    // ── IQ Test entry point ──────────────────────────────
                    onNavigateToIqTest = { quizId ->
                        val color = "7C3AED" // purple accent for IQ test
                        QuizSessionHolder.selectQuizById(
                            quizId = quizId,
                            config = QuizConfig.iqTest(),
                            moduleColor = color,
                            moduleEmoji = "\uD83E\uDDE0",
                            moduleTitle = "IQ Challenge",
                        )
                        navController.navigate(NavRoutes.quizIntro(color))
                    },
                    // ── Anonymous user sign-in (from tournament banner) ──
                    onGoogleSignIn = {
                        navController.navigate(NavRoutes.AUTH)
                    },
                    onPhoneSignIn = {
                        navController.navigate(NavRoutes.AUTH)
                    },
                )
            }

            composable(NavRoutes.LEADERBOARD) {
                val viewModel = koinInject<LeaderboardViewModel>()
                LeaderboardScreen(viewModel = viewModel, userId = userId)
            }

            composable(NavRoutes.STATS) {
                val viewModel = koinInject<StatsViewModel>()
                StatsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onStartIqTest = {
                        val color = "7C3AED" // purple accent for IQ test
                        QuizSessionHolder.selectQuizById(
                            quizId = AppConstants.IQ_QUIZ_ID,
                            config = QuizConfig.iqTest(),
                            moduleColor = color,
                            moduleEmoji = "\uD83E\uDDE0",
                            moduleTitle = "IQ Challenge",
                        )
                        navController.navigate(NavRoutes.quizIntro(color))
                    },
                )
            }

            composable(NavRoutes.PROFILE) {
                val profileViewModel = koinInject<ProfileViewModel>()
                val authViewModel = koinInject<AuthViewModel>()

                // Observe auth state changes and reload profile when linking completes
                LaunchedEffect(Unit) {
                    authViewModel.authState.collect { authState ->
                        if (authState is UiState.Success) {
                            // Account linking completed, reload profile data
                            profileViewModel.loadProfile(userId)
                        }
                    }
                }

                ProfileScreen(
                    viewModel = profileViewModel,
                    userId = userId,
                    onSignOut = {
                        profileViewModel.signOut()
                        navController.navigate(NavRoutes.LOGIN_JOURNEY) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLinkGoogle = {
                        authViewModel.linkAccount("google")
                    },
                )
            }

            // ── Chapters ────────────────────────────────────────────────
            composable(
                route = NavRoutes.CHAPTERS,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_MODULE_ID) { type = NavType.StringType },
                    navArgument(NavRoutes.ARG_MODULE_TITLE) { type = NavType.StringType },
                    navArgument(NavRoutes.ARG_MODULE_EMOJI) { type = NavType.StringType },
                    navArgument(NavRoutes.ARG_MODULE_COLOR) { type = NavType.StringType },
                ),
            ) { entry ->
                val moduleId = entry.arguments?.getString(NavRoutes.ARG_MODULE_ID).orEmpty()
                val moduleTitle = entry.arguments?.getString(NavRoutes.ARG_MODULE_TITLE).orEmpty()
                val moduleEmoji = entry.arguments?.getString(NavRoutes.ARG_MODULE_EMOJI).orEmpty()
                val moduleColor = entry.arguments?.getString(NavRoutes.ARG_MODULE_COLOR).orEmpty()
                val viewModel = koinInject<ChaptersViewModel>()

                // Refresh chapters when returning from quiz
                val needsChapterRefresh = entry.savedStateHandle
                    .getStateFlow("needsChapterRefresh", false)
                    .collectAsState()
                LaunchedEffect(needsChapterRefresh.value) {
                    if (needsChapterRefresh.value) {
                        viewModel.refreshModule()
                        entry.savedStateHandle["needsChapterRefresh"] = false
                    }
                }

                ChaptersScreen(
                    moduleId = moduleId,
                    moduleTitle = moduleTitle,
                    moduleEmoji = moduleEmoji,
                    moduleColor = moduleColor,
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onQuizSelect = { quiz ->
                        QuizSessionHolder.selectQuiz(
                            quiz = quiz,
                            config = QuizConfig.module(
                                moduleColor = moduleColor,
                                moduleEmoji = moduleEmoji,
                                moduleTitle = moduleTitle,
                            ),
                            moduleColor = moduleColor,
                            moduleEmoji = moduleEmoji,
                            moduleTitle = moduleTitle,
                        )
                        navController.navigate(NavRoutes.quizIntro(moduleColor))
                    },
                    viewModel = viewModel,
                )
            }

            // ── Quiz flow (unified — serves Module, IQ, Tournament) ─────
            composable(
                route = NavRoutes.QUIZ_INTRO,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_MODULE_COLOR) { type = NavType.StringType },
                ),
            ) { entry ->
                val moduleColor = entry.arguments?.getString(NavRoutes.ARG_MODULE_COLOR).orEmpty()
                QuizIntroScreen(
                    moduleColor = moduleColor,
                    onStartQuiz = {
                        navController.navigate(NavRoutes.quizPlay(moduleColor)) {
                            popUpTo(NavRoutes.quizIntro(moduleColor)) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    userId = userId,
                )
            }

            composable(
                route = NavRoutes.QUIZ_PLAY,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_MODULE_COLOR) { type = NavType.StringType },
                ),
            ) { entry ->
                val moduleColor = entry.arguments?.getString(NavRoutes.ARG_MODULE_COLOR).orEmpty()
                val viewModel = koinInject<QuizViewModel>()

                // Auto-start quiz from session holder
                LaunchedEffect(Unit) {
                    viewModel.startFromSession(userId = userId)
                }

                // Route onFinish based on quiz behavior
                val config = QuizSessionHolder.config

                QuizPlayScreen(
                    viewModel = viewModel,
                    moduleColor = moduleColor,
                    onFinish = {
                        // Signal HOME: mark quiz done locally + refresh dashboard
                        val homeEntry = navController.getBackStackEntry(NavRoutes.HOME)
                        homeEntry.savedStateHandle["completedQuizId"] =
                            QuizSessionHolder.quizId
                                ?: QuizSessionHolder.currentQuiz?.id
                                ?: ""
                        homeEntry.savedStateHandle["needsRefresh"] = true

                        when (config?.behavior) {
                            QuizBehavior.TOURNAMENT -> {
                                navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                                    popUpTo(NavRoutes.HOME) { inclusive = false }
                                }
                            }
                            else -> {
                                // If launched from chapters, go back to chapters (with refresh)
                                val chaptersEntry = try {
                                    navController.getBackStackEntry(NavRoutes.CHAPTERS)
                                } catch (_: Exception) { null }

                                if (chaptersEntry != null) {
                                    chaptersEntry.savedStateHandle["needsChapterRefresh"] = true
                                    navController.popBackStack(NavRoutes.CHAPTERS, inclusive = false)
                                } else {
                                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                                }
                            }
                        }
                    },
                    onReview = {
                        navController.navigate(NavRoutes.QUIZ_REVIEW)
                    },
                    onRetry = {
                        // Re-navigate to quiz play to restart
                        navController.navigate(NavRoutes.quizPlay(moduleColor)) {
                            popUpTo(NavRoutes.quizPlay(moduleColor)) { inclusive = true }
                        }
                    },
                    onClose = {
                        // Close without marking quiz as done (error / user quit)
                        when (config?.behavior) {
                            QuizBehavior.TOURNAMENT -> {
                                navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                                    popUpTo(NavRoutes.HOME) { inclusive = false }
                                }
                            }
                            else -> {
                                val chaptersEntry = try {
                                    navController.getBackStackEntry(NavRoutes.CHAPTERS)
                                } catch (_: Exception) { null }

                                if (chaptersEntry != null) {
                                    navController.popBackStack(NavRoutes.CHAPTERS, inclusive = false)
                                } else {
                                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                                }
                            }
                        }
                    },
                    onPause = {
                        // Only reachable when config.allowPause == true (tournament)
                        viewModel.pauseQuiz()
                        navController.navigate(NavRoutes.TOURNAMENT_PAUSE)
                    },
                )
            }

            composable(NavRoutes.QUIZ_REVIEW) {
                QuizReviewScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Tournament flow ─────────────────────────────────────────
            composable(NavRoutes.TOURNAMENT_LOBBY) {
                val viewModel = koinInject<TournamentViewModel>()
                TournamentLobbyScreen(
                    viewModel = viewModel,
                    onStartQuiz = { tournament, tournamentEntry, quiz ->
                        // Store quiz + tournament config in session holder
                        val color = "7C3AED" // tournament purple accent
                        QuizSessionHolder.selectQuiz(
                            quiz = quiz,
                            config = QuizConfig.tournament(
                                tournamentId = tournament.id,
                                entryId = tournamentEntry.id,
                                allowPause = true,
                            ),
                            moduleColor = color,
                            moduleEmoji = "\uD83C\uDFC6",
                            moduleTitle = tournament.title,
                        )
                        // Navigate to unified quiz play screen
                        navController.navigate(NavRoutes.quizPlay(color)) {
                            popUpTo(NavRoutes.TOURNAMENT_LOBBY) { inclusive = true }
                        }
                    },
                    onViewResults = {
                        navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                            popUpTo(NavRoutes.TOURNAMENT_LOBBY) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            // Keep legacy tournament play route for backward compatibility
            composable(NavRoutes.TOURNAMENT_PLAY) {
                val viewModel = koinInject<TournamentViewModel>()
                TournamentPlayScreen(
                    viewModel = viewModel,
                    onPause = { navController.navigate(NavRoutes.TOURNAMENT_PAUSE) },
                    onFinish = {
                        navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                            popUpTo(NavRoutes.TOURNAMENT_PLAY) { inclusive = true }
                        }
                    },
                )
            }

            composable(NavRoutes.TOURNAMENT_PAUSE) {
                val viewModel = koinInject<TournamentViewModel>()
                TournamentPauseScreen(
                    viewModel = viewModel,
                    onResume = { navController.popBackStack() },
                    onQuit = {
                        navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                            popUpTo(NavRoutes.HOME) { inclusive = false }
                        }
                    },
                )
            }

            composable(NavRoutes.TOURNAMENT_RESULT) {
                val viewModel = koinInject<TournamentViewModel>()
                TournamentResultScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onViewLeaderboard = {
                        navController.navigate(NavRoutes.LEADERBOARD) {
                            popUpTo(NavRoutes.HOME) { inclusive = false }
                        }
                    },
                    onExit = {
                        navController.popBackStack(NavRoutes.HOME, inclusive = false)
                    },
                )
            }
        }
    }
}
