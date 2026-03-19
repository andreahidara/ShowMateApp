package com.example.showmateapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import android.util.Log

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> {
    return withContext(Dispatchers.IO) {
        try {
            Resource.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            Log.e("safeApiCall", "API Call Failed: ${throwable.message}", throwable)
            when (throwable) {
                is IOException -> Resource.Error("Error de conexión. Revisa tu internet.")
                is HttpException -> {
                    val code = throwable.code()
                    val message = "Error del servidor ($code)"
                    Resource.Error(message, throwable)
                }
                else -> {
                    Resource.Error("Ha ocurrido un error inesperado", throwable)
                }
            }
        }
    }
}

