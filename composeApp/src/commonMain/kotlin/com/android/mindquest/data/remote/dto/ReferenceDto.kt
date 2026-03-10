package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradeDto(
    val id: String,
    val code: String,
    val label: String,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CountryDto(
    val id: String,
    val name: String,
    val code: String
)

@Serializable
data class CityDto(
    val id: String,
    @SerialName("country_id") val countryId: String,
    val name: String
)
