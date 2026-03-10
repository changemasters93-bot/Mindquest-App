package com.android.mindquest.domain.model

data class Grade(val id: String, val code: String, val label: String, val sortOrder: Int)
data class Country(val id: String, val name: String, val code: String)
data class City(val id: String, val countryId: String, val name: String)
