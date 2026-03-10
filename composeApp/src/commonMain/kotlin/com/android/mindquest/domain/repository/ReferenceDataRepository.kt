package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade

interface ReferenceDataRepository {
    suspend fun getGrades(): Resource<List<Grade>>
    suspend fun getCountries(): Resource<List<Country>>
    suspend fun getCities(countryId: String): Resource<List<City>>
}
