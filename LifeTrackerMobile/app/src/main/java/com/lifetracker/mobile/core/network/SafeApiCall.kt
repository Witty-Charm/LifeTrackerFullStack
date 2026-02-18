package com.lifetracker.mobile.core.network

import kotlinx.serialization.json.Json
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class SafeApiCaller(private val json: Json) {
    suspend fun <T : Any> safeApiCall(
        apiCall: suspend () -> Response<T>,
    ): NetworkResult<T> = try {
        val response = apiCall()
        val code = response.code()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error(code, ApiError(message = "Empty response for non-Unit type"))
            }
        } else {
            NetworkResult.Error(code, parseErrorBody(response))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Exception(e)
    }

    suspend fun safeApiCallUnit(
        apiCall: suspend () -> Response<Unit>,
    ): NetworkResult<Unit> = try {
        val response = apiCall()
        if (response.isSuccessful) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(response.code(), parseErrorBody(response))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Exception(e)
    }

    private fun parseErrorBody(response: Response<*>): ApiError {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        if (raw.isNullOrBlank()) return ApiError(message = "HTTP ${response.code()}")
        return try {
            json.decodeFromString<ApiError>(raw)
        } catch (_: Exception) {
            ApiError(message = raw.trim().removeSurrounding("\""))
        }
    }
}