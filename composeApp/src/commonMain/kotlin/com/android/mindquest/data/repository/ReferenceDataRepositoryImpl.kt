package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.repository.ReferenceDataRepository
import kotlinx.datetime.Clock

/**
 * Reference data repository with in-memory caching.
 *
 * Grades and countries rarely change, so we cache them for [CACHE_TTL_MS]
 * to avoid redundant API calls on every screen visit.
 */
class ReferenceDataRepositoryImpl(
    private val apiService: ApiService
) : ReferenceDataRepository {

    // ── In-memory cache ──────────────────────────────────────────────
    private var cachedGrades: List<Grade>? = null
    private var gradesCachedAt: Long = 0L

    private var cachedCountries: List<Country>? = null
    private var countriesCachedAt: Long = 0L

    private val citiesCache = mutableMapOf<String, Pair<Long, List<City>>>()

    override suspend fun getGrades(): Resource<List<Grade>> {
        // Return cached if still fresh
        cachedGrades?.let { cached ->
            if (Clock.System.now().toEpochMilliseconds() - gradesCachedAt < CACHE_TTL_MS) {
                return Resource.Success(cached)
            }
        }
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockGrades())
            } else {
                val response = withRetry { apiService.getGrades() }
                val grades = response.map { it.toDomain() }
                cachedGrades = grades
                gradesCachedAt = Clock.System.now().toEpochMilliseconds()
                Resource.Success(grades)
            }
        } catch (e: Exception) {
            AppLogger.e("RefDataRepo", "load grades failed", e)
            // Return stale cache on error if available
            cachedGrades?.let { return Resource.Success(it) }
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun getCountries(): Resource<List<Country>> {
        cachedCountries?.let { cached ->
            if (Clock.System.now().toEpochMilliseconds() - countriesCachedAt < CACHE_TTL_MS) {
                return Resource.Success(cached)
            }
        }
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockCountries())
            } else {
                val response = withRetry { apiService.getCountries() }
                val countries = response.map { it.toDomain() }
                cachedCountries = countries
                countriesCachedAt = Clock.System.now().toEpochMilliseconds()
                Resource.Success(countries)
            }
        } catch (e: Exception) {
            AppLogger.e("RefDataRepo", "load countries failed", e)
            cachedCountries?.let { return Resource.Success(it) }
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun getCities(countryId: String): Resource<List<City>> {
        citiesCache[countryId]?.let { (cachedAt, cached) ->
            if (Clock.System.now().toEpochMilliseconds() - cachedAt < CACHE_TTL_MS) {
                return Resource.Success(cached)
            }
        }
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockCities(countryId))
            } else {
                val response = withRetry { apiService.getCities(countryId) }
                val cities = response.map { it.toDomain() }
                citiesCache[countryId] = Clock.System.now().toEpochMilliseconds() to cities
                Resource.Success(cities)
            }
        } catch (e: Exception) {
            AppLogger.e("RefDataRepo", "load cities failed", e)
            citiesCache[countryId]?.let { return Resource.Success(it.second) }
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    companion object {
        /** Cache TTL: 10 minutes. Grades/countries/cities rarely change. */
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
