package com.lifetracker.mobile.domain.model

sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Failure(val error: GameError) : DomainResult<Nothing>()
}

inline fun <T, R> DomainResult<T>.map(
    transform: (T) -> R,
): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(data))
    is DomainResult.Failure -> this
}

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (GameError) -> R,
): R = when (this) {
    is DomainResult.Success -> onSuccess(data)
    is DomainResult.Failure -> onFailure(error)
}

inline fun <T> DomainResult<T>.onSuccess(
    action: (T) -> Unit,
): DomainResult<T> {
    if (this is DomainResult.Success) action(data)
    return this
}

inline fun <T> DomainResult<T>.onFailure(
    action: (GameError) -> Unit,
): DomainResult<T> {
    if (this is DomainResult.Failure) action(error)
    return this
}

fun <T> DomainResult<T>.dataOrNull(): T? = when (this) {
    is DomainResult.Success -> data
    is DomainResult.Failure -> null
}

fun <T> DomainResult<T>.errorOrNull(): GameError? = when (this) {
    is DomainResult.Success -> null
    is DomainResult.Failure -> error
}