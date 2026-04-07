package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IAchievementRepository {

    private fun userDoc() = auth.currentUser?.uid?.let { db.collection("users").document(it) }

    override suspend fun getUnlockedIds(): List<String> = withContext(ioDispatcher) {
        try {
            @Suppress("UNCHECKED_CAST")
            userDoc()?.get()?.await()?.get("unlockedAchievementIds") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getXp(): Int = withContext(ioDispatcher) {
        try {
            userDoc()?.get()?.await()?.getLong("xp")?.toInt() ?: 0
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            0
        }
    }

    override suspend fun unlockAchievements(achievementIds: List<String>, xpToAdd: Int) =
        withContext(ioDispatcher) {
            if (achievementIds.isEmpty() && xpToAdd == 0) return@withContext
            try {
                val doc = userDoc() ?: return@withContext
                val updates = mutableMapOf<String, Any>(
                    "xp" to FieldValue.increment(xpToAdd.toLong())
                )
                if (achievementIds.isNotEmpty()) {
                    updates["unlockedAchievementIds"] =
                        FieldValue.arrayUnion(*achievementIds.toTypedArray<Any>())
                }
                doc.update(updates).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            Unit
        }

    override suspend fun addXp(delta: Int) = withContext(ioDispatcher) {
        try {
            userDoc()?.update("xp", FieldValue.increment(delta.toLong()))?.await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
        Unit
    }

    override suspend fun incrementAndGetGroupMatchCount(): Int = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext 0
        try {
            doc.update("completedGroupMatches", FieldValue.increment(1)).await()
            doc.get().await().getLong("completedGroupMatches")?.toInt() ?: 1
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            1
        }
    }

    override suspend fun getFriendLeaderboard(
        friendEmails: List<String>
    ): List<IAchievementRepository.LeaderboardEntry> = withContext(ioDispatcher) {
        if (friendEmails.isEmpty()) return@withContext emptyList()
        try {
            friendEmails.distinct().chunked(30).flatMap { chunk ->
                db.collection("users")
                    .whereIn("email", chunk)
                    .get().await()
                    .documents.mapNotNull { doc ->
                        val email = doc.getString("email") ?: return@mapNotNull null
                        val username = doc.getString("username")?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@")
                        val xp = doc.getLong("xp")?.toInt() ?: 0
                        IAchievementRepository.LeaderboardEntry(
                            username  = username,
                            email     = email,
                            xp        = xp,
                            levelName = AchievementDefs.levelForXp(xp).name
                        )
                    }
            }.sortedByDescending { it.xp }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }
}
