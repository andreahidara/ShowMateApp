package com.example.showmateapp.data.repository

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.TvShow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = db.collection("users")

    suspend fun getUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    suspend fun getFavorites(): List<TvShow> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(uid).collection("favorites").get().await()
            snapshot.toObjects(TvShow::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(tvShow: TvShow): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val favRef = usersCollection.document(uid).collection("favorites").document(tvShow.id.toString())

        return try {
            val doc = favRef.get().await()
            if (doc.exists()) {
                favRef.delete().await()
                false
            } else {
                favRef.set(tvShow).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserInterests(
        genres: List<String>,
        keywords: List<String>,
        actors: List<Int>,
        isPositive: Boolean
    ) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            val factor = if (isPositive) 1 else -1

            val newGenreScores = profile.genreScores.toMutableMap()
            genres.forEach { id ->
                newGenreScores[id] = (newGenreScores[id] ?: 0) + factor
            }

            transaction.set(userRef, profile.copy(
                genreScores = newGenreScores,
                preferredKeywords = if (isPositive) profile.preferredKeywords + keywords else profile.preferredKeywords,
                preferredActors = if (isPositive) profile.preferredActors + actors else profile.preferredActors
            ))
        }.await()
    }
}
