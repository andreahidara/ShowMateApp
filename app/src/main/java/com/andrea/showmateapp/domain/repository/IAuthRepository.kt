package com.andrea.showmateapp.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun login(email: String, pass: String): Result<Unit>
    suspend fun signUp(email: String, pass: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>
    fun getCurrentUser(): FirebaseUser?
    val authState: Flow<Boolean>
    fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
