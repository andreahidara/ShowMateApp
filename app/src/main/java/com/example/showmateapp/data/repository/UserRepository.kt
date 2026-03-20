package com.example.showmateapp.data.repository

import com.example.showmateapp.data.local.MediaInteractionDao
import com.example.showmateapp.data.local.MediaInteractionEntity
import com.example.showmateapp.data.local.ShowDao
import com.example.showmateapp.data.model.MediaEntity
import com.example.showmateapp.data.model.UserProfile
import com.example.showmateapp.data.model.toEntity
import com.example.showmateapp.data.network.MediaContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val interactionDao: MediaInteractionDao,
    private val showDao: ShowDao
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

    suspend fun getLocalInteractionState(mediaId: Int): MediaInteractionEntity? {
        return interactionDao.getById(mediaId)
    }

    suspend fun toggleWatched(media: MediaContent, setWatched: Boolean): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val watchedRef = usersCollection.document(uid).collection("watched").document(media.id.toString())

        return try {
            // Write to Room first so state persists even if Firestore is slow/unavailable
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isWatched = setWatched))
            if (setWatched) {
                showDao.insertShows(listOf(media.toEntity("watched")))
                watchedRef.set(media).await()
            } else {
                showDao.deleteWatchedShow(media.id)
                watchedRef.delete().await()
            }
            setWatched
        } catch (e: Exception) {
            setWatched // Room already updated; Firestore will sync when back online
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

    fun getLikedShowsFlow(): Flow<List<MediaEntity>> = showDao.getLikedShowsFlow()

    fun getWatchedShowsFlow(): Flow<List<MediaEntity>> = showDao.getWatchedShowsFlow()

    suspend fun getWatchedMediaIds(): Set<Int> = interactionDao.getWatchedMediaIds().toSet()

    suspend fun toggleFavorite(media: MediaContent, setLiked: Boolean): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val favRef = usersCollection.document(uid).collection("favorites").document(media.id.toString())

        return try {
            // Write to Room first so state persists even if Firestore is slow/unavailable
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isLiked = setLiked))
            if (setLiked) {
                showDao.insertShows(listOf(media.toEntity("liked")))
                favRef.set(media).await()
            } else {
                showDao.deleteLikedShow(media.id)
                favRef.delete().await()
            }
            setLiked
        } catch (e: Exception) {
            setLiked // Room already updated; Firestore will sync when back online
        }
    }

    suspend fun getEssentials(): List<MediaContent> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(uid).collection("essentials").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleEssential(media: MediaContent, setEssential: Boolean): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val essentialRef = usersCollection.document(uid).collection("essentials").document(media.id.toString())

        return try {
            // Write to Room first so state persists even if Firestore is slow/unavailable
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isEssential = setEssential))
            if (setEssential) {
                essentialRef.set(media).await()
            } else {
                essentialRef.delete().await()
            }
            setEssential
        } catch (e: Exception) {
            setEssential // Room already updated; Firestore will sync when back online
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
        usersCollection.document(uid).collection("ratings").document(mediaId.toString())
            .set(mapOf("rating" to rating)).await()
    }

    suspend fun deleteRating(mediaId: Int) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid).collection("ratings").document(mediaId.toString()).delete().await()
    }

    suspend fun getAllRatings(): Map<Int, Int> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        return try {
            val snapshot = usersCollection.document(uid).collection("ratings").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.id.toIntOrNull() ?: return@mapNotNull null
                val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                id to rating
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
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
            val newEssential = profile.essentialMediaIds.toMutableList()
            val newDisliked = profile.dislikedMediaIds.toMutableList()
            val newRatings = profile.ratings.toMutableMap()

            when (interactionType) {
                is InteractionType.Like -> {
                    weightModifier = 5f
                    if (!newLiked.contains(mediaId)) newLiked.add(mediaId)
                    newDisliked.remove(mediaId)
                }
                is InteractionType.Essential -> {
                    weightModifier = 10f
                    if (!newEssential.contains(mediaId)) newEssential.add(mediaId)
                    newDisliked.remove(mediaId)
                }
                is InteractionType.Dislike -> {
                    weightModifier = -2f
                    if (!newDisliked.contains(mediaId)) newDisliked.add(mediaId)
                    newLiked.remove(mediaId)
                    newEssential.remove(mediaId)
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

            val now = System.currentTimeMillis()

            val newGenreScores = profile.genreScores.toMutableMap()
            val newGenreDates  = profile.genreScoreDates.toMutableMap()
            genres.forEach { id ->
                newGenreScores[id] = (newGenreScores[id] ?: 0f) + weightModifier
                newGenreDates[id]  = now
            }

            val newKeywordScores = profile.preferredKeywords.toMutableMap()
            val newKeywordDates  = profile.keywordScoreDates.toMutableMap()
            keywords.forEach { kw ->
                newKeywordScores[kw] = (newKeywordScores[kw] ?: 0f) + weightModifier
                newKeywordDates[kw]  = now
            }

            val newActorScores = profile.preferredActors.toMutableMap()
            val newActorDates  = profile.actorScoreDates.toMutableMap()
            actors.forEach { actorId ->
                val key = actorId.toString()
                newActorScores[key] = (newActorScores[key] ?: 0f) + weightModifier
                newActorDates[key]  = now
            }

            transaction.set(userRef, profile.copy(
                genreScores       = newGenreScores,
                genreScoreDates   = newGenreDates,
                preferredKeywords = newKeywordScores,
                keywordScoreDates = newKeywordDates,
                preferredActors   = newActorScores,
                actorScoreDates   = newActorDates,
                likedMediaIds     = newLiked,
                essentialMediaIds = newEssential,
                dislikedMediaIds  = newDisliked,
                ratings           = newRatings
            ))
        }.await()
    }

    sealed class InteractionType {
        object Like : InteractionType()
        object Essential : InteractionType()
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
                essentialMediaIds = emptyList(),
                dislikedMediaIds = emptyList(),
                ratings = emptyMap(),
                watchedEpisodes = emptyMap()
            ))
        }.await()
    }

    suspend fun toggleEpisodeWatched(showId: Int, episodeId: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val userRef = usersCollection.document(uid)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            val watchedEpisodes = profile.watchedEpisodes.toMutableMap()
            val episodesForShow = watchedEpisodes[showId.toString()]?.toMutableList() ?: mutableListOf()

            val isNowWatched = if (episodesForShow.contains(episodeId)) {
                episodesForShow.remove(episodeId)
                false
            } else {
                episodesForShow.add(episodeId)
                true
            }

            watchedEpisodes[showId.toString()] = episodesForShow
            transaction.set(userRef, profile.copy(watchedEpisodes = watchedEpisodes))
            isNowWatched
        }.await()
    }

    suspend fun updateProfile(username: String) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid)
            .set(mapOf("username" to username), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    suspend fun saveReview(mediaId: Int, review: String) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val reviews = profile.mediaReviews.toMutableMap()
            reviews[mediaId.toString()] = review
            transaction.set(userRef, profile.copy(mediaReviews = reviews))
        }.await()
    }

    suspend fun getReview(mediaId: Int): String? {
        val profile = getUserProfile() ?: return null
        return profile.mediaReviews[mediaId.toString()]
    }

    // ── Custom Lists ──────────────────────────────────────────────────────────

    suspend fun getCustomLists(): Map<String, List<Int>> {
        return getUserProfile()?.customLists ?: emptyMap()
    }

    suspend fun createCustomList(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            if (!profile.customLists.containsKey(name)) {
                val lists = profile.customLists.toMutableMap()
                lists[name] = emptyList()
                transaction.set(userRef, profile.copy(customLists = lists))
            }
        }.await()
    }

    suspend fun addToCustomList(listName: String, mediaId: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val lists   = profile.customLists.toMutableMap()
            val items   = (lists[listName] ?: emptyList()).toMutableList()
            if (!items.contains(mediaId)) items.add(mediaId)
            lists[listName] = items
            transaction.set(userRef, profile.copy(customLists = lists))
        }.await()
    }

    suspend fun removeFromCustomList(listName: String, mediaId: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val lists = profile.customLists.toMutableMap()
            lists[listName] = (lists[listName] ?: emptyList()).filter { it != mediaId }
            transaction.set(userRef, profile.copy(customLists = lists))
        }.await()
    }

    suspend fun deleteCustomList(listName: String) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val lists = profile.customLists.toMutableMap()
            lists.remove(listName)
            transaction.set(userRef, profile.copy(customLists = lists))
        }.await()
    }

    // ── Compare with friend ───────────────────────────────────────────────────

    /** Returns media IDs that both the current user and [friendEmail] have liked. */
    suspend fun compareWithFriend(friendEmail: String): List<Int> {
        val myProfile = getUserProfile() ?: return emptyList()
        return try {
            val friendSnapshot = usersCollection
                .whereEqualTo("email", friendEmail)
                .limit(1)
                .get()
                .await()
            val friendProfile = friendSnapshot.documents.firstOrNull()
                ?.toObject(UserProfile::class.java) ?: return emptyList()
            val myLiked     = myProfile.likedMediaIds.toSet()
            val friendLiked = friendProfile.likedMediaIds.toSet()
            (myLiked intersect friendLiked).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Viewing history (for advanced stats) ─────────────────────────────────

    /** Records a session in the format "YYYY-MM-DD:showId:episodeCount". */
    suspend fun recordViewingSession(showId: Int, episodeCount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        val today   = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val entry   = "$today:$showId:$episodeCount"
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val history = profile.viewingHistory.toMutableList()
            // Update existing entry for today+show, or append
            val existingIdx = history.indexOfFirst { it.startsWith("$today:$showId:") }
            if (existingIdx >= 0) history[existingIdx] = entry else history.add(entry)
            transaction.set(userRef, profile.copy(viewingHistory = history))
        }.await()
    }

    // ── Season tracking (for new-season notifications) ────────────────────────

    suspend fun updateLastKnownSeasons(mediaId: Int, seasonCount: Int) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(lastKnownSeasons = seasonCount))
    }

    suspend fun getWatchedShowsWithSeasonCount() = interactionDao.getWatchedWithSeasonCount()

    /**
     * Syncs favorites and watched shows from Firestore into the local Room cache.
     * Called on startup so that the Favorites and Profile screens show data even
     * after a fresh install or after the local database was wiped.
     */
    suspend fun syncFavoritesAndWatchedToRoom() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val favSnapshot = usersCollection.document(uid).collection("favorites").get().await()
            val favorites = favSnapshot.toObjects(MediaContent::class.java)
            showDao.replaceCategory("liked", favorites.map { it.toEntity("liked") })
            favorites.forEach { media ->
                val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
                interactionDao.upsert(current.copy(isLiked = true))
            }

            val watchedSnapshot = usersCollection.document(uid).collection("watched").get().await()
            val watched = watchedSnapshot.toObjects(MediaContent::class.java)
            showDao.replaceCategory("watched", watched.map { it.toEntity("watched") })
            watched.forEach { media ->
                val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
                interactionDao.upsert(current.copy(isWatched = true))
            }
        } catch (e: Exception) {
            android.util.Log.w("UserRepository", "Firestore→Room sync failed; local data unchanged", e)
        }
    }

    /** Caches interaction state in Room so subsequent detail opens are instant. */
    suspend fun cacheInteractionState(mediaId: Int, isLiked: Boolean, isEssential: Boolean, isWatched: Boolean) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(isLiked = isLiked, isEssential = isEssential, isWatched = isWatched))
    }
}
