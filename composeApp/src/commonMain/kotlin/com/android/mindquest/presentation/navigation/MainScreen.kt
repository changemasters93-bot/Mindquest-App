package com.android.mindquest.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.presentation.components.MindquestBottomNav
import com.android.mindquest.presentation.home.HomeScreen
import com.android.mindquest.presentation.home.HomeViewModel
import com.android.mindquest.presentation.leaderboard.LeaderboardScreen
import com.android.mindquest.presentation.leaderboard.LeaderboardViewModel
import com.android.mindquest.presentation.profile.ProfileScreen
import com.android.mindquest.presentation.profile.ProfileViewModel
import com.android.mindquest.presentation.stats.StatsScreen
import com.android.mindquest.presentation.stats.StatsViewModel
import com.android.mindquest.core.session.SessionProvider
import com.android.mindquest.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    navController: NavHostController,
) {
    val sessionProvider = koinInject<SessionProvider>()
    // Reactively poll userId until it's available (session may not be ready immediately after Google sign-in)
    var userId by remember { mutableStateOf(sessionProvider.userId) }
    LaunchedEffect(Unit) {
        if (userId.isBlank()) {
            AppLogger.d("MQ_HOME", "MainScreen: userId is EMPTY — polling for session (max 5s)")
            repeat(10) {
                delay(500)
                userId = sessionProvider.userId
                if (userId.isNotBlank()) {
                    AppLogger.d("MQ_HOME", "MainScreen: userId became available → ${userId.take(8)}...")
                    return@LaunchedEffect
                }
            }
            AppLogger.e("MQ_HOME", "MainScreen: userId still EMPTY after 5s polling")
        }
    }
    var currentTab by rememberSaveable { mutableStateOf(NavRoutes.HOME) }

    Scaffold(
        bottomBar = {
            MindquestBottomNav(
                currentRoute = currentTab,
                onNavigate = { route -> currentTab = route },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentTab) {
                NavRoutes.HOME -> {
                    val viewModel = koinInject<HomeViewModel>()
                    val authViewModel = koinInject<AuthViewModel>()
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
                        onNavigateToDailyChallenge = { },
                        onNavigateToTournament = {
                            navController.navigate(NavRoutes.TOURNAMENT_LOBBY)
                        },
                        onGoogleSignIn = {
                            // Anonymous user → link with Google
                            authViewModel.linkAccount("google")
                        },
                    )
                }

                NavRoutes.LEADERBOARD -> {
                    val viewModel = koinInject<LeaderboardViewModel>()
                    LeaderboardScreen(viewModel = viewModel, userId = userId)
                }

                NavRoutes.STATS -> {
                    val viewModel = koinInject<StatsViewModel>()
                    StatsScreen(viewModel = viewModel, userId = userId)
                }

                NavRoutes.PROFILE -> {
                    val viewModel = koinInject<ProfileViewModel>()
                    val authViewModel = koinInject<AuthViewModel>()
                    ProfileScreen(
                        viewModel = viewModel,
                        userId = userId,
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(NavRoutes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onLinkGoogle = {
                            // Triggers Google account linking for anonymous users
                            authViewModel.linkAccount("google")
                        },
                    )
                }
            }
        }
    }
}
