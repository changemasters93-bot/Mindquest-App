package com.android.mindquest.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.android.mindquest.presentation.components.MindquestBottomNav
import com.android.mindquest.presentation.home.HomeScreen
import com.android.mindquest.presentation.home.HomeViewModel
import com.android.mindquest.presentation.leaderboard.LeaderboardScreen
import com.android.mindquest.presentation.leaderboard.LeaderboardViewModel
import com.android.mindquest.presentation.profile.ProfileScreen
import com.android.mindquest.presentation.profile.ProfileViewModel
import com.android.mindquest.presentation.stats.StatsScreen
import com.android.mindquest.presentation.stats.StatsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
) {
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
                        onNavigateToDailyChallenge = { },
                        onNavigateToTournament = {
                            navController.navigate(NavRoutes.TOURNAMENT_LOBBY)
                        },
                    )
                }

                NavRoutes.LEADERBOARD -> {
                    val viewModel = koinViewModel<LeaderboardViewModel>()
                    LeaderboardScreen(viewModel = viewModel)
                }

                NavRoutes.STATS -> {
                    val viewModel = koinViewModel<StatsViewModel>()
                    StatsScreen(viewModel = viewModel)
                }

                NavRoutes.PROFILE -> {
                    val viewModel = koinViewModel<ProfileViewModel>()
                    ProfileScreen(
                        viewModel = viewModel,
                        onSignOut = {
                            navController.navigate(NavRoutes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
