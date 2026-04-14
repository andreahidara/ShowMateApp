package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.NowWatching
import com.andrea.showmateapp.data.model.SharedList
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.ISharedListRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SharedListRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISharedListRepository {

    private val sharedLists = db.collection("sharedLists")
    private val nowWatching = db.collection("nowWatching")

    private val currentUid get() = auth.currentUser?.uid

    override fun observeMySharedLists(): Flow<List<SharedList>> = callbackFlow {
        val uid = currentUid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = sharedLists
            .whereArrayContains("memberUids", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing shared lists")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lists = snapshot?.toObjects(SharedList::class.java) ?: emptyList()
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override suspend fun createSharedList(
        name: String,
        memberUids: List<String>,
        memberUsernames: List<String>
    ): String? = withContext(ioDispatcher) {
        val uid = currentUid ?: return@withContext null
        val profile = try {
            db.collection("users").document(uid).get().await()
        } catch (e: Exception) {
            null
        }
        val ownerUsername = profile?.getString("username") ?: ""
        val listId = UUID.randomUUID().toString()
        val allUids = (memberUids + uid).distinct()
        val allUsernames = (memberUsernames + ownerUsername).distinct()
        val now = System.currentTimeMillis()
        val data = SharedList(
            listId = listId,
            listName = name,
            ownerUid = uid,
            ownerUsername = ownerUsername,
            memberUids = allUids,
            memberUsernames = allUsernames,
            showIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        try {
            sharedLists.document(listId).set(data).await()
            listId
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error creating shared list")
            null
        }
    }

    override suspend fun addShowToSharedList(listId: String, showId: Int): Unit = withContext(ioDispatcher) {
        try {
            sharedLists.document(listId).update(
                "showIds",
                FieldValue.arrayUnion(showId),
                "updatedAt",
                System.currentTimeMillis()
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error adding show to shared list")
        }
    }

    override suspend fun removeShowFromSharedList(listId: String, showId: Int): Unit = withContext(ioDispatcher) {
        try {
            sharedLists.document(listId).update(
                "showIds",
                FieldValue.arrayRemove(showId),
                "updatedAt",
                System.currentTimeMillis()
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error removing show from shared list")
        }
    }

    override suspend fun deleteSharedList(listId: String): Unit = withContext(ioDispatcher) {
        try {
            sharedLists.document(listId).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error deleting shared list")
        }
    }

    override suspend fun setNowWatching(showId: Int, showName: String, posterPath: String?) = withContext(ioDispatcher) {
        val uid = currentUid ?: return@withContext
        val profile = try {
            db.collection("users").document(uid).get().await()
        } catch (e: Exception) {
            null
        }
        val username = profile?.getString("username") ?: ""
        val data = NowWatching(
            uid = uid,
            username = username,
            showId = showId,
            showName = showName,
            posterPath = posterPath,
            updatedAt = System.currentTimeMillis()
        )
        try {
            nowWatching.document(uid).set(data).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error setting now watching")
        }
    }

    override suspend fun clearNowWatching() = withContext(ioDispatcher) {
        val uid = currentUid ?: return@withContext
        try {
            nowWatching.document(uid).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    override suspend fun getFriendsNowWatching(friendUids: List<String>): List<NowWatching> = withContext(
        ioDispatcher
    ) {
        if (friendUids.isEmpty()) return@withContext emptyList()
        val cutoff = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        try {
            val batches = friendUids.chunked(10)
            batches.flatMap { batch ->
                nowWatching
                    .whereIn("uid", batch)
                    .whereGreaterThan("updatedAt", cutoff)
                    .get()
                    .await()
                    .toObjects(NowWatching::class.java)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }
}
