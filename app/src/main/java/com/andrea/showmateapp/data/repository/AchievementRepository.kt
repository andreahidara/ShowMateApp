package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.util.safeFirestoreCall
import com.andrea.showmateapp.util.safeFirestoreRun
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class AchievementRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IAchievementRepository {

    private fun userDoc() = auth.currentUser?.uid?.let { db.collection("users").document(it) }

    override suspend fun getUnlockedIds(): List<String> = withContext(ioDispatcher) {
        safeFirestoreCall(emptyList()) {
            @Suppress("UNCHECKED_CAST")
            userDoc()?.get()?.await()?.get("unlockedAchievementIds") as? List<String> ?: emptyList()
        }
    }

    override suspend fun getXp(): Int = withContext(ioDispatcher) {
        safeFirestoreCall(0) {
            userDoc()?.get()?.await()?.getLong("xp")?.toInt() ?: 0
        }
    }

    override suspend fun unlockAchievements(achievementIds: List<String>, xpToAdd: Int) = withContext(ioDispatcher) {
        if (achievementIds.isEmpty() && xpToAdd == 0) return@withContext
        safeFirestoreRun {
            val doc = userDoc() ?: return@safeFirestoreRun
            val updates = mutableMapOf<String, Any>("xp" to FieldValue.increment(xpToAdd.toLong()))
            if (achievementIds.isNotEmpty()) {
                updates["unlockedAchievementIds"] = FieldValue.arrayUnion(*achievementIds.toTypedArray<Any>())
            }
            doc.update(updates).await()
        }
    }

    override suspend fun addXp(delta: Int) = withContext(ioDispatcher) {
        safeFirestoreRun {
            userDoc()?.update("xp", FieldValue.increment(delta.toLong()))?.await()
        }
    }

    override suspend fun incrementAndGetGroupMatchCount(): Int = withContext(ioDispatcher) {
        val doc = userDoc() ?: return@withContext 0
        safeFirestoreCall(1) {
            doc.update("completedGroupMatches", FieldValue.increment(1)).await()
            doc.get().await().getLong("completedGroupMatches")?.toInt() ?: 1
        }
    }

    override suspend fun getFriendLeaderboard(
        friendEmails: List<String>
    ): List<IAchievementRepository.LeaderboardEntry> = withContext(ioDispatcher) {
        if (friendEmails.isEmpty()) return@withContext emptyList()
        safeFirestoreCall(emptyList()) {
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
                            username = username,
                            email = email,
                            xp = xp,
                            levelName = AchievementDefs.levelForXp(xp).name
                        )
                    }
            }.sortedByDescending { it.xp }
        }
    }
}
