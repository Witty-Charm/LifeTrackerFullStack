package com.lifetracker.mobile.core.network

import retrofit2.Response

suspend fun <T : Any> safeApiCall(
    apiCall: suspend () -> Response<T>,
): NetworkResult<T> = try {
    val response = apiCall()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            @Suppress("UNCHECKED_CAST")
            NetworkResult.Success(Unit as T)
        }
        } else {
            NetworkResult.Error(response.code(), parseE )
        }
    } catch (e: Exception) {
        NetworkResult.Exception(e)
    }
