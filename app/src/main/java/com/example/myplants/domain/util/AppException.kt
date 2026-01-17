package com.example.myplants.domain.util

/**
 * Custom exception types for the app
 */
sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    data class DatabaseException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class NetworkException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class ValidationException(
        override val message: String,
        val field: String? = null
    ) : AppException(message)

    data class NotFoundException(
        override val message: String,
        val resourceId: Int? = null
    ) : AppException(message)

    data class UnknownException(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : AppException(message, cause)
}


fun Throwable.toAppException(): AppException {
    return when (this) {
        is AppException -> this
        is java.io.IOException -> AppException.NetworkException(
            message = this.message ?: "Network error occurred",
            cause = this
        )

        is IllegalArgumentException -> AppException.ValidationException(
            message = this.message ?: "Invalid input"
        )

        else -> AppException.UnknownException(
            message = this.message ?: "An unknown error occurred",
            cause = this
        )
    }
}
