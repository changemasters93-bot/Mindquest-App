package com.android.mindquest.data.remote

import com.android.mindquest.core.util.AppLogger
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
import com.android.mindquest.data.remote.dto.TournamentLeaderboardResponseDto
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
 *
 * All API calls are logged with URL, request parameters, and response details.
 */
class ApiService(private val client: SupabaseClient) {

    /**
     * Safely masks sensitive data in parameters for logging.
     * Masks: email, password, phone, token, token_hash, etc.
     */
    private fun maskSensitiveData(params: JsonObject): JsonObject {
        val masked = buildJsonObject {
            params.forEach { (key, value) ->
                when {
                    key.lowercase().contains("email") -> put(key, JsonPrimitive("***@***.***"))
                    key.lowercase().contains("password") -> put(key, JsonPrimitive("***"))
                    key.lowercase().contains("phone") && value.toString().length > 3 -> put(key, JsonPrimitive("***${value.toString().takeLast(2)}"))
                    key.lowercase().contains("token") -> put(key, JsonPrimitive("***"))
                    key.lowercase().contains("secret") -> put(key, JsonPrimitive("***"))
                    key.lowercase().contains("hash") -> put(key, JsonPrimitive("***"))
                    else -> put(key, value)
                }
            }
        }
        return masked
    }

    // ── Dashboard ────────────────────────────────────────────────────────

    suspend fun getUserDashboard(userId: String): DashboardResponseDto {
        val params = buildJsonObject { put("p_user_id", userId) }
        AppLogger.d("MQ_API", "🔵 RPC: get_user_dashboard | Params: userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_user_dashboard",
                parameters = params
            ).decodeAs<DashboardResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_user_dashboard SUCCESS | Response: user(id=${response.user.id}), modules=${response.modules.size}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_user_dashboard FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getDailyChallenges(userId: String): List<DailyChallengeDto> {
        val params = buildJsonObject { put("p_user_id", userId) }
        AppLogger.d("MQ_API", "🔵 RPC: get_daily_challenges | Params: userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_daily_challenges",
                parameters = params
            ).decodeList<DailyChallengeDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_daily_challenges SUCCESS | Response: ${response.size} challenges")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_daily_challenges FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Modules & Chapters ───────────────────────────────────────────────

    suspend fun getModuleFull(moduleId: String, userId: String): ModuleFullResponseDto {
        val params = buildJsonObject {
            put("p_module_id", moduleId)
            put("p_user_id", userId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_module_full | Params: moduleId=$moduleId, userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_module_full",
                parameters = params
            ).decodeAs<ModuleFullResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_module_full SUCCESS | Response: ${response.chapters.size} chapters")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_module_full FAILED for moduleId=$moduleId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getChapterQuizzes(chapterId: String, userId: String): List<QuizDto> {
        val params = buildJsonObject {
            put("p_chapter_id", chapterId)
            put("p_user_id", userId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_chapter_quizzes | Params: chapterId=$chapterId, userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_chapter_quizzes",
                parameters = params
            ).decodeList<QuizDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_chapter_quizzes SUCCESS | Response: ${response.size} quizzes")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_chapter_quizzes FAILED for chapterId=$chapterId | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Quiz ─────────────────────────────────────────────────────────────

    /** Load a single quiz with its questions by quiz ID (for IQ test, daily challenge). */
    suspend fun getQuizWithQuestions(quizId: String, userId: String): QuizDto {
        val params = buildJsonObject {
            put("p_quiz_id", quizId)
            put("p_user_id", userId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_quiz_with_questions | Params: quizId=$quizId, userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_quiz_with_questions",
                parameters = params
            ).decodeAs<QuizDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_quiz_with_questions SUCCESS | Response: ${response.questions.size} questions")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_quiz_with_questions FAILED for quizId=$quizId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun submitQuizAttempt(payload: JsonObject): QuizResultDto {
        AppLogger.d("MQ_API", "🔵 RPC: submit_quiz_attempt | Params: payload keys=${payload.keys.joinToString(",")}")
        return try {
            val response = client.postgrest.rpc(
                function = "submit_quiz_attempt",
                parameters = buildJsonObject { put("p_payload", payload) }
            ).decodeAs<QuizResultDto>()
            AppLogger.d("MQ_API", "✅ RPC: submit_quiz_attempt SUCCESS | Response: score=${response.score}, xpEarned=${response.xpEarned}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: submit_quiz_attempt FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Tournament ───────────────────────────────────────────────────────

    /** Read the user's tournament entry directly from the table (for result screen). */
    suspend fun getTournamentEntry(userId: String, tournamentId: String): TournamentEntryDto? {
        AppLogger.d("MQ_API", "🔵 TABLE: tournament_entries | Filter: userId=$userId, tournamentId=$tournamentId")
        return try {
            val response = client.postgrest.from("tournament_entries")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("tournament_id", tournamentId)
                    }
                }
                .decodeSingleOrNull<TournamentEntryDto>()
            if (response != null) {
                AppLogger.d("MQ_API", "✅ TABLE: tournament_entries SUCCESS | Response: entryId=${response.id}")
            } else {
                AppLogger.d("MQ_API", "✅ TABLE: tournament_entries SUCCESS | Response: NULL (not found)")
            }
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ TABLE: tournament_entries FAILED | Error: ${e.message}", e)
            null
        }
    }

    suspend fun getActiveTournament(userId: String, gradeId: String): TournamentInfoDto? {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_grade_id", gradeId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_active_tournament | Params: userId=$userId, gradeId=$gradeId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_active_tournament",
                parameters = params
            ).decodeAsOrNull<TournamentInfoDto>()
            if (response != null) {
                AppLogger.d("MQ_API", "✅ RPC: get_active_tournament SUCCESS | Response: tournamentId=${response.id}")
            } else {
                AppLogger.d("MQ_API", "✅ RPC: get_active_tournament SUCCESS | Response: NULL (no active)")
            }
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_active_tournament FAILED | Error: ${e.message}", e)
            null
        }
    }

    suspend fun startTournament(userId: String, tournamentId: String): TournamentStartResponseDto {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_tournament_id", tournamentId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: start_tournament | Params: userId=$userId, tournamentId=$tournamentId")
        return try {
            val response = client.postgrest.rpc(
                function = "start_tournament",
                parameters = params
            ).decodeAs<TournamentStartResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: start_tournament SUCCESS | Response: entryId=${response.entryId}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: start_tournament FAILED for tournamentId=$tournamentId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun pauseTournament(entryId: String): TournamentEntryDto {
        val params = buildJsonObject { put("p_entry_id", entryId) }
        AppLogger.d("MQ_API", "🔵 RPC: pause_tournament | Params: entryId=$entryId")
        return try {
            val response = client.postgrest.rpc(
                function = "pause_tournament",
                parameters = params
            ).decodeAs<TournamentEntryDto>()
            AppLogger.d("MQ_API", "✅ RPC: pause_tournament SUCCESS | Response: entryId=${response.id}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: pause_tournament FAILED for entryId=$entryId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun resumeTournament(entryId: String): TournamentEntryDto {
        val params = buildJsonObject { put("p_entry_id", entryId) }
        AppLogger.d("MQ_API", "🔵 RPC: resume_tournament | Params: entryId=$entryId")
        return try {
            val response = client.postgrest.rpc(
                function = "resume_tournament",
                parameters = params
            ).decodeAs<TournamentEntryDto>()
            AppLogger.d("MQ_API", "✅ RPC: resume_tournament SUCCESS | Response: entryId=${response.id}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: resume_tournament FAILED for entryId=$entryId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun submitTournament(
        entryId: String,
        answers: JsonObject,
        timeTaken: Int
    ): QuizResultDto {
        val params = buildJsonObject {
            put("p_entry_id", entryId)
            put("p_answers", answers)
            put("p_time_taken", timeTaken)
        }
        AppLogger.d("MQ_API", "🔵 RPC: submit_tournament | Params: entryId=$entryId, timeTaken=$timeTaken, answers=${answers.size}")
        return try {
            val response = client.postgrest.rpc(
                function = "submit_tournament",
                parameters = params
            ).decodeAs<QuizResultDto>()
            AppLogger.d("MQ_API", "✅ RPC: submit_tournament SUCCESS | Response: score=${response.score}, status=${response.status}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: submit_tournament FAILED for entryId=$entryId | Error: ${e.message}", e)
            throw e
        }
    }

    /** Submit a single tournament answer (per-question non-blocking mode) */
    suspend fun submitTournamentAnswer(
        entryId: String,
        answer: JsonObject,
    ) {
        AppLogger.d("MQ_API", "🔵 RPC: submit_tournament_answer | Params: entryId=$entryId")
        return try {
            client.postgrest.rpc(
                function = "submit_tournament_answer",
                parameters = buildJsonObject {
                    put("p_entry_id", JsonPrimitive(entryId))
                    put("p_answer", answer)
                },
            )
            AppLogger.d("MQ_API", "✅ RPC: submit_tournament_answer SUCCESS")
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: submit_tournament_answer FAILED for entryId=$entryId | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Tournament Leaderboard ────────────────────────────────────────────

    suspend fun getTournamentLeaderboard(
        tournamentId: String,
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): TournamentLeaderboardResponseDto {
        val params = buildJsonObject {
            put("p_tournament_id", tournamentId)
            put("p_user_id", userId)
            put("p_limit", limit)
            put("p_offset", offset)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_tournament_leaderboard | Params: tournamentId=$tournamentId, userId=$userId, limit=$limit, offset=$offset")
        return try {
            val response = client.postgrest.rpc(
                function = "get_tournament_leaderboard",
                parameters = params
            ).decodeAs<TournamentLeaderboardResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_tournament_leaderboard SUCCESS | Response: ${response.rankedUsers.size} entries")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_tournament_leaderboard FAILED for tournamentId=$tournamentId | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Leaderboard ──────────────────────────────────────────────────────

    suspend fun getLeaderboard(
        userId: String,
        filter: String,
        filterId: String?,
        limit: Int,
        offset: Int
    ): LeaderboardResponseDto {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_filter", filter)
            put("p_limit", limit)
            put("p_offset", offset)
            filterId?.let { put("p_filter_id", it) }
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_leaderboard | Params: userId=$userId, filter=$filter, filterId=$filterId, limit=$limit, offset=$offset")
        return try {
            val response = client.postgrest.rpc(
                function = "get_leaderboard",
                parameters = params
            ).decodeAs<LeaderboardResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_leaderboard SUCCESS | Response: ${response.rankedUsers.size} users, userRank=${response.userRank.rankGlobal}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_leaderboard FAILED for filter=$filter | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Stats ────────────────────────────────────────────────────────────

    suspend fun getUserStats(userId: String, period: String): StatsResponseDto {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_period", period)
        }
        AppLogger.d("MQ_API", "🔵 RPC: get_user_stats | Params: userId=$userId, period=$period")
        return try {
            val response = client.postgrest.rpc(
                function = "get_user_stats",
                parameters = params
            ).decodeAs<StatsResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_user_stats SUCCESS | Response: totalXp=${response.stats.totalXp}, accuracy=${response.accuracyPct}%")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_user_stats FAILED for userId=$userId | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Profile ──────────────────────────────────────────────────────────

    suspend fun getProfile(userId: String): ProfileResponseDto {
        val params = buildJsonObject { put("p_user_id", userId) }
        AppLogger.d("MQ_API", "🔵 RPC: get_profile | Params: userId=$userId")
        return try {
            val response = client.postgrest.rpc(
                function = "get_profile",
                parameters = params
            ).decodeAs<ProfileResponseDto>()
            AppLogger.d("MQ_API", "✅ RPC: get_profile SUCCESS | Response: user=${response.user.displayName}, email=${response.user.email?.let { "***" } ?: "NULL"}, phone=${response.user.phone?.let { "***" } ?: "NULL"}, authProvider=${response.user.authProvider}")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: get_profile FAILED for userId=$userId | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateProfile(userId: String, fields: JsonObject) {
        AppLogger.d("MQ_API", "🔵 RPC: update_profile | Params: userId=$userId, fields=${fields.keys.joinToString(",")}")
        return try {
            client.postgrest.rpc(
                function = "update_profile",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_fields", fields)
                }
            )
            AppLogger.d("MQ_API", "✅ RPC: update_profile SUCCESS | Updated fields: ${fields.keys.joinToString(",")}")
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: update_profile FAILED for userId=$userId | Error: ${e.message}", e)
            throw e
        }
    }

    // ── User row (direct table write — guide §2.2) ────────────────────────

    /**
     * Creates or updates the public.users row after a successful auth.
     * This is the ONLY direct table write the app performs — every other
     * mutation goes through an RPC.
     */
    suspend fun upsertUser(data: JsonObject) {
        AppLogger.d("MQ_API", "🔵 TABLE UPSERT: users | Columns: ${data.keys.joinToString(",")}")
        return try {
            client.postgrest.from("users").upsert(data)
            AppLogger.d("MQ_API", "✅ TABLE UPSERT: users SUCCESS | userId=${data["id"]}")
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ TABLE UPSERT: users FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Account linking & duplicate detection ──────────────────────────────

    /** Find existing user by email. Returns null if not found. */
    suspend fun findUserByEmail(email: String): JsonObject? {
        AppLogger.d("MQ_API", "🔵 RPC: find_user_by_email | Params: email=***@***.***")
        return try {
            val response = client.postgrest.rpc(
                function = "find_user_by_email",
                parameters = buildJsonObject { put("p_email", email) }
            ).decodeAsOrNull<JsonObject>()
            if (response != null) {
                AppLogger.d("MQ_API", "✅ RPC: find_user_by_email SUCCESS | Response: FOUND (id=${response["id"]})")
            } else {
                AppLogger.d("MQ_API", "✅ RPC: find_user_by_email SUCCESS | Response: NOT FOUND")
            }
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: find_user_by_email FAILED | Error: ${e.message}", e)
            null
        }
    }

    /** Find existing user by phone. Returns null if not found. */
    suspend fun findUserByPhone(phone: String): JsonObject? {
        AppLogger.d("MQ_API", "🔵 RPC: find_user_by_phone | Params: phone=***")
        return try {
            val response = client.postgrest.rpc(
                function = "find_user_by_phone",
                parameters = buildJsonObject { put("p_phone", phone) }
            ).decodeAsOrNull<JsonObject>()
            if (response != null) {
                AppLogger.d("MQ_API", "✅ RPC: find_user_by_phone SUCCESS | Response: FOUND (id=${response["id"]})")
            } else {
                AppLogger.d("MQ_API", "✅ RPC: find_user_by_phone SUCCESS | Response: NOT FOUND")
            }
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: find_user_by_phone FAILED | Error: ${e.message}", e)
            null
        }
    }

    /** Merge all data from one user to another (quiz attempts, XP, etc). */
    suspend fun mergeUsers(fromId: String, toId: String) {
        val params = buildJsonObject {
            put("p_from_id", fromId)
            put("p_to_id", toId)
        }
        AppLogger.d("MQ_API", "🔵 RPC: merge_users | Params: fromId=$fromId → toId=$toId")
        return try {
            client.postgrest.rpc(
                function = "merge_users",
                parameters = params
            )
            AppLogger.d("MQ_API", "✅ RPC: merge_users SUCCESS | Merged $fromId → $toId")
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: merge_users FAILED from=$fromId to=$toId | Error: ${e.message}", e)
            throw e
        }
    }

    /** Merge anonymous user into Google user (clone profile + transfer all child data). */
    suspend fun mergeAnonymousToGoogle(anonId: String, googleId: String, email: String) {
        AppLogger.d("MQ_API", "🔵 RPC: merge_anonymous_to_google | anon=$anonId → google=$googleId")
        return try {
            client.postgrest.rpc(
                function = "merge_anonymous_to_google",
                parameters = buildJsonObject {
                    put("p_anon_id", anonId)
                    put("p_google_id", googleId)
                    put("p_email", email)
                }
            )
            AppLogger.d("MQ_API", "✅ RPC: merge_anonymous_to_google SUCCESS")
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ RPC: merge_anonymous_to_google FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    // ── Reference data (direct table reads) ──────────────────────────────

    suspend fun getGrades(): List<GradeDto> {
        AppLogger.d("MQ_API", "🔵 TABLE: grades")
        return try {
            val response = client.postgrest.from("grades").select().decodeList<GradeDto>()
            AppLogger.d("MQ_API", "✅ TABLE: grades SUCCESS | Response: ${response.size} grades")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ TABLE: grades FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getCountries(): List<CountryDto> {
        AppLogger.d("MQ_API", "🔵 TABLE: countries | Filter: is_active=true")
        return try {
            val response = client.postgrest.from("countries").select {
                filter { eq("is_active", true) }
            }.decodeList<CountryDto>()
            AppLogger.d("MQ_API", "✅ TABLE: countries SUCCESS | Response: ${response.size} countries")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ TABLE: countries FAILED | Error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getCities(countryId: String): List<CityDto> {
        AppLogger.d("MQ_API", "🔵 TABLE: cities | Filter: countryId=$countryId")
        return try {
            val response = client.postgrest.from("cities").select {
                filter { eq("country_id", countryId) }
            }.decodeList<CityDto>()
            AppLogger.d("MQ_API", "✅ TABLE: cities SUCCESS | Response: ${response.size} cities for $countryId")
            response
        } catch (e: Exception) {
            AppLogger.e("MQ_API", "❌ TABLE: cities FAILED for countryId=$countryId | Error: ${e.message}", e)
            throw e
        }
    }
}
