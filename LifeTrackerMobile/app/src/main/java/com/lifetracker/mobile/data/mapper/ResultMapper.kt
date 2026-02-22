package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError

fun <T> NetworkResult<T>.toDomainResult(): DomainResult<T> = when (this) {
    is NetworkResult.Success -> DomainResult.Success(data)
    is NetworkResult.Error -> DomainResult.Failure(apiError.toDomain())
    is NetworkResult.Exception -> DomainResult.Failure(
        when (throwable) {
            is java.io.IOException -> GameError.Network
            else -> GameError.Unknown(throwable.message ?: "Unexpected error")
        }
    )
}