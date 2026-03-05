package com.example.showmateapp.data.repository

import com.example.showmateapp.data.network.TvShow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Obtiene el email del usuario actual
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Incrementa la puntuación de un género para el algoritmo de recomendación
     */
    fun incrementGenreScore(userId: String, genreId: String) {
        val userRef = db.collection("users").document(userId)

        userRef.update("genreScores.$genreId", FieldValue.increment(1))
            .addOnFailureListener {
                val initialData = hashMapOf(
                    "genreScores" to hashMapOf(genreId to 1)
                )
                userRef.set(initialData, SetOptions.merge())
            }
    }

    /**
     * Obtiene la lista de favoritos del usuario
     */
    suspend fun getFavorites(): List<TvShow> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("favorites").get().await()
            snapshot.toObjects(TvShow::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Añade o quita una serie de favoritos
     */
    suspend fun toggleFavorite(tvShow: TvShow): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val docRef = db.collection("users").document(userId)
            .collection("favorites").document(tvShow.id.toString())

        return try {
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.delete().await()
                false
            } else {
                docRef.set(tvShow).await()
                tvShow.genre_ids?.forEach { id ->
                    incrementGenreScore(userId, id.toString())
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}