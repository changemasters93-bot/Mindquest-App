package com.android.mindquest.domain.model

data class DashboardData(
    val user: User,
    val stats: UserStats,
    val modules: List<Module>,
    val activeTournament: Tournament? = null,
    val activeTournamentEntry: TournamentEntry? = null,
    val lastIqTestDateMillis: Long? = null,
)
