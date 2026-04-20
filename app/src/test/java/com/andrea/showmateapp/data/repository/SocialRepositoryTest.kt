package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.UserProfile
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocialRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val db: FirebaseFirestore = mockk(relaxed = true)

    // Shared mocks configured before repository construction
    private val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    private val mockFriendRequestsCollection = mockk<CollectionReference>(relaxed = true)
    private val mockDefaultQuery = mockk<Query>(relaxed = true)
    private val mockDefaultSnapshot = mockk<QuerySnapshot>(relaxed = true)
    private val mockDefaultDocRef = mockk<DocumentReference>(relaxed = true)
    private val mockDefaultDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    private lateinit var repository: SocialRepository

    @Before
    fun setup() {
        // Configure default completing Firestore chain before repository construction
        every { mockDefaultSnapshot.documents } returns emptyList()
        every { mockDefaultSnapshot.isEmpty } returns true
        every { mockDefaultSnapshot.size() } returns 0
        every { mockDefaultSnapshot.toObjects(UserProfile::class.java) } returns emptyList()

        every { mockDefaultQuery.get() } returns Tasks.forResult(mockDefaultSnapshot)
        every { mockDefaultQuery.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.whereGreaterThanOrEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.whereLessThanOrEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.limit(any()) } returns mockDefaultQuery
        every { mockDefaultQuery.orderBy(any<String>(), any()) } returns mockDefaultQuery

        every { mockDefaultDocSnapshot.toObject(UserProfile::class.java) } returns null
        every { mockDefaultDocRef.get() } returns Tasks.forResult(mockDefaultDocSnapshot)

        every { mockUsersCollection.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockUsersCollection.whereGreaterThanOrEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockUsersCollection.document(any()) } returns mockDefaultDocRef

        every { mockFriendRequestsCollection.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockFriendRequestsCollection.document() } returns mockDefaultDocRef
        every { mockFriendRequestsCollection.document(any()) } returns mockDefaultDocRef

        every { db.collection("users") } returns mockUsersCollection
        every { db.collection("friendRequests") } returns mockFriendRequestsCollection

        repository = SocialRepository(db, auth, mainDispatcherRule.testDispatcher)
    }

    // region getCurrentUid

    @Test
    fun `getCurrentUid returns null when no user logged in`() {
        every { auth.currentUser } returns null
        assertNull(repository.getCurrentUid())
    }

    @Test
    fun `getCurrentUid returns uid when user is logged in`() {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user123"
        assertEquals("user123", repository.getCurrentUid())
    }

    // endregion

    // region searchByUsername

    @Test
    fun `searchByUsername returns empty list when query is 1 char`() = runTest {
        val result = repository.searchByUsername("a")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchByUsername returns empty list when query is empty`() = runTest {
        val result = repository.searchByUsername("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchByUsername filters out current user from results`() = runTest {
        val myUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns myUser
        every { myUser.uid } returns "myUid"

        val otherProfile = UserProfile(userId = "otherUid", username = "abcUser")
        val selfProfile = UserProfile(userId = "myUid", username = "abMe")
        every { mockDefaultSnapshot.toObjects(UserProfile::class.java) } returns listOf(otherProfile, selfProfile)

        val result = repository.searchByUsername("ab")

        assertEquals(1, result.size)
        assertEquals("otherUid", result[0].userId)
    }

    @Test
    fun `searchByUsername returns all results when not logged in`() = runTest {
        every { auth.currentUser } returns null

        val profile = UserProfile(userId = "someUid", username = "abcUser")
        every { mockDefaultSnapshot.toObjects(UserProfile::class.java) } returns listOf(profile)

        val result = repository.searchByUsername("ab")
        assertEquals(1, result.size)
    }

    // endregion

    // region sendFriendRequest

    @Test
    fun `sendFriendRequest returns false when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.sendFriendRequest("toUid", "toUsername")
        assertFalse(result)
    }

    @Test
    fun `sendFriendRequest returns false when request already exists`() = runTest {
        val myUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns myUser
        every { myUser.uid } returns "myUid"
        every { myUser.email } returns "me@test.com"

        val existingSnapshot = mockk<QuerySnapshot>()
        every { existingSnapshot.isEmpty } returns false
        every { mockDefaultQuery.get() } returns Tasks.forResult(existingSnapshot)

        val result = repository.sendFriendRequest("toUid", "toUser")
        assertFalse(result)
    }

    // endregion

    // region acceptFriendRequest

    @Test
    fun `acceptFriendRequest returns early when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        repository.acceptFriendRequest("requestId", "fromUid")
    }

    // endregion

    // region removeFriend

    @Test
    fun `removeFriend returns early when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        repository.removeFriend("friendUid")
    }

    // endregion

    // region getFriends

    @Test
    fun `getFriends returns empty list when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getFriends()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFriends returns empty list when user has no friends`() = runTest {
        val myUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns myUser
        every { myUser.uid } returns "myUid"

        val myProfile = UserProfile(userId = "myUid", friendIds = emptyList())
        every { mockDefaultDocSnapshot.toObject(UserProfile::class.java) } returns myProfile

        val result = repository.getFriends()
        assertTrue(result.isEmpty())
    }

    // endregion

    // region getIncomingRequests

    @Test
    fun `getIncomingRequests returns empty list when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getIncomingRequests()
        assertTrue(result.isEmpty())
    }

    // endregion

    // region getOutgoingRequests

    @Test
    fun `getOutgoingRequests returns empty list when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getOutgoingRequests()
        assertTrue(result.isEmpty())
    }

    // endregion

    // region getPendingRequestCount

    @Test
    fun `getPendingRequestCount returns 0 when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getPendingRequestCount()
        assertEquals(0, result)
    }

    // endregion

    // region saveDeviceToken

    @Test
    fun `saveDeviceToken returns early when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        repository.saveDeviceToken("token123")
    }

    // endregion

    // region getFriendProfile

    @Test
    fun `getFriendProfile returns null when Firestore returns no object`() = runTest {
        every { mockDefaultDocSnapshot.toObject(UserProfile::class.java) } returns null
        val result = repository.getFriendProfile("friendUid")
        assertNull(result)
    }

    @Test
    fun `getFriendProfile returns profile when Firestore returns data`() = runTest {
        val expectedProfile = UserProfile(userId = "friendUid", username = "FriendUser")
        every { mockDefaultDocSnapshot.toObject(UserProfile::class.java) } returns expectedProfile
        val result = repository.getFriendProfile("friendUid")
        assertEquals(expectedProfile, result)
    }

    // endregion
}
