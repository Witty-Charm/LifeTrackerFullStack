package com.lifetracker.mobile.core.network

import kotlinx.serialization.json.Json
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class SafeApiCaller(private val json: Json) {
    suspend fun <T : Any> safeApiCall(
        apiCall: suspend () -> Response<T>,
    ): NetworkResult<T> = try {
        val response = apiCall()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            val body = response.body() ?: Unit as T
            NetworkResult.Success(body)
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
        } catch (_: Exception) {
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
