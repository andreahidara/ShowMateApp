package com.example.showmateapp.data.repository

import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.network.MediaContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
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

    suspend fun getWatchedShows(): List<MediaContent> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(uid).collection("watched").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleWatched(media: MediaContent): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val watchedRef = usersCollection.document(uid).collection("watched").document(media.id.toString())

        return try {
            val doc = watchedRef.get().await()
            if (doc.exists()) {
                watchedRef.delete().await()
                false
            } else {
                watchedRef.set(media).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFavorites(): List<MediaContent> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(uid).collection("favorites").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(media: MediaContent): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val favRef = usersCollection.document(uid).collection("favorites").document(media.id.toString())

        return try {
            val doc = favRef.get().await()
            if (doc.exists()) {
                favRef.delete().await()
                false
            } else {
                favRef.set(media).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserRating(mediaId: Int): Int? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = usersCollection.document(uid).collection("ratings").document(mediaId.toString()).get().await()
            if (doc.exists()) {
                doc.getLong("rating")?.toInt()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateRating(mediaId: Int, rating: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersCollection.document(uid).collection("ratings").document(mediaId.toString())
                .set(mapOf("rating" to rating)).await()
        } catch (_: Exception) {}
    }

    suspend fun deleteRating(mediaId: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersCollection.document(uid).collection("ratings").document(mediaId.toString()).delete().await()
        } catch (_: Exception) {}
    }

    suspend fun saveOnboardingInterests(genres: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            val newGenreScores = profile.genreScores.toMutableMap()
            genres.forEach { id ->
                newGenreScores[id] = (newGenreScores[id] ?: 0f) + 15f
            }

            transaction.set(userRef, profile.copy(genreScores = newGenreScores))
        }.await()
    }

    suspend fun trackMediaInteraction(
        mediaId: Int,
        genres: List<String>,
        keywords: List<String>,
        actors: List<Int>,
        interactionType: InteractionType
    ) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            var weightModifier = 0f
            val newLiked = profile.likedMediaIds.toMutableList()
            val newDisliked = profile.dislikedMediaIds.toMutableList()
            val newRatings = profile.ratings.toMutableMap()

            when (interactionType) {
                is InteractionType.Like -> {
                    weightModifier = 5f
                    if (!newLiked.contains(mediaId)) newLiked.add(mediaId)
                    newDisliked.remove(mediaId)
                }
                is InteractionType.Dislike -> {
                    weightModifier = -2f
                    if (!newDisliked.contains(mediaId)) newDisliked.add(mediaId)
                    newLiked.remove(mediaId)
                }
                is InteractionType.Rate -> {
                    val rating = interactionType.score
                    newRatings[mediaId.toString()] = rating.toFloat()
                    weightModifier = when (rating) {
                        5 -> 4f
                        4 -> 2f
                        3 -> 0f
                        2 -> -1f
                        1 -> -3f
                        else -> 0f
                    }
                }
                is InteractionType.Watched -> {
                    weightModifier = 3f
                }
            }

            val newGenreScores = profile.genreScores.toMutableMap()
            genres.forEach { id ->
                newGenreScores[id] = (newGenreScores[id] ?: 0f) + weightModifier
            }

            val newKeywordScores = profile.preferredKeywords.toMutableMap()
            keywords.forEach { kw ->
                newKeywordScores[kw] = (newKeywordScores[kw] ?: 0f) + weightModifier
            }

            val newActorScores = profile.preferredActors.toMutableMap()
            actors.forEach { actorId ->
                val actorKey = actorId.toString()
                newActorScores[actorKey] = (newActorScores[actorKey] ?: 0f) + weightModifier
            }

            transaction.set(userRef, profile.copy(
                genreScores = newGenreScores,
                preferredKeywords = newKeywordScores,
                preferredActors = newActorScores,
                likedMediaIds = newLiked,
                dislikedMediaIds = newDisliked,
                ratings = newRatings
            ))
        }.await()
    }

    sealed class InteractionType {
        object Like : InteractionType()
        object Dislike : InteractionType()
        object Watched : InteractionType()
        data class Rate(val score: Int) : InteractionType()
    }

    suspend fun resetAlgorithmData() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            transaction.set(userRef, profile.copy(
                genreScores = emptyMap(),
                preferredKeywords = emptyMap(),
                preferredActors = emptyMap(),
                likedMediaIds = emptyList(),
                dislikedMediaIds = emptyList(),
                ratings = emptyMap()
            ))
        }.await()
    }
}
