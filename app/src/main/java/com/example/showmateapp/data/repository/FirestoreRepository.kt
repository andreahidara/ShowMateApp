package com.example.showmateapp.data.repository

import com.example.showmateapp.data.network.Movie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    suspend fun toggleFavorite(movie: Movie): Boolean {
        val uid = getCurrentUserId() ?: return false
        val docRef = db.collection("users").document(uid).collection("favorites").document(movie.id.toString())
        
        return try {
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.delete().await()
                false // removed
            } else {
                docRef.set(movie).await()
                true // added
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFavorites(): List<Movie> {
        val uid = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("favorites").get().await()
            snapshot.toObjects(Movie::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
