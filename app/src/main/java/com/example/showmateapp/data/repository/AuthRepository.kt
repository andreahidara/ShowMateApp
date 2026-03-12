package com.example.showmateapp.data.repository

import com.google.firebase.auth.FirebaseAuth

import kotlin.Result;
import kotlin.Unit;
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
        private val firebaseAuth: FirebaseAuth
) {
    // Función para iniciar sesión devolviendo un Result (éxito o error)
    suspend fun login(email: String, pass: String):Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Función para registrarse
    suspend fun signUp(email: String, pass: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser
}