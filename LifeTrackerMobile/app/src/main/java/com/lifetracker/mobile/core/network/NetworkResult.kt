package com.lifetracker.mobile.core.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val apiError: ApiError) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}

inline fun <T, R> NetworkResult<T>.map(
    transform: (T) -> R,
): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
    is NetworkResult.Exception -> this
}

inline fun <T, R> NetworkResult<T>.fold(
    onSuccess: (T) -> R,
    onError: (code: Int, apiError: ApiError) -> R,
    onException: (throwable: Throwable) -> R
): R = when (this) {
    is NetworkResult.Success -> onSuccess(data)
    is NetworkResult.Error -> onError(code, apiError)
    is NetworkResult.Exception -> onException(throwable)
}

inline fun <T> NetworkResult<T>.onSuccess(
    action: (T) -> Unit,
): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onFailure(
    action: () -> Unit
): NetworkResult<T> {
    if (this !is NetworkResult.Success) action()
    return this
}

fun <T> NetworkResult<T>.dataOrNull(): T? = when (this) {
    is NetworkResult.Success -> data
    else -> null
}

fun <T> NetworkResult<T>.errorOrNull(): ApiError? = when (this) {
    is NetworkResult.Error -> apiError
    else -> null
}


