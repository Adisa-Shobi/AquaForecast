package com.example.aquaforecast.domain.repository

sealed interface Result<out T> {
    /**
     * Represents a successful operation
     * @property data The successfully retrieved or processed data
     */
    data class Success<T>(val data: T) : Result<T>

    /**
     * Represents a failed operation
     * @property message A human-readable error message describing what went wrong
     */
    data class Error(val message: String) : Result<Nothing>

    /**
     * Check if this result represents a successful operation
     * @return true if this is a Success, false if Error
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if this result represents a failed operation
     * @return true if this is an Error, false if Success
     */
    fun isError(): Boolean = this is Error
}

/**
 * Extension function to wrap a value in a Success result
 * @return Result.Success containing this value
 */
fun <T> T.asSuccess(): Result<T> = Result.Success(this)

/**
 * Extension function to wrap a string in an Error result
 * @return Result.Error with this string as the error message
 */
fun String.asError(): Result<Nothing> = Result.Error(this)

/**
 * Execute an action if the result is successful
 * @param action The action to perform with the success data
 * @return The original Result for chaining
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Execute an action if the result is an error
 * @param action The action to perform with the error message
 * @return The original Result for chaining
 */
inline fun <T> Result<T>.onError(action: (String) -> Unit): Result<T> {
    if (this is Result.Error) action(message)
    return this
}

/**
 * Get the data if successful, otherwise return null
 * @return The success data or null if this is an error
 */
fun <T> Result<T>.getOrNull(): T? =
    (this as? Result.Success)?.data

/**
 * Get the data if successful, otherwise return the provided default value
 * @param default The value to return if this is an error
 * @return The success data or the default value
 */
fun <T> Result<T>.getOrDefault(default: T): T =
    (this as? Result.Success)?.data ?: default

/**
 * Get the data if successful, otherwise throw an exception
 * @return The success data
 * @throws Exception if this is an error
 */
fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw Exception(message)
}