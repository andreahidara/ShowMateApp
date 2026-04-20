package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.util.MainDispatcherRule
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val db: FirebaseFirestore = mockk(relaxed = true)

    private val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    private val mockDefaultQuery = mockk<Query>(relaxed = true)
    private val mockDefaultQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    private val mockDefaultDocRef = mockk<DocumentReference>(relaxed = true)
    private val mockDefaultDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    private lateinit var repository: AchievementRepository

    @Before
    fun setup() {
        // Wire up default completing Firestore chain before constructing repository
        every { mockDefaultDocSnapshot.get("unlockedAchievementIds") } returns null
        every { mockDefaultDocSnapshot.getLong("xp") } returns null
        every { mockDefaultDocRef.get() } returns Tasks.forResult(mockDefaultDocSnapshot)

        every { mockDefaultQuerySnapshot.documents } returns emptyList()
        every { mockDefaultQuery.whereIn(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.get() } returns Tasks.forResult(mockDefaultQuerySnapshot)

        every { mockUsersCollection.document(any()) } returns mockDefaultDocRef
        every { mockUsersCollection.whereIn(any<String>(), any()) } returns mockDefaultQuery
        every { db.collection("users") } returns mockUsersCollection

        repository = AchievementRepository(db, auth, mainDispatcherRule.testDispatcher)
    }

    // region getUnlockedIds

    @Test
    fun `getUnlockedIds returns empty list when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getUnlockedIds()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUnlockedIds returns empty list when Firestore returns null`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocSnapshot.get("unlockedAchievementIds") } returns null

        val result = repository.getUnlockedIds()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUnlockedIds returns achievement ids from Firestore`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocSnapshot.get("unlockedAchievementIds") } returns listOf("ACH_1", "ACH_5")

        val result = repository.getUnlockedIds()
        assertEquals(listOf("ACH_1", "ACH_5"), result)
    }

    @Test
    fun `getUnlockedIds returns empty list when Firestore task fails`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocRef.get() } returns Tasks.forException(RuntimeException("Firestore error"))

        val result = repository.getUnlockedIds()
        assertTrue(result.isEmpty())
    }

    // endregion

    // region getXp

    @Test
    fun `getXp returns 0 when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        assertEquals(0, repository.getXp())
    }

    @Test
    fun `getXp returns 0 when Firestore returns null`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocSnapshot.getLong("xp") } returns null

        assertEquals(0, repository.getXp())
    }

    @Test
    fun `getXp returns value from Firestore`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocSnapshot.getLong("xp") } returns 350L

        assertEquals(350, repository.getXp())
    }

    @Test
    fun `getXp returns 0 when Firestore task fails`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultDocRef.get() } returns Tasks.forException(RuntimeException("Firestore error"))

        assertEquals(0, repository.getXp())
    }

    // endregion

    // region unlockAchievements

    @Test
    fun `unlockAchievements does nothing when achievementIds is empty and xpToAdd is 0`() = runTest {
        // Should return immediately without any Firestore calls
        repository.unlockAchievements(emptyList(), 0)
    }

    // endregion

    // region getFriendLeaderboard

    @Test
    fun `getFriendLeaderboard returns empty list when friendEmails is empty`() = runTest {
        val result = repository.getFriendLeaderboard(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFriendLeaderboard returns sorted entries from Firestore`() = runTest {
        val doc1 = mockk<DocumentSnapshot>(relaxed = true)
        val doc2 = mockk<DocumentSnapshot>(relaxed = true)
        every { doc1.getString("email") } returns "user1@test.com"
        every { doc1.getString("username") } returns "UserOne"
        every { doc1.getLong("xp") } returns 500L
        every { doc2.getString("email") } returns "user2@test.com"
        every { doc2.getString("username") } returns "UserTwo"
        every { doc2.getLong("xp") } returns 200L
        every { mockDefaultQuerySnapshot.documents } returns listOf(doc1, doc2)

        val result = repository.getFriendLeaderboard(listOf("user1@test.com", "user2@test.com"))

        assertEquals(2, result.size)
        assertEquals("UserOne", result[0].username)
        assertEquals(500, result[0].xp)
        assertEquals("UserTwo", result[1].username)
    }

    @Test
    fun `getFriendLeaderboard returns empty list when Firestore fails`() = runTest {
        every { mockDefaultQuery.get() } returns Tasks.forException(RuntimeException("error"))

        val result = repository.getFriendLeaderboard(listOf("user@test.com"))
        assertTrue(result.isEmpty())
    }

    // endregion

    // region incrementAndGetGroupMatchCount

    @Test
    fun `incrementAndGetGroupMatchCount returns 0 when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.incrementAndGetGroupMatchCount()
        assertEquals(0, result)
    }

    // endregion
}
