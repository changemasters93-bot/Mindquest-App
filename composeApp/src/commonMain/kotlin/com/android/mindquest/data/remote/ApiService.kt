package com.android.mindquest.data.remote

import com.android.mindquest.data.remote.dto.CityDto
import com.android.mindquest.data.remote.dto.CountryDto
import com.android.mindquest.data.remote.dto.DailyChallengeDto
import com.android.mindquest.data.remote.dto.DashboardResponseDto
import com.android.mindquest.data.remote.dto.GradeDto
import com.android.mindquest.data.remote.dto.LeaderboardResponseDto
import com.android.mindquest.data.remote.dto.ModuleFullResponseDto
import com.android.mindquest.data.remote.dto.ProfileResponseDto
import com.android.mindquest.data.remote.dto.QuizDto
import com.android.mindquest.data.remote.dto.QuizResultDto
import com.android.mindquest.data.remote.dto.StatsResponseDto
import com.android.mindquest.data.remote.dto.TournamentEntryDto
import com.android.mindquest.data.remote.dto.TournamentInfoDto
import com.android.mindquest.data.remote.dto.TournamentStartResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Wraps all Supabase RPC calls and direct table reads for the Mindquest API.
 *
 * Each method maps to a single Supabase RPC function or table query and
 * deserializes the result into the corresponding DTO type.
 */
class ApiService(private val client: SupabaseClient) {

    // ── Dashboard ────────────────────────────────────────────────────────

    suspend fun getUserDashboard(userId: String): DashboardResponseDto {
        return client.postgrest.rpc(
            function = "get_user_dashboard",
            parameters = buildJsonObject { put("p_user_id", userId) }
        ).decodeAs()
    }

    suspend fun getDailyChallenges(userId: String): List<DailyChallengeDto> {
        return client.postgrest.rpc(
            function = "get_daily_challenges",
            parameters = buildJsonObject { put("p_user_id", userId) }
        ).decodeList()
    }

    // ── Modules & Chapters ───────────────────────────────────────────────

    suspend fun getModuleFull(moduleId: String, userId: String): ModuleFullResponseDto {
        return client.postgrest.rpc(
            function = "get_module_full",
            parameters = buildJsonObject {
                put("p_module_id", moduleId)
                put("p_user_id", userId)
            }
        ).decodeAs()
    }

    suspend fun getChapterQuizzes(chapterId: String, userId: String): List<QuizDto> {
        return client.postgrest.rpc(
            function = "get_chapter_quizzes",
            parameters = buildJsonObject {
                put("p_chapter_id", chapterId)
                put("p_user_id", userId)
            }
        ).decodeList()
    }

    // ── Quiz ─────────────────────────────────────────────────────────────

    suspend fun submitQuizAttempt(payload: JsonObject): QuizResultDto {
        return client.postgrest.rpc(
            function = "submit_quiz_attempt",
            parameters = buildJsonObject { put("payload", payload) }
        ).decodeAs()
    }

    // ── Tournament ───────────────────────────────────────────────────────

    suspend fun getActiveTournament(userId: String, gradeId: String): TournamentInfoDto? {
        return try {
            client.postgrest.rpc(
                function = "get_active_tournament",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_grade_id", gradeId)
                }
            ).decodeAsOrNull()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun startTournament(userId: String, tournamentId: String): TournamentStartResponseDto {
        return client.postgrest.rpc(
            function = "start_tournament",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_tournament_id", tournamentId)
            }
        ).decodeAs()
    }

    suspend fun pauseTournament(entryId: String): TournamentEntryDto {
        return client.postgrest.rpc(
            function = "pause_tournament",
            parameters = buildJsonObject { put("p_entry_id", entryId) }
        ).decodeAs()
    }

    suspend fun resumeTournament(entryId: String): TournamentEntryDto {
        return client.postgrest.rpc(
            function = "resume_tournament",
            parameters = buildJsonObject { put("p_entry_id", entryId) }
        ).decodeAs()
    }

    suspend fun submitTournament(
        entryId: String,
        answers: JsonObject,
        timeTaken: Int
    ): QuizResultDto {
        return client.postgrest.rpc(
            function = "submit_tournament",
            parameters = buildJsonObject {
                put("p_entry_id", entryId)
                put("p_answers", answers)
                put("p_time_taken", timeTaken)
            }
        ).decodeAs()
    }

    /** Submit a single tournament answer (per-question non-blocking mode) */
    suspend fun submitTournamentAnswer(
        entryId: String,
        answer: JsonObject,
    ) {
        client.postgrest.rpc(
            function = "submit_tournament_answer",
            parameters = buildJsonObject {
                put("p_entry_id", JsonPrimitive(entryId))
                put("p_answer", answer)
            },
        )
    }

    // ── Leaderboard ──────────────────────────────────────────────────────

    suspend fun getLeaderboard(
        userId: String,
        filter: String,
        filterId: String?,
        limit: Int,
        offset: Int
    ): LeaderboardResponseDto {
        return client.postgrest.rpc(
            function = "get_leaderboard",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_filter", filter)
                put("p_limit", limit)
                put("p_offset", offset)
                filterId?.let { put("p_filter_id", it) }
            }
        ).decodeAs()
    }

    // ── Stats ────────────────────────────────────────────────────────────

    suspend fun getUserStats(userId: String, period: String): StatsResponseDto {
        return client.postgrest.rpc(
            function = "get_user_stats",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_period", period)
            }
        ).decodeAs()
    }

    // ── Profile ──────────────────────────────────────────────────────────

    suspend fun getProfile(userId: String): ProfileResponseDto {
        return client.postgrest.rpc(
            function = "get_profile",
            parameters = buildJsonObject { put("p_user_id", userId) }
        ).decodeAs()
    }

    suspend fun updateProfile(userId: String, fields: JsonObject) {
        client.postgrest.rpc(
            function = "update_profile",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_fields", fields)
            }
        )
    }

    // ── Reference data (direct table reads) ──────────────────────────────

    suspend fun getGrades(): List<GradeDto> {
        return client.postgrest.from("grades").select().decodeList()
    }

    suspend fun getCountries(): List<CountryDto> {
        return client.postgrest.from("countries").select {
            filter { eq("is_active", true) }
        }.decodeList()
    }

    suspend fun getCities(countryId: String): List<CityDto> {
        return client.postgrest.from("cities").select {
            filter { eq("country_id", countryId) }
        }.decodeList()
    }
}
