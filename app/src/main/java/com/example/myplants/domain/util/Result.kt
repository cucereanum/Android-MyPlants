package com.example.myplants.domain.util

import kotlinx.coroutines.flow.Flow


sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable, message: String? = null): Result<Nothing> =
            Error(exception, message)
    }
}


suspend inline fun <T> Result<T>.handle(
    crossinline onSuccess: suspend (T) -> Unit,
    crossinline onError: suspend (String?) -> Unit,
    crossinline onLoading: suspend () -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(message)
        is Result.Loading -> onLoading()
    }
}


suspend inline fun <T> Flow<Result<T>>.collectResult(
    crossinline onSuccess: suspend (T) -> Unit,
    crossinline onError: suspend (String?) -> Unit,
    crossinline onLoading: suspend () -> Unit
) {
    collect { result ->
        result.handle(onSuccess, onError, onLoading)
    }
}
