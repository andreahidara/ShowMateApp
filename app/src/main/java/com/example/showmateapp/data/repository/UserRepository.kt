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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    @Volatile private var _profileCache: UserProfile? = null
    @Volatile private var _profileCacheTime: Long = 0L
    // Distinguishes "never fetched" from "fetched but null" (missing/invalid Firestore document)
    @Volatile private var _profileCacheFetched: Boolean = false
    private companion object { private const val PROFILE_CACHE_TTL = 120_000L }

    private fun invalidateProfileCache() {
        _profileCache = null
        _profileCacheFetched = false
    }

    suspend fun getUserProfile(): UserProfile? {
        if (_profileCacheFetched && System.currentTimeMillis() - _profileCacheTime < PROFILE_CACHE_TTL) {
            return _profileCache
        }
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(UserProfile::class.java).also {
                _profileCache = it
                _profileCacheTime = System.currentTimeMillis()
                _profileCacheFetched = true
            }
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
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isWatched = setWatched))
            if (setWatched) {
                showDao.insertShows(listOf(media.toEntity(ShowRepository.CAT_WATCHED)))
                watchedRef.set(media).await()
            } else {
                showDao.deleteWatchedShow(media.id)
                watchedRef.delete().await()
            }
            setWatched
        } catch (e: Exception) {
            setWatched // Room already updated; Firestore will sync when connection is restored
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
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isLiked = setLiked))
            if (setLiked) {
                showDao.insertShows(listOf(media.toEntity(ShowRepository.CAT_LIKED)))
                favRef.set(media).await()
            } else {
                showDao.deleteLikedShow(media.id)
                favRef.delete().await()
            }
            setLiked
        } catch (e: Exception) {
            setLiked // Room already updated; Firestore will sync when connection is restored
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
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isEssential = setEssential))
            if (setEssential) {
                essentialRef.set(media).await()
            } else {
                essentialRef.delete().await()
            }
            setEssential
        } catch (e: Exception) {
            setEssential // Room already updated; Firestore will sync when connection is restored
        }
    }

    suspend fun getWatchlist(): List<MediaContent> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(uid).collection("watchlist").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun isInWatchlist(mediaId: Int): Boolean {
        return interactionDao.getById(mediaId)?.isInWatchlist ?: false
    }

    suspend fun toggleWatchlist(media: MediaContent, add: Boolean): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val watchlistRef = usersCollection.document(uid).collection("watchlist").document(media.id.toString())

        return try {
            val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
            interactionDao.upsert(current.copy(isInWatchlist = add))
            if (add) {
                watchlistRef.set(media).await()
            } else {
                watchlistRef.delete().await()
            }
            add
        } catch (e: Exception) {
            add // Room already updated; Firestore will sync when connection is restored
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

    suspend fun initUserProfile(username: String) {
        val uid   = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        usersCollection.document(uid)
            .set(
                mapOf("userId" to uid, "username" to username, "email" to email),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
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
        invalidateProfileCache()
    }

    suspend fun trackMediaInteraction(
        mediaId: Int,
        genres: List<String>,
        keywords: List<String>,
        actors: List<Int>,
        creators: List<Int> = emptyList(),
        narrativeStyles: Map<String, Float> = emptyMap(),
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
                    // Normalises against the user's own average to compensate for personal rating bias
                    val userAvg = if (profile.ratings.isNotEmpty())
                        profile.ratings.values.average().toFloat() else 3f
                    weightModifier = ((rating - userAvg) * 1.5f).coerceIn(-4f, 4f)
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

            val newNarrativeScores = profile.narrativeStyleScores.toMutableMap()
            val newNarrativeDates  = profile.narrativeStyleDates.toMutableMap()
            narrativeStyles.forEach { (style, relevance) ->
                newNarrativeScores[style] = (newNarrativeScores[style] ?: 0f) + weightModifier * relevance
                newNarrativeDates[style]  = now
            }

            val newCreatorScores = profile.preferredCreators.toMutableMap()
            val newCreatorDates  = profile.creatorScoreDates.toMutableMap()
            creators.forEach { creatorId ->
                val key = creatorId.toString()
                newCreatorScores[key] = (newCreatorScores[key] ?: 0f) + weightModifier
                newCreatorDates[key]  = now
            }

            transaction.set(userRef, profile.copy(
                genreScores          = newGenreScores,
                genreScoreDates      = newGenreDates,
                preferredKeywords    = newKeywordScores,
                keywordScoreDates    = newKeywordDates,
                preferredActors      = newActorScores,
                actorScoreDates      = newActorDates,
                narrativeStyleScores = newNarrativeScores,
                narrativeStyleDates  = newNarrativeDates,
                preferredCreators    = newCreatorScores,
                creatorScoreDates    = newCreatorDates,
                likedMediaIds        = newLiked,
                essentialMediaIds    = newEssential,
                dislikedMediaIds     = newDisliked,
                ratings              = newRatings
            ))
        }.await()
        invalidateProfileCache()
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
                genreScores          = emptyMap(),
                genreScoreDates      = emptyMap(),
                preferredKeywords    = emptyMap(),
                keywordScoreDates    = emptyMap(),
                preferredActors      = emptyMap(),
                actorScoreDates      = emptyMap(),
                narrativeStyleScores = emptyMap(),
                narrativeStyleDates  = emptyMap(),
                preferredCreators    = emptyMap(),
                creatorScoreDates    = emptyMap(),
                likedMediaIds        = emptyList(),
                essentialMediaIds    = emptyList(),
                dislikedMediaIds     = emptyList(),
                ratings              = emptyMap(),
                watchedEpisodes      = emptyMap()
            ))
        }.await()
        invalidateProfileCache()
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
        }.await().also { invalidateProfileCache() }
    }

    suspend fun updateProfile(username: String) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid)
            .set(mapOf("username" to username), com.google.firebase.firestore.SetOptions.merge())
            .await()
        invalidateProfileCache()
    }

    suspend fun saveReview(mediaId: Int, review: String) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val reviews = profile.mediaReviews.toMutableMap()
            if (review.isBlank()) reviews.remove(mediaId.toString())
            else reviews[mediaId.toString()] = review
            transaction.set(userRef, profile.copy(mediaReviews = reviews))
        }.await()
        invalidateProfileCache()
    }

    suspend fun getReview(mediaId: Int): String? {
        val profile = getUserProfile() ?: return null
        return profile.mediaReviews[mediaId.toString()]
    }

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
        invalidateProfileCache()
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
        invalidateProfileCache()
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
        invalidateProfileCache()
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
        invalidateProfileCache()
    }

    suspend fun getSimilarUsers(limit: Long = 30): List<UserProfile> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection
                .whereNotEqualTo("userId", uid)
                .limit(limit)
                .get()
                .await()
            snapshot.toObjects(UserProfile::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun userExists(email: String): Boolean {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFriendProfile(friendEmail: String): UserProfile? {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("email", friendEmail)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun compareWithFriend(friendEmail: String): List<Int> {
        return try {
            val (myProfile, friendProfile) = coroutineScope {
                val mine   = async { getUserProfile() }
                val friend = async { getFriendProfile(friendEmail) }
                mine.await() to friend.await()
            }
            if (myProfile == null || friendProfile == null) return@compareWithFriend emptyList()
            val myLiked     = myProfile.likedMediaIds.toSet()
            val friendLiked = friendProfile.likedMediaIds.toSet()
            (myLiked intersect friendLiked).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordViewingSession(showId: Int, episodeCount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        val today   = java.time.LocalDate.now().toString()
        val entry   = "$today:$showId:$episodeCount"
        db.runTransaction { transaction ->
            val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val history = profile.viewingHistory.toMutableList()
            val existingIdx = history.indexOfFirst { it.startsWith("$today:$showId:") }
            if (existingIdx >= 0) history[existingIdx] = entry else history.add(entry)
            transaction.set(userRef, profile.copy(viewingHistory = history))
        }.await()
        invalidateProfileCache()
    }

    suspend fun updateLastKnownSeasons(mediaId: Int, seasonCount: Int) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(lastKnownSeasons = seasonCount))
    }

    suspend fun getWatchedShowsWithSeasonCount() = interactionDao.getWatchedWithSeasonCount()

    suspend fun syncFavoritesAndWatchedToRoom() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val (favorites, watched) = coroutineScope {
                val favJob     = async { usersCollection.document(uid).collection("favorites").get().await().toObjects(MediaContent::class.java) }
                val watchedJob = async { usersCollection.document(uid).collection("watched").get().await().toObjects(MediaContent::class.java) }
                favJob.await() to watchedJob.await()
            }

            showDao.replaceCategory(ShowRepository.CAT_LIKED,   favorites.map { it.toEntity(ShowRepository.CAT_LIKED) })
            showDao.replaceCategory(ShowRepository.CAT_WATCHED, watched.map  { it.toEntity(ShowRepository.CAT_WATCHED) })

            val favoriteIds = favorites.map { it.id }.toSet()
            val watchedIds  = watched.map  { it.id }.toSet()
            val allIds      = (favoriteIds + watchedIds).toList()
            if (allIds.isNotEmpty()) {
                val existingMap = interactionDao.getByIds(allIds).associateBy { it.mediaId }
                val toUpsert = allIds.map { id ->
                    val existing = existingMap[id] ?: MediaInteractionEntity(mediaId = id)
                    existing.copy(
                        isLiked   = if (id in favoriteIds) true else existing.isLiked,
                        isWatched = if (id in watchedIds)  true else existing.isWatched
                    )
                }
                interactionDao.upsertAll(toUpsert)
            }
        } catch (e: Exception) {
            android.util.Log.w("UserRepository", "Firestore→Room sync failed; local data unchanged", e)
        }
    }

    suspend fun clearUserCache() {
        showDao.clearUserData()
        invalidateProfileCache()
    }

    suspend fun cacheInteractionState(mediaId: Int, isLiked: Boolean, isEssential: Boolean, isWatched: Boolean) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(isLiked = isLiked, isEssential = isEssential, isWatched = isWatched))
    }
}
