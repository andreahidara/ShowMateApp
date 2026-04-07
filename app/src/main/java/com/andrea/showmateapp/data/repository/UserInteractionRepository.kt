package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.MediaInteractionEntity
import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.ActivityEvent
import com.andrea.showmateapp.data.model.MediaEntity
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.toEntity
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository.InteractionType
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInteractionRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val interactionDao: MediaInteractionDao,
    private val showDao: ShowDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val socialRepository: Lazy<ISocialRepository>,
    private val achievementChecker: Lazy<AchievementChecker>
) : IInteractionRepository {

    private val usersCollection = db.collection("users")
    private fun userDoc(uid: String) = usersCollection.document(uid)

    override fun getLikedShowsFlow(): Flow<List<MediaEntity>> = showDao.getLikedShowsFlow()

    override fun getWatchedShowsFlow(): Flow<List<MediaEntity>> = showDao.getWatchedShowsFlow()

    override suspend fun getWatchedShows(): List<MediaContent> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = userDoc(uid).collection("watched").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getWatchlist(): List<MediaContent> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = userDoc(uid).collection("watchlist").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getFavorites(): List<MediaContent> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = userDoc(uid).collection("favorites").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getEssentials(): List<MediaContent> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = userDoc(uid).collection("essentials").get().await()
            snapshot.toObjects(MediaContent::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getWatchedMediaIds(): Set<Int> = withContext(ioDispatcher) {
        interactionDao.getWatchedMediaIds().toSet()
    }

    override fun getWatchedMediaIdsFlow(): Flow<Set<Int>> = interactionDao.getWatchedMediaIdsFlow()
        .map { it.toSet() }

    override suspend fun getLocalInteractionState(mediaId: Int): MediaInteractionEntity? = withContext(ioDispatcher) {
        interactionDao.getById(mediaId)
    }

    override suspend fun toggleWatched(media: MediaContent, setWatched: Boolean): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val watchedRef = userDoc(uid).collection("watched").document(media.id.toString())

val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
        interactionDao.upsert(current.copy(isWatched = setWatched, syncPending = true))
        if (setWatched) showDao.insertShows(listOf(media.toEntity(ShowRepository.CAT_WATCHED)))
        else showDao.deleteWatchedShow(media.id)

try {
            if (setWatched) watchedRef.set(media).await() else watchedRef.delete().await()
            interactionDao.upsert(current.copy(isWatched = setWatched, syncPending = false))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Firestore toggleWatched failed, will sync later")
        }
        setWatched
    }

    override suspend fun toggleFavorite(media: MediaContent, setLiked: Boolean): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val favRef = userDoc(uid).collection("favorites").document(media.id.toString())

val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
        interactionDao.upsert(current.copy(isLiked = setLiked, syncPending = true))
        if (setLiked) showDao.insertShows(listOf(media.toEntity(ShowRepository.CAT_LIKED)))
        else showDao.deleteLikedShow(media.id)

try {
            if (setLiked) {
                favRef.set(media).await()
                runCatching {
                    socialRepository.get().postActivityEvent(
                        type        = ActivityEvent.TYPE_LIKED,
                        mediaId     = media.id,
                        mediaTitle  = media.name,
                        mediaPoster = media.posterPath ?: ""
                    )
                }
                runCatching { achievementChecker.get().addXp(AchievementDefs.XP_LIKE_SHOW) }
            } else {
                favRef.delete().await()
            }
            interactionDao.upsert(current.copy(isLiked = setLiked, syncPending = false))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Firestore toggleFavorite failed, will sync later")
        }
        setLiked
    }

    override suspend fun toggleEssential(media: MediaContent, setEssential: Boolean): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val essentialRef = userDoc(uid).collection("essentials").document(media.id.toString())

val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
        interactionDao.upsert(current.copy(isEssential = setEssential, syncPending = true))

try {
            if (setEssential) {
                essentialRef.set(media).await()
                runCatching {
                    socialRepository.get().postActivityEvent(
                        type        = ActivityEvent.TYPE_ESSENTIAL,
                        mediaId     = media.id,
                        mediaTitle  = media.name,
                        mediaPoster = media.posterPath ?: ""
                    )
                }
                runCatching { achievementChecker.get().addXp(AchievementDefs.XP_LIKE_SHOW) }
            } else {
                essentialRef.delete().await()
            }
            interactionDao.upsert(current.copy(isEssential = setEssential, syncPending = false))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Firestore toggleEssential failed, will sync later")
        }
        setEssential
    }

    override suspend fun toggleWatchlist(media: MediaContent, add: Boolean): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val watchlistRef = userDoc(uid).collection("watchlist").document(media.id.toString())

val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
        interactionDao.upsert(current.copy(isInWatchlist = add, syncPending = true))

try {
            if (add) watchlistRef.set(media).await() else watchlistRef.delete().await()
            interactionDao.upsert(current.copy(isInWatchlist = add, syncPending = false))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Firestore toggleWatchlist failed, will sync later")
        }
        add
    }

    override suspend fun isInWatchlist(mediaId: Int): Boolean = withContext(ioDispatcher) {
        interactionDao.getById(mediaId)?.isInWatchlist ?: false
    }

    override suspend fun cacheInteractionState(mediaId: Int, isLiked: Boolean, isEssential: Boolean, isWatched: Boolean) = withContext(ioDispatcher) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(isLiked = isLiked, isEssential = isEssential, isWatched = isWatched))
    }

    override suspend fun trackMediaInteraction(
        mediaId: Int,
        genres: List<String>,
        keywords: List<String>,
        actors: List<Int>,
        narrativeStyles: Map<String, Float>,
        creators: List<Int>,
        interactionType: InteractionType
    ) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)

        try {
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
                    is InteractionType.Rate -> {
                        val rating = interactionType.score
                        newRatings[mediaId.toString()] = rating.toFloat()
                        val userAvg = if (profile.ratings.isNotEmpty())
                            profile.ratings.values.average().toFloat() else 3f
                        weightModifier = ((rating - userAvg) * 1.5f).coerceIn(-4f, 4f)
                    }
                    is InteractionType.Watched -> {
                        weightModifier = 3f
                    }
                    is InteractionType.Dislike -> {
                        weightModifier = -8f
                        if (!newDisliked.contains(mediaId)) newDisliked.add(mediaId)
                        newLiked.remove(mediaId)
                        newEssential.remove(mediaId)
                    }
                    else -> {}
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
        } catch (e: Exception) {
            Timber.e(e, "trackMediaInteraction failed")
        }
    }

    override suspend fun updateRating(mediaId: Int, rating: Int) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        userDoc(uid).collection("ratings").document(mediaId.toString())
            .set(mapOf("rating" to rating)).await()
        runCatching {
            socialRepository.get().postActivityEvent(
                type        = ActivityEvent.TYPE_RATED,
                mediaId     = mediaId,
                mediaTitle  = mediaId.toString(),
                mediaPoster = "",
                score       = rating.toFloat()
            )
        }
        runCatching { achievementChecker.get().addXp(AchievementDefs.XP_RATE_SHOW) }
        Unit
    }

    override suspend fun deleteRating(mediaId: Int) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        userDoc(uid).collection("ratings").document(mediaId.toString()).delete().await()
    }

    override suspend fun getAllRatings(): Map<Int, Int> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyMap()
        try {
            val snapshot = userDoc(uid).collection("ratings").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.id.toIntOrNull() ?: return@mapNotNull null
                val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                id to rating
            }.toMap()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyMap()
        }
    }

    override suspend fun getUserRating(mediaId: Int): Int? = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        try {
            val doc = userDoc(uid).collection("ratings").document(mediaId.toString()).get().await()
            if (doc.exists()) doc.getLong("rating")?.toInt() else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun saveReview(mediaId: Int, text: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        try {
            db.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                val reviews = profile.mediaReviews.toMutableMap()
                if (text.isBlank()) reviews.remove(mediaId.toString())
                else reviews[mediaId.toString()] = text
                transaction.set(userRef, profile.copy(mediaReviews = reviews))
            }.await()
        } catch (e: Exception) {
            Timber.e(e, "saveReview failed")
            throw e
        }
    }

    override suspend fun getReview(mediaId: Int): String? = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        try {
            val snapshot = userDoc(uid).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
            profile?.mediaReviews?.get(mediaId.toString())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun addToCustomList(listName: String, mediaId: Int) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        try {
            db.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                val lists   = profile.customLists.toMutableMap()
                val items   = (lists[listName] ?: emptyList()).toMutableList()
                if (!items.contains(mediaId)) {
                    items.add(mediaId)
                    lists[listName] = items
                    transaction.set(userRef, profile.copy(customLists = lists))
                }
            }.await()
        } catch (e: Exception) {
            Timber.e(e, "addToCustomList failed")
        }
    }

    override suspend fun removeFromCustomList(listName: String, mediaId: Int) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        try {
            db.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                val lists   = profile.customLists.toMutableMap()
                val items   = (lists[listName] ?: emptyList()).toMutableList()
                if (items.remove(mediaId)) {
                    lists[listName] = items
                    transaction.set(userRef, profile.copy(customLists = lists))
                }
            }.await()
        } catch (e: Exception) {
            Timber.e(e, "removeFromCustomList failed")
        }
    }

    override suspend fun createCustomList(listName: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        try {
            db.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                val lists   = profile.customLists.toMutableMap()
                if (!lists.containsKey(listName)) {
                    lists[listName] = emptyList()
                    transaction.set(userRef, profile.copy(customLists = lists))
                }
            }.await()
        } catch (e: Exception) {
            Timber.e(e, "createCustomList failed")
        }
    }

    override suspend fun deleteCustomList(listName: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        try {
            db.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                val lists   = profile.customLists.toMutableMap()
                if (lists.remove(listName) != null) {
                    transaction.set(userRef, profile.copy(customLists = lists))
                }
            }.await()
        } catch (e: Exception) {
            Timber.e(e, "deleteCustomList failed")
        }
    }

    override suspend fun getCustomLists(): Map<String, List<Int>> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyMap()
        try {
            val snapshot = userDoc(uid).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
            profile?.customLists ?: emptyMap()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyMap()
        }
    }

    override suspend fun toggleEpisodeWatched(showId: Int, episodeId: Int): Boolean = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val userRef = userDoc(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            val watchedEpisodes = profile.watchedEpisodes.toMutableMap()
            val episodesForShow = watchedEpisodes[showId.toString()]?.toMutableSet() ?: mutableSetOf()

            val isNowWatched = if (episodesForShow.contains(episodeId)) {
                episodesForShow.remove(episodeId)
                false
            } else {
                episodesForShow.add(episodeId)
                true
            }

            watchedEpisodes[showId.toString()] = episodesForShow.toList()
            transaction.update(userRef, "watchedEpisodes", watchedEpisodes)
            isNowWatched
        }.await().also { isNowWatched ->
            if (isNowWatched) runCatching { achievementChecker.get().addXp(AchievementDefs.XP_WATCH_EPISODE) }
        }
    }

    override suspend fun setAllEpisodesWatched(showId: Int, episodeIds: List<Int>) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
            val watchedEpisodes = profile.watchedEpisodes.toMutableMap()

            if (episodeIds.isEmpty()) {
                watchedEpisodes.remove(showId.toString())
            } else {
                watchedEpisodes[showId.toString()] = episodeIds
            }
            transaction.update(userRef, "watchedEpisodes", watchedEpisodes)
            null
        }.await()
    }

    override suspend fun getWatchedShowsWithSeasonCount() = withContext(ioDispatcher) {
        interactionDao.getWatchedWithSeasonCount()
    }

    override suspend fun updateLastKnownSeasons(mediaId: Int, seasons: Int) = withContext(ioDispatcher) {
        interactionDao.updateLastKnownSeasons(mediaId, seasons)
    }

    override suspend fun syncFavoritesAndWatchedToRoom() = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val (favorites, watched) = coroutineScope {
                val favJob     = async { userDoc(uid).collection("favorites").get().await().toObjects(MediaContent::class.java) }
                val watchedJob = async { userDoc(uid).collection("watched").get().await().toObjects(MediaContent::class.java) }
                favJob.await() to watchedJob.await()
            }

            showDao.replaceCategory(ShowRepository.CAT_LIKED,   favorites.map { it.toEntity(ShowRepository.CAT_LIKED) })
            showDao.replaceCategory(ShowRepository.CAT_WATCHED, watched.map  { it.toEntity(ShowRepository.CAT_WATCHED) })

            val favoriteIds = favorites.map { it.id }.toSet()
            val watchedIds  = watched.map  { it.id }.toSet()
            val allIds      = (favoriteIds + watchedIds).toList()
            if (allIds.isNotEmpty()) {
                val existingMap = interactionDao.getByIds(allIds).associateBy { it.mediaId }
                val toUpsert = allIds.mapNotNull { id ->
                    val existing = existingMap[id] ?: MediaInteractionEntity(mediaId = id)
                    if (!existing.syncPending) {
                        existing.copy(
                            isLiked   = if (id in favoriteIds) true else existing.isLiked,
                            isWatched = if (id in watchedIds)  true else existing.isWatched
                        )
                    } else null
                }
                interactionDao.upsertAll(toUpsert)
            }
        } catch (e: Exception) {
            Timber.w(e, "Firestore→Room sync failed")
        }
    }
}
