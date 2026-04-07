package com.andrea.showmateapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

suspend fun <T> safeApiCall(
    retries: Int = 0,
    initialDelayMs: Long = 500L,
    apiCall: suspend () -> T
): Resource<T> {
    var attempt = 0
    while (true) {
        val result: Resource<T> = withContext(Dispatchers.IO) {
            try {
                Resource.Success(apiCall())
            } catch (t: Throwable) {
                Timber.e(t, "API call failed (attempt %d)", attempt + 1)
                t.toErrorResource()
            }
        }
        if (result is Resource.Success) return result
        val error    = result as Resource.Error
        val canRetry = error.type is ErrorType.Network && attempt < retries
        if (!canRetry) return error
        val backoffMs = (initialDelayMs * (1L shl attempt)).coerceAtMost(10_000L)
        delay(backoffMs)
        attempt++
    }
}

private fun Throwable.toErrorResource(): Resource.Error = when (this) {
    is IOException   -> Resource.Error(type = ErrorType.Network)
    is HttpException -> when (val code = code()) {
        401, 403 -> Resource.Error(type = ErrorType.Auth)
        else     -> Resource.Error(type = ErrorType.Server(code))
    }
    else -> Resource.Error(type = ErrorType.Data, throwable = this)
}
