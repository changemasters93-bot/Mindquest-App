package com.android.mindquest.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.mindquest.data.mock.MockDataSource
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MindquestNavGraph(
    navController: NavHostController = rememberNavController(),
) {
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
                val viewModel = koinViewModel<AuthViewModel>()
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
                val viewModel = koinViewModel<AuthViewModel>()
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
            composable(NavRoutes.HOME) {
                val viewModel = koinViewModel<HomeViewModel>()
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
                        val quiz = MockDataSource.mockDailyChallengeQuiz(challenge)
                        val color = "4F46E5" // default accent for daily challenges
                        QuizSessionHolder.selectQuiz(
                            quiz = quiz,
                            config = QuizConfig.module(
                                moduleColor = color,
                                moduleEmoji = "\uD83C\uDFAF",
                                moduleTitle = challenge.title,
                            ),
                            moduleColor = color,
                            moduleEmoji = "\uD83C\uDFAF",
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
                    onNavigateToIqTest = {
                        val quiz = MockDataSource.mockIqTestQuiz()
                        val color = "7C3AED" // purple accent for IQ test
                        QuizSessionHolder.selectQuiz(
                            quiz = quiz,
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
                val viewModel = koinViewModel<LeaderboardViewModel>()
                LeaderboardScreen(viewModel = viewModel)
            }

            composable(NavRoutes.STATS) {
                val viewModel = koinViewModel<StatsViewModel>()
                StatsScreen(viewModel = viewModel)
            }

            composable(NavRoutes.PROFILE) {
                val viewModel = koinViewModel<ProfileViewModel>()
                ProfileScreen(
                    viewModel = viewModel,
                    onSignOut = {
                        navController.navigate(NavRoutes.LOGIN_JOURNEY) {
                            popUpTo(0) { inclusive = true }
                        }
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
                val viewModel = koinViewModel<ChaptersViewModel>()

                ChaptersScreen(
                    moduleId = moduleId,
                    moduleTitle = moduleTitle,
                    moduleEmoji = moduleEmoji,
                    moduleColor = moduleColor,
                    userId = "",
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
                )
            }

            composable(
                route = NavRoutes.QUIZ_PLAY,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_MODULE_COLOR) { type = NavType.StringType },
                ),
            ) { entry ->
                val moduleColor = entry.arguments?.getString(NavRoutes.ARG_MODULE_COLOR).orEmpty()
                val viewModel = koinViewModel<QuizViewModel>()

                // Auto-start quiz from session holder
                LaunchedEffect(Unit) {
                    viewModel.startFromSession(userId = "")
                }

                // Route onFinish based on quiz behavior
                val config = QuizSessionHolder.config

                QuizPlayScreen(
                    viewModel = viewModel,
                    moduleColor = moduleColor,
                    onFinish = {
                        when (config?.behavior) {
                            QuizBehavior.TOURNAMENT -> {
                                navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                                    popUpTo(NavRoutes.HOME) { inclusive = false }
                                }
                            }
                            else -> {
                                navController.popBackStack(NavRoutes.HOME, inclusive = false)
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
                        when (config?.behavior) {
                            QuizBehavior.TOURNAMENT -> {
                                // For tournament, closing submits and goes to result
                                navController.navigate(NavRoutes.TOURNAMENT_RESULT) {
                                    popUpTo(NavRoutes.HOME) { inclusive = false }
                                }
                            }
                            else -> {
                                navController.popBackStack(NavRoutes.HOME, inclusive = false)
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
                val viewModel = koinViewModel<TournamentViewModel>()
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
                val viewModel = koinViewModel<TournamentViewModel>()
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
                val viewModel = koinViewModel<TournamentViewModel>()
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
                val viewModel = koinViewModel<TournamentViewModel>()
                TournamentResultScreen(
                    viewModel = viewModel,
                    userId = "current_user",
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
