package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.repository.ReferenceDataRepository

class ReferenceDataRepositoryImpl(
    private val apiService: ApiService
) : ReferenceDataRepository {

    override suspend fun getGrades(): Resource<List<Grade>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockGrades())
            } else {
                val response = apiService.getGrades()
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load grades",
                throwable = e
            )
        }
    }

    override suspend fun getCountries(): Resource<List<Country>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockCountries())
            } else {
                val response = apiService.getCountries()
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load countries",
                throwable = e
            )
        }
    }

    override suspend fun getCities(countryId: String): Resource<List<City>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockCities(countryId))
            } else {
                val response = apiService.getCities(countryId)
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load cities",
                throwable = e
            )
        }
    }
}
