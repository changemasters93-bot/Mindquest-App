package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.repository.ReferenceDataRepository

class GetReferenceDataUseCase(
    private val repository: ReferenceDataRepository
) {
    suspend fun getGrades(): Resource<List<Grade>> {
        return repository.getGrades()
    }

    suspend fun getCountries(): Resource<List<Country>> {
        return repository.getCountries()
    }

    suspend fun getCities(countryId: String): Resource<List<City>> {
        return repository.getCities(countryId)
    }

    suspend operator fun invoke(): Resource<List<Grade>> {
        return repository.getGrades()
    }
}
