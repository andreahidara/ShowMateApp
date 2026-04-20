package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.local.MediaInteractionDao
import com.andrea.showmateapp.data.local.ShowDao
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val db: FirebaseFirestore = mockk(relaxed = true)
    private val showDao: ShowDao = mockk(relaxed = true)
    private val mediaInteractionDao: MediaInteractionDao = mockk(relaxed = true)

    // Shared mocks set up before repository construction
    private val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    private val mockDefaultQuery = mockk<Query>(relaxed = true)
    private val mockDefaultSnapshot = mockk<QuerySnapshot>(relaxed = true)
    private val mockDefaultDocRef = mockk<DocumentReference>(relaxed = true)
    private val mockDefaultDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        // Set up default Firestore chain BEFORE constructing the repository
        // so usersCollection = db.collection("users") captures the mock
        every { mockDefaultSnapshot.documents } returns emptyList()
        every { mockDefaultQuery.get() } returns Tasks.forResult(mockDefaultSnapshot)
        every { mockDefaultQuery.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.whereNotEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.limit(any()) } returns mockDefaultQuery

        every { mockDefaultDocSnapshot.toObject(UserProfile::class.java) } returns null
        every { mockDefaultDocRef.get() } returns Tasks.forResult(mockDefaultDocSnapshot)

        every { mockUsersCollection.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockUsersCollection.whereNotEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockUsersCollection.document(any()) } returns mockDefaultDocRef

        every { db.collection("users") } returns mockUsersCollection

        repository = UserRepository(db, auth, showDao, mediaInteractionDao, mainDispatcherRule.testDispatcher)
    }

    // region getCurrentUserEmail

    @Test
    fun `getCurrentUserEmail returns null when not authenticated`() {
        every { auth.currentUser } returns null
        assertNull(repository.getCurrentUserEmail())
    }

    @Test
    fun `getCurrentUserEmail returns email when authenticated`() {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.email } returns "andrea@test.com"
        assertEquals("andrea@test.com", repository.getCurrentUserEmail())
    }

    // endregion

    // region getUserProfile

    @Test
    fun `getUserProfile returns null when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        assertNull(repository.getUserProfile())
    }

    @Test
    fun `getUserProfile returns profile from Firestore when authenticated`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"

        val expectedProfile = UserProfile(userId = "uid123", username = "Andrea")
        val mockDocRef = mockk<DocumentReference>()
        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockUsersCollection.document("uid123") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forResult(mockSnapshot)
        every { mockSnapshot.toObject(UserProfile::class.java) } returns expectedProfile

        val result = repository.getUserProfile()
        assertEquals(expectedProfile, result)
    }

    @Test
    fun `getUserProfile returns null when Firestore throws`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"

        val mockDocRef = mockk<DocumentReference>()
        every { mockUsersCollection.document("uid123") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forException(RuntimeException("Firestore unavailable"))

        val result = repository.getUserProfile()
        assertNull(result)
    }

    // endregion

    // region getUserProfileFlow

    @Test
    fun `getUserProfileFlow emits null when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getUserProfileFlow().first()
        assertNull(result)
    }

    // endregion

    // region getSimilarUsers

    @Test
    fun `getSimilarUsers returns empty list when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getSimilarUsers(limit = 10)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSimilarUsers returns empty list when Firestore returns empty`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"

        every { mockDefaultSnapshot.toObjects(UserProfile::class.java) } returns emptyList()

        val result = repository.getSimilarUsers(limit = 5)
        assertTrue(result.isEmpty())
    }

    // endregion

    // region compareWithFriend

    @Test
    fun `compareWithFriend returns empty list when both profiles have no shared media`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "myUid"

        val myDocRef = mockk<DocumentReference>()
        val mySnapshot = mockk<DocumentSnapshot>()
        val myProfile = UserProfile(userId = "myUid", likedMediaIds = listOf(1, 2))
        every { mockUsersCollection.document("myUid") } returns myDocRef
        every { myDocRef.get() } returns Tasks.forResult(mySnapshot)
        every { mySnapshot.toObject(UserProfile::class.java) } returns myProfile

        // getFriendProfile queries by email — default query returns no docs
        every { mockDefaultSnapshot.documents } returns emptyList()

        val result = repository.compareWithFriend("stranger@test.com")
        assertTrue(result.isEmpty())
    }

    // endregion

    // region userExists

    @Test
    fun `userExists returns false when Firestore returns no documents`() = runTest {
        every { mockDefaultSnapshot.documents } returns emptyList()
        val result = repository.userExists("unknown@test.com")
        assertTrue(!result)
    }

    // endregion

    // region deleteAccount

    @Test
    fun `deleteAccount returns early when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        repository.deleteAccount()
    }

    // endregion
}
