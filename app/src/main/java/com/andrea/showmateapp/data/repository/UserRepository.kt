package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.ShowDao
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val showDao: ShowDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IUserRepository {
    private val usersCollection = db.collection("users")
    private fun userDoc(uid: String) = usersCollection.document(uid)

    private val cacheMutex = Mutex()
    private var _profileCache: UserProfile? = null
    private var _profileCacheTime: Long = 0L
    private var _profileCacheFetched: Boolean = false
    private companion object { private const val PROFILE_CACHE_TTL = 120_000L }

    private suspend fun invalidateProfileCache() = cacheMutex.withLock {
        _profileCache = null
        _profileCacheFetched = false
    }

    override suspend fun getUserProfile(): UserProfile? = withContext(ioDispatcher) {
        cacheMutex.withLock {
            if (_profileCacheFetched && System.currentTimeMillis() - _profileCacheTime < PROFILE_CACHE_TTL) {
                return@withLock _profileCache
            }
            val uid = auth.currentUser?.uid ?: return@withLock null
            try {
                val snapshot = userDoc(uid).get().await()
                snapshot.toObject(UserProfile::class.java).also {
                    _profileCache = it
                    _profileCacheTime = System.currentTimeMillis()
                    _profileCacheFetched = true
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        }
    }

    override fun getCurrentUserEmail(): String? = auth.currentUser?.email

    override suspend fun initUserProfile(username: String) = withContext(ioDispatcher) {
        val uid   = auth.currentUser?.uid ?: return@withContext
        val email = auth.currentUser?.email ?: ""
        userDoc(uid)
            .set(
                mapOf("userId" to uid, "username" to username, "email" to email),
                SetOptions.merge()
            ).await()
    }

    override suspend fun saveOnboardingInterests(
        genres: List<String>,
        watchedShowIds: List<Int>,
        lovedShowIds: List<Int>,
        preferShortEpisodes: Boolean?,
        preferFinishedShows: Boolean?,
        preferDubbed: Boolean?
    ) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)

            val newGenreScores = profile.genreScores.toMutableMap()
            genres.forEach { id -> newGenreScores[id] = (newGenreScores[id] ?: 0f) + 15f }

            val newLiked = (profile.likedMediaIds + watchedShowIds).distinct()
            val newEssential = (profile.essentialMediaIds + lovedShowIds).distinct()

            val newRatings = profile.ratings.toMutableMap()
            watchedShowIds.forEach { id -> newRatings[id.toString()] = newRatings[id.toString()] ?: 3.5f }
            lovedShowIds.forEach { id -> newRatings[id.toString()] = 4.5f }

            transaction.set(
                userRef,
                profile.copy(
                    genreScores = newGenreScores,
                    likedMediaIds = newLiked,
                    essentialMediaIds = newEssential,
                    ratings = newRatings,
                    preferShortEpisodes = preferShortEpisodes,
                    preferFinishedShows = preferFinishedShows,
                    preferDubbed = preferDubbed
                )
            )
        }.await()
        invalidateProfileCache()
    }

    override suspend fun updateProfile(username: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        userDoc(uid)
            .set(mapOf("username" to username), SetOptions.merge())
            .await()
        invalidateProfileCache()
    }

    override suspend fun getSimilarUsers(limit: Long): List<UserProfile> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = usersCollection
                .whereNotEqualTo("userId", uid)
                .limit(limit)
                .get()
                .await()
            snapshot.toObjects(UserProfile::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun userExists(email: String): Boolean = withContext(ioDispatcher) {
        try {
            val snapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override suspend fun getFriendProfile(friendEmail: String): UserProfile? = withContext(ioDispatcher) {
        try {
            val snapshot = usersCollection
                .whereEqualTo("email", friendEmail)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun compareWithFriend(friendEmail: String): List<Int> = withContext(ioDispatcher) {
        try {
            val (myProfile, friendProfile) = coroutineScope {
                val mine   = async { getUserProfile() }
                val friend = async { getFriendProfile(friendEmail) }
                mine.await() to friend.await()
            }
            if (myProfile == null || friendProfile == null) return@withContext emptyList()
            val myLiked     = myProfile.likedMediaIds.toSet()
            val friendLiked = friendProfile.likedMediaIds.toSet()
            (myLiked intersect friendLiked).toList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun recordViewingSession(showId: Int, episodeCount: Int) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)
        val today   = java.time.LocalDate.now().toString()
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val profile = snapshot.toObject(UserProfile::class.java)
                ?: UserProfile(userId = uid)
            val history = profile.viewingHistory.toMutableList()

            val entryPrefix = "$today:$showId:"
            val existingIdx = history.indexOfFirst { it.startsWith(entryPrefix) }

            if (existingIdx >= 0) {
                val parts = history[existingIdx].split(":")
                val prevCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                history[existingIdx] = "$today:$showId:${prevCount + episodeCount}"
            } else {
                history.add("$today:$showId:$episodeCount")
            }

            transaction.update(userRef, "viewingHistory", history)
            null
        }.await()
        invalidateProfileCache()
    }

    override suspend fun resetAlgorithmData() = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val userRef = userDoc(uid)

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

    override suspend fun clearUserCache() = withContext(ioDispatcher) {
        showDao.clearUserData()
        invalidateProfileCache()
    }

    override suspend fun updateProfilePhoto(url: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        userDoc(uid).set(mapOf("photoUrl" to url), SetOptions.merge()).await()
        invalidateProfileCache()
    }

    override suspend fun deleteAccount() = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            userDoc(uid).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error deleting Firestore document")
        }
        showDao.clearUserData()
        invalidateProfileCache()
        auth.currentUser?.delete()?.await()
    }
}
