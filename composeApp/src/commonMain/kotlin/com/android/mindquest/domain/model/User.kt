package com.android.mindquest.domain.model

data class User(
    val id: String,
    val displayName: String,
    val avatarId: Int,
    val gradeId: String,
    val gradeLabel: String = "",
    val authProvider: String, // google / phone / anonymous / google_and_phone
    val isAnonymous: Boolean,
    val email: String? = null,
    val phone: String? = null,
    val countryId: String? = null,
    val countryName: String? = null,
    val cityId: String? = null,
    val cityName: String? = null,
    val schoolName: String? = null
)
