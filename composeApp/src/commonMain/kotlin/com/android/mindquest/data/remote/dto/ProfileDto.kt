package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponseDto(
    val user: UserProfileDto,
    val stats: UserStatsDto,
    @SerialName("completed_chapters") val completedChapters: List<CompletedChapterDto> = emptyList(),
    @SerialName("tournament_results") val tournamentResults: List<TournamentResultDto> = emptyList()
)

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: Int,
    @SerialName("grade_id") val gradeId: String,
    @SerialName("grade_label") val gradeLabel: String = "",
    @SerialName("auth_provider") val authProvider: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("country_id") val countryId: String? = null,
    @SerialName("city_id") val cityId: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("city_name") val cityName: String? = null,
    @SerialName("school_name") val schoolName: String? = null
)

@Serializable
data class CompletedChapterDto(
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("chapter_title") val chapterTitle: String,
    @SerialName("module_title") val moduleTitle: String,
    @SerialName("module_emoji") val moduleEmoji: String,
    @SerialName("completed_at") val completedAt: String? = null
)

@Serializable
data class TournamentResultDto(
    @SerialName("tournament_id") val tournamentId: String,
    val title: String,
    val score: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    val rank: Int,
    @SerialName("participant_count") val participantCount: Int = 0,
    @SerialName("certificate_url") val certificateUrl: String? = null,
    val date: String? = null
)
