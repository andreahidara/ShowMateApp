package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.MediaInteractionEntity
import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.toEntity
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.util.safeFirestoreCall
import com.andrea.showmateapp.domain.repository.IInteractionRepository.InteractionType
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class UserInteractionRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val interactionDao: MediaInteractionDao,
    private val showDao: ShowDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userRepo: Lazy<IUserRepository>,
    private val socialRepo: Lazy<ISocialRepository>,
    private val achievement: Lazy<AchievementChecker>
) : IInteractionRepository {

    private val uid get() = auth.currentUser?.uid
    private fun userDoc() = uid?.let { db.collection("users").document(it) }

    private suspend fun <T> getCollection(path: String, clazz: Class<T>): List<T> = withContext(ioDispatcher) {
        safeFirestoreCall(emptyList()) { userDoc()?.collection(path)?.get()?.await()?.toObjects(clazz) ?: emptyList() }
    }

    override fun getLikedShowsFlow() = showDao.getLikedShowsFlow()
    override fun getWatchedShowsFlow() = showDao.getWatchedShowsFlow()
    override fun getWatchlistShowsFlow() = showDao.getWatchlistShowsFlow()
    override suspend fun getWatchedShows() = getCollection("watched", MediaContent::class.java)
    override suspend fun getWatchlist() = getCollection("watchlist", MediaContent::class.java)
    override suspend fun getFavorites() = getCollection("favorites", MediaContent::class.java)
    override suspend fun getEssentials() = getCollection("essentials", MediaContent::class.java)

    override suspend fun getWatchedMediaIds() = withContext(ioDispatcher) { interactionDao.getWatchedMediaIds().toSet() }
    override fun getWatchedMediaIdsFlow() = interactionDao.getWatchedMediaIdsFlow().map { it.toSet() }.distinctUntilChanged()
    override suspend fun getExcludedMediaIds() = withContext(ioDispatcher) { interactionDao.getExcludedMediaIds().toSet() }
    override fun getExcludedMediaIdsFlow() = interactionDao.getExcludedMediaIdsFlow().map { it.toSet() }.distinctUntilChanged()
    override fun getInteractedMediaIdsFlow() = interactionDao.getInteractedMediaIdsFlow().map { it.toSet() }.distinctUntilChanged()

    override suspend fun getLocalInteractionState(mediaId: Int) = withContext(ioDispatcher) { interactionDao.getById(mediaId) }

    private suspend fun toggleBase(
        media: MediaContent,
        coll: String,
        set: Boolean,
        updateDao: (MediaInteractionEntity) -> MediaInteractionEntity,
        onSuccess: suspend () -> Unit = {}
    ): Boolean = withContext(ioDispatcher) {
        val current = interactionDao.getById(media.id) ?: MediaInteractionEntity(mediaId = media.id)
        val optimisticState = updateDao(current).copy(syncPending = true)
        interactionDao.upsert(optimisticState)

        onSuccess()

        try {
            val ref = userDoc()?.collection(coll)?.document(media.id.toString())
            if (set) ref?.set(media)?.await() else ref?.delete()?.await()
            interactionDao.upsert(optimisticState.copy(syncPending = false))
            userRepo.get().getUserProfile()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Firestore sync failed for $coll")
        }
        set
    }

    override suspend fun toggleWatched(media: MediaContent, setWatched: Boolean) = toggleBase(
        media, "watched", setWatched,
        updateDao = {
            if (setWatched) it.copy(isWatched = true, isInWatchlist = false)
            else it.copy(isWatched = false)
        }
    ) {
        if (setWatched) {
            showDao.insertShows(listOf(media.toEntity("watched")))
            showDao.deleteWatchlistShow(media.id)
            runCatching {
                userDoc()?.collection("watchlist")?.document(media.id.toString())?.delete()?.await()
            }
        } else {
            showDao.deleteWatchedShow(media.id)
        }
    }

    override suspend fun toggleFavorite(media: MediaContent, setLiked: Boolean) = toggleBase(
        media, "favorites", setLiked,
        updateDao = { it.copy(isLiked = setLiked) }
    ) {
        if (setLiked) {
            showDao.insertShows(listOf(media.toEntity("liked")))
            runCatching { achievement.get().addXp(AchievementDefs.XP_LIKE_SHOW) }
        } else {
            showDao.deleteLikedShow(media.id)
        }
    }

    override suspend fun toggleEssential(media: MediaContent, setEssential: Boolean) = toggleBase(media, "essentials", setEssential, { it.copy(isEssential = setEssential) }) {
        if (setEssential) {
            runCatching { achievement.get().addXp(AchievementDefs.XP_LIKE_SHOW) }
        }
    }

    override suspend fun toggleWatchlist(media: MediaContent, add: Boolean) = toggleBase(
        media, "watchlist", add,
        updateDao = { it.copy(isInWatchlist = add) }
    ) {
        if (add) {
            showDao.insertShows(listOf(media.toEntity("watchlist")))
        } else {
            showDao.deleteWatchlistShow(media.id)
        }
    }
    override suspend fun toggleDislike(media: MediaContent, setDisliked: Boolean) = toggleBase(media, "disliked", setDisliked, { it.copy(isDisliked = setDisliked) })

    override suspend fun isInWatchlist(mediaId: Int) = interactionDao.getById(mediaId)?.isInWatchlist ?: false

    override suspend fun cacheInteractionState(mediaId: Int, isLiked: Boolean, isEssential: Boolean, isWatched: Boolean) {
        val current = interactionDao.getById(mediaId) ?: MediaInteractionEntity(mediaId = mediaId)
        interactionDao.upsert(current.copy(isLiked = isLiked, isEssential = isEssential, isWatched = isWatched))
    }

    override suspend fun trackMediaInteraction(mediaId: Int, genres: List<String>, keywords: List<String>, actors: List<Int>, narrativeStyles: Map<String, Float>, creators: List<Int>, interactionType: InteractionType) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        try {
            db.runTransaction { tx ->
                val profile = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
                var weight = 0f
                val liked = profile.likedMediaIds.toMutableList()
                val essential = profile.essentialMediaIds.toMutableList()
                val disliked = profile.dislikedMediaIds.toMutableList()
                val ratings = profile.ratings.toMutableMap()

                when (interactionType) {
                    is InteractionType.Like -> { weight = 5f; if (!liked.contains(mediaId)) liked.add(mediaId); disliked.remove(mediaId) }
                    is InteractionType.Essential -> { weight = 10f; if (!essential.contains(mediaId)) essential.add(mediaId); disliked.remove(mediaId) }
                    is InteractionType.Rate -> {
                        val score = interactionType.score
                        ratings[mediaId.toString()] = score.toFloat()
                        weight = ((score - (if (profile.ratings.isNotEmpty()) profile.ratings.values.average().toFloat() else 3f)) * 1.5f).coerceIn(-4f, 4f)
                    }
                    is InteractionType.Watched -> weight = 3f
                    is InteractionType.Dislike -> { weight = -8f; if (!disliked.contains(mediaId)) disliked.add(mediaId); liked.remove(mediaId); essential.remove(mediaId) }
                    else -> {}
                }

                val now = System.currentTimeMillis()
                fun updateScores(map: Map<String, Float>, dates: Map<String, Long>, keys: List<String>, mod: Float) =
                    (map.toMutableMap() to dates.toMutableMap()).also { (m, d) -> keys.forEach { k -> m[k] = (m[k] ?: 0f) + mod; d[k] = now } }

                val (gS, gD) = updateScores(profile.genreScores, profile.genreScoreDates, genres, weight)
                val (kS, kD) = updateScores(profile.preferredKeywords, profile.keywordScoreDates, keywords, weight)
                val (aS, aD) = updateScores(profile.preferredActors, profile.actorScoreDates, actors.map { it.toString() }, weight)
                val (cS, cD) = updateScores(profile.preferredCreators, profile.creatorScoreDates, creators.map { it.toString() }, weight)

                val nS = profile.narrativeStyleScores.toMutableMap()
                val nD = profile.narrativeStyleDates.toMutableMap()
                narrativeStyles.forEach { (s, r) -> nS[s] = (nS[s] ?: 0f) + weight * r; nD[s] = now }

                tx.set(doc, profile.copy(genreScores = gS, genreScoreDates = gD, preferredKeywords = kS, keywordScoreDates = kD, preferredActors = aS, actorScoreDates = aD,
                    narrativeStyleScores = nS, narrativeStyleDates = nD, preferredCreators = cS, creatorScoreDates = cD, likedMediaIds = liked, essentialMediaIds = essential, dislikedMediaIds = disliked, ratings = ratings))
            }.await()
            userRepo.get().getUserProfile()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "trackMediaInteraction failed")
        }
    }

    override suspend fun updateRating(mediaId: Int, rating: Int) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        val show = showDao.getLikedShowById(mediaId) ?: showDao.getWatchedShowById(mediaId)
        try {
            db.runTransaction { tx ->
                tx.set(doc.collection("ratings").document(mediaId.toString()), mapOf("rating" to rating))
                val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
                tx.set(doc, p.copy(ratings = p.ratings.toMutableMap().apply { put(mediaId.toString(), rating.toFloat()) }))
            }.await()
            runCatching { achievement.get().addXp(AchievementDefs.XP_RATE_SHOW) }
            userRepo.get().getUserProfile()
        } catch (e: Exception) { Timber.e(e, "updateRating failed"); throw e }
    }

    override suspend fun deleteRating(mediaId: Int) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        try {
            db.runTransaction { tx ->
                tx.delete(doc.collection("ratings").document(mediaId.toString()))
                tx.get(doc).toObject(UserProfile::class.java)?.let { p ->
                    val r = p.ratings.toMutableMap()
                    if (r.remove(mediaId.toString()) != null) tx.set(doc, p.copy(ratings = r))
                }
            }.await()
            userRepo.get().getUserProfile()
        } catch (e: Exception) { Timber.e(e, "deleteRating failed"); throw e }
    }

    override suspend fun getAllRatings(): Map<Int, Int> = withContext(ioDispatcher) {
        val s = userDoc()?.collection("ratings")?.get()?.await()
        s?.documents?.mapNotNull { d -> d.id.toIntOrNull()?.to(d.getLong("rating")?.toInt() ?: return@mapNotNull null) }?.toMap() ?: emptyMap()
    }

    override suspend fun getUserRating(mediaId: Int) = safeFirestoreCall(null) {
        userDoc()?.collection("ratings")?.document(mediaId.toString())?.get()?.await()?.getLong("rating")?.toInt()
    }

    override suspend fun saveReview(mediaId: Int, text: String) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            tx.set(doc, p.copy(mediaReviews = p.mediaReviews.toMutableMap().apply { if (text.isBlank()) remove(mediaId.toString()) else put(mediaId.toString(), text) }))
        }.await()
        userRepo.get().getUserProfile()
    }

    override suspend fun getReview(mediaId: Int) = withContext(ioDispatcher) {
        userDoc()?.get()?.await()?.toObject(UserProfile::class.java)?.mediaReviews?.get(mediaId.toString())
    }

    private suspend fun updateCustomList(list: String, action: (MutableList<Int>) -> Boolean) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            val lists = p.customLists.toMutableMap()
            val items = (lists[list] ?: emptyList()).toMutableList()
            if (action(items)) {
                lists[list] = items
                tx.set(doc, p.copy(customLists = lists))
            }
        }.await()
        userRepo.get().getUserProfile()
    }

    override suspend fun addToCustomList(list: String, mediaId: Int) = updateCustomList(list) { if (!it.contains(mediaId)) it.add(mediaId) else false }
    override suspend fun removeFromCustomList(list: String, mediaId: Int) = updateCustomList(list) { it.remove(mediaId) }

    override suspend fun createCustomList(list: String) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            if (!p.customLists.containsKey(list)) tx.set(doc, p.copy(customLists = p.customLists.toMutableMap().apply { put(list, emptyList()) }))
        }.await()
        userRepo.get().getUserProfile()
    }

    override suspend fun deleteCustomList(list: String) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            val updated = p.customLists.toMutableMap()
            if (updated.remove(list) != null) tx.set(doc, p.copy(customLists = updated))
        }.await()
        userRepo.get().getUserProfile()
    }

    override suspend fun getCustomLists() = withContext(ioDispatcher) {
        userDoc()?.get()?.await()?.toObject(UserProfile::class.java)?.customLists ?: emptyMap()
    }
    override fun getCustomListsFlow() = userRepo.get().getUserProfileFlow().map { it?.customLists ?: emptyMap() }.distinctUntilChanged()

    override suspend fun toggleEpisodeWatched(showId: Int, episodeId: Int) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext false
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            val watched = p.watchedEpisodes.toMutableMap()
            val eps = watched[showId.toString()]?.toMutableSet() ?: mutableSetOf()
            val added = if (eps.contains(episodeId)) { eps.remove(episodeId); false } else { eps.add(episodeId); true }
            tx.set(doc, p.copy(watchedEpisodes = watched.apply { put(showId.toString(), eps.toList()) }))
            added
        }.await().also { if (it) runCatching { achievement.get().addXp(AchievementDefs.XP_WATCH_EPISODE) }; userRepo.get().getUserProfile() }
    }

    override suspend fun setAllEpisodesWatched(showId: Int, episodeIds: List<Int>) = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        db.runTransaction { tx ->
            val p = tx.get(doc).toObject(UserProfile::class.java) ?: UserProfile(userId = uid ?: "")
            val watched = p.watchedEpisodes.toMutableMap()
            if (episodeIds.isEmpty()) watched.remove(showId.toString()) else watched[showId.toString()] = episodeIds
            tx.set(doc, p.copy(watchedEpisodes = watched))
        }.await()
        userRepo.get().getUserProfile()
    }

    override suspend fun getWatchedShowsWithSeasonCount() = interactionDao.getWatchedWithSeasonCount()
    override suspend fun updateLastKnownSeasons(mediaId: Int, seasons: Int) = interactionDao.updateLastKnownSeasons(mediaId, seasons)

    override suspend fun syncFavoritesAndWatchedToRoom() = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext
        try {
            val favSnapshot = doc.collection("favorites").get().await()
            val watSnapshot = doc.collection("watched").get().await()
            val wlsSnapshot = doc.collection("watchlist").get().await()

            val fav = favSnapshot.toObjects(MediaContent::class.java)
            val wat = watSnapshot.toObjects(MediaContent::class.java)
            val wls = wlsSnapshot.toObjects(MediaContent::class.java)

            val validFav = fav.filter { it.id != 0 }
            val validWat = wat.filter { it.id != 0 }
            val validWls = wls.filter { it.id != 0 }

            if (fav.isNotEmpty() && validFav.isEmpty()) return@withContext
            if (wat.isNotEmpty() && validWat.isEmpty()) return@withContext
            if (wls.isNotEmpty() && validWls.isEmpty()) return@withContext

            showDao.syncCategory("liked", validFav.map { it.toEntity("liked") })
            showDao.syncCategory("watched", validWat.map { it.toEntity("watched") })

            val pendingWatchlistIds = interactionDao.getPendingSyncInteractions()
                .filter { it.isInWatchlist }.map { it.mediaId }.toSet()

            val extraLocalEntities = if (pendingWatchlistIds.isNotEmpty()) {
                val firestoreIds = validWls.map { it.id }.toSet()
                showDao.getShowsByCategory("watchlist")
                    .filter { it.id in pendingWatchlistIds && it.id !in firestoreIds }
                    .also { localShows ->
                        localShows.forEach { entity ->
                            try {
                                doc.collection("watchlist")
                                    .document(entity.id.toString())
                                    .set(entity.toDomain()).await()
                                interactionDao.getById(entity.id)?.let { interaction ->
                                    interactionDao.upsert(interaction.copy(syncPending = false))
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Timber.w(e, "Retry watchlist write failed for ${entity.id}")
                            }
                        }
                    }
            } else emptyList()

            val effectiveWatchlist = validWls.map { it.toEntity("watchlist") } + extraLocalEntities
            showDao.syncCategory("watchlist", effectiveWatchlist)

            val fIds = validFav.map { it.id }.toSet()
            val wIds = validWat.map { it.id }.toSet()
            val wlIds = effectiveWatchlist.map { it.id }.toSet()
            val all = (fIds + wIds + wlIds).toList()

            if (all.isNotEmpty()) {
                val existing = interactionDao.getByIds(all).associateBy { it.mediaId }
                interactionDao.upsertAll(all.map { id ->
                    val e = existing[id] ?: MediaInteractionEntity(mediaId = id)
                    e.copy(
                        isLiked = id in fIds,
                        isWatched = id in wIds,
                        isInWatchlist = id in wlIds,
                        syncPending = false
                    )
                })
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "syncFavoritesAndWatchedToRoom failed")
        }
    }
}
