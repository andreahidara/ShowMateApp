package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.FriendRequest
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.util.safeFirestoreCall
import com.andrea.showmateapp.util.safeFirestoreRun
import com.andrea.showmateapp.util.GenreMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SocialRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISocialRepository {

    private val users = db.collection("users")
    private val friendRequests = db.collection("friendRequests")

    @Volatile private var cachedUsername: String? = null

    @Volatile private var cachedUsernameUid: String? = null

    override fun getCurrentUid(): String? = auth.currentUser?.uid

    override suspend fun searchByUsername(query: String): List<UserProfile> = withContext(ioDispatcher) {
        if (query.length < 2) return@withContext emptyList()
        val myUid = auth.currentUser?.uid
        safeFirestoreCall(emptyList()) {
            users
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(UserProfile::class.java)
                .filter { it.userId != myUid }
        }
    }

    override suspend fun sendFriendRequest(toUid: String, toUsername: String): Boolean = withContext(ioDispatcher) {
        val me = auth.currentUser ?: return@withContext false
        try {
            val (fwd, rev) = coroutineScope {
                val a = async {
                    friendRequests.whereEqualTo("fromUid", me.uid).whereEqualTo("toUid", toUid)
                        .limit(1).get().await()
                }
                val b = async {
                    friendRequests.whereEqualTo("fromUid", toUid).whereEqualTo("toUid", me.uid)
                        .limit(1).get().await()
                }
                a.await() to b.await()
            }
            if (!fwd.isEmpty || !rev.isEmpty) return@withContext false

            val myUsername = resolveUsername(me.uid)
            val docRef = friendRequests.document()
            docRef.set(
                mapOf(
                    "id" to docRef.id,
                    "fromUid" to me.uid,
                    "toUid" to toUid,
                    "fromUsername" to myUsername,
                    "toUsername" to toUsername,
                    "status" to FriendRequest.STATUS_PENDING,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            users.document(toUid).collection("notifications").add(
                mapOf(
                    "type" to "friend_request",
                    "fromUid" to me.uid,
                    "fromUsername" to myUsername,
                    "message" to "$myUsername quiere ser tu amigo en ShowMate",
                    "createdAt" to System.currentTimeMillis(),
                    "read" to false
                )
            ).await()
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override suspend fun acceptFriendRequest(requestId: String, fromUid: String) = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext
        try {
            db.runTransaction { transaction ->
                val requestRef = friendRequests.document(requestId)
                val myUserRef = users.document(myUid)

                val myDoc = transaction.get(myUserRef)

                @Suppress("UNCHECKED_CAST")
                val myFriendIds = (myDoc.get("friendIds") as? List<String> ?: emptyList()).toMutableList()
                if (!myFriendIds.contains(fromUid)) myFriendIds.add(fromUid)

                transaction.delete(requestRef)
                transaction.update(myUserRef, "friendIds", myFriendIds)
            }.await()

            users.document(fromUid)
                .update("friendIds", FieldValue.arrayUnion(myUid))
                .await()

            runCatching {
                val myUsername = resolveUsername(myUid)
                users.document(fromUid).collection("notifications").add(
                    mapOf(
                        "type" to "request_accepted",
                        "fromUid" to myUid,
                        "fromUsername" to myUsername,
                        "message" to "$myUsername aceptó tu solicitud de amistad",
                        "createdAt" to System.currentTimeMillis(),
                        "read" to false
                    )
                ).await()
            }.onFailure { Timber.w(it, "Notification after accept failed (non-critical)") }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error accepting friend request $requestId: ${e.message}")
            throw e
        }
    }

    override suspend fun rejectFriendRequest(requestId: String) = withContext(ioDispatcher) {
        safeFirestoreRun { friendRequests.document(requestId).delete().await() }
    }

    override suspend fun removeFriend(friendUid: String) = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext
        try {
            coroutineScope {
                val q1 = async {
                    friendRequests
                        .whereEqualTo("fromUid", myUid).whereEqualTo("toUid", friendUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED).get().await()
                }
                val q2 = async {
                    friendRequests
                        .whereEqualTo("fromUid", friendUid).whereEqualTo("toUid", myUid)
                        .whereEqualTo("status", FriendRequest.STATUS_ACCEPTED).get().await()
                }

                val requests = q1.await().documents + q2.await().documents

                db.runTransaction { transaction ->
                    requests.forEach { transaction.delete(it.reference) }

                    val myUserRef = users.document(myUid)
                    transaction.update(myUserRef, "friendIds", FieldValue.arrayRemove(friendUid))

                    val friendUserRef = users.document(friendUid)
                    transaction.update(friendUserRef, "friendIds", FieldValue.arrayRemove(myUid))
                }.await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error removing friend")
        }
    }

    override suspend fun getFriends(): List<FriendInfo> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        safeFirestoreCall(emptyList()) {
            val myProfile = users.document(myUid).get().await().toObject(UserProfile::class.java)
            val friendUids = myProfile?.friendIds ?: return@safeFirestoreCall emptyList()
            if (friendUids.isEmpty()) return@safeFirestoreCall emptyList()
            coroutineScope {
                friendUids.take(30).map { uid ->
                    async {
                        val p = users.document(uid).get().await().toObject(UserProfile::class.java)
                        val compat = if (myProfile != null && p != null) {
                            (GenreMapper.jaccardSimilarity(myProfile.genreScores, p.genreScores) * 100)
                                .toInt().coerceIn(0, 100)
                        } else 0
                        FriendInfo(uid = uid, username = p?.username ?: "", email = p?.email ?: "", compatibilityScore = compat)
                    }
                }.awaitAll()
            }
        }
    }

    override suspend fun getIncomingRequests(): List<FriendRequest> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        safeFirestoreCall(emptyList()) {
            friendRequests
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await().documents
                .mapNotNull { doc -> doc.toObject(FriendRequest::class.java)?.copy(id = doc.id) }
        }
    }

    override suspend fun getOutgoingRequests(): List<FriendRequest> = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext emptyList()
        safeFirestoreCall(emptyList()) {
            friendRequests
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await().documents
                .mapNotNull { doc -> doc.toObject(FriendRequest::class.java)?.copy(id = doc.id) }
        }
    }

    override suspend fun getPendingRequestCount(): Int = withContext(ioDispatcher) {
        val myUid = auth.currentUser?.uid ?: return@withContext 0
        safeFirestoreCall(0) {
            friendRequests
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get().await().size()
        }
    }

    override suspend fun getFriendProfile(friendUid: String): UserProfile? = withContext(ioDispatcher) {
        safeFirestoreCall(null) { users.document(friendUid).get().await().toObject(UserProfile::class.java) }
    }

    override suspend fun saveDeviceToken(token: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        safeFirestoreRun { users.document(uid).update("fcmToken", token).await() }
    }

    private suspend fun resolveUsername(uid: String): String {
        cachedUsername?.takeIf { uid == cachedUsernameUid }?.let { return it }
        return safeFirestoreCall(auth.currentUser?.email ?: "Usuario") {
            val profile = users.document(uid).get().await().toObject(UserProfile::class.java)
            val name = profile?.username?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: "Usuario"
            if (uid == auth.currentUser?.uid) { cachedUsername = name; cachedUsernameUid = uid }
            name
        }
    }
}
