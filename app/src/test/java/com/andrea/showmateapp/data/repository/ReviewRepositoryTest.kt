package com.andrea.showmateapp.data.repository

import com.andrea.showmateapp.data.model.ReviewPage
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val db: FirebaseFirestore = mockk(relaxed = true)

    private val mockReviewsCollection = mockk<CollectionReference>(relaxed = true)
    private val mockDefaultQuery = mockk<Query>(relaxed = true)
    private val mockDefaultSnapshot = mockk<QuerySnapshot>(relaxed = true)
    private val mockDefaultDocRef = mockk<DocumentReference>(relaxed = true)

    private lateinit var repository: ReviewRepository

    @Before
    fun setup() {
        // Configure completing Firestore chain before repository construction
        every { mockDefaultSnapshot.documents } returns emptyList()
        every { mockDefaultSnapshot.size() } returns 0

        every { mockDefaultQuery.get() } returns Tasks.forResult(mockDefaultSnapshot)
        every { mockDefaultQuery.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.whereIn(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.orderBy(any<String>(), any()) } returns mockDefaultQuery
        every { mockDefaultQuery.limit(any()) } returns mockDefaultQuery
        every { mockDefaultQuery.startAfter(any<DocumentSnapshot>()) } returns mockDefaultQuery

        every { mockDefaultDocRef.get() } returns Tasks.forResult(mockk<DocumentSnapshot>(relaxed = true))
        every { mockDefaultDocRef.delete() } returns Tasks.forResult(null)
        every { mockDefaultDocRef.update(any<String>(), any()) } returns Tasks.forResult(null)
        every { mockDefaultDocRef.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

        every { mockReviewsCollection.whereEqualTo(any<String>(), any()) } returns mockDefaultQuery
        every { mockReviewsCollection.whereIn(any<String>(), any()) } returns mockDefaultQuery
        every { mockReviewsCollection.orderBy(any<String>(), any()) } returns mockDefaultQuery
        every { mockReviewsCollection.document(any()) } returns mockDefaultDocRef
        every { mockReviewsCollection.document() } returns mockDefaultDocRef

        every { db.collection("reviews") } returns mockReviewsCollection

        repository = ReviewRepository(db, auth, mainDispatcherRule.testDispatcher)
    }

    // region getFriendReviews

    @Test
    fun `getFriendReviews returns empty list when friendEmails is empty`() = runTest {
        val result = repository.getFriendReviews(mediaId = 1, seasonNumber = 1, friendEmails = emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFriendReviews returns empty list when Firestore returns no docs`() = runTest {
        every { mockDefaultSnapshot.toObjects(com.andrea.showmateapp.data.model.Review::class.java) } returns emptyList()
        val result = repository.getFriendReviews(
            mediaId = 42,
            seasonNumber = 1,
            friendEmails = listOf("friend@test.com")
        )
        assertTrue(result.isEmpty())
    }

    // endregion

    // region getMyReview

    @Test
    fun `getMyReview returns null when user not authenticated`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getMyReview(mediaId = 1, seasonNumber = 1)
        assertNull(result)
    }

    @Test
    fun `getMyReview returns null when Firestore returns empty`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid123"
        every { mockDefaultSnapshot.documents } returns emptyList()

        val result = repository.getMyReview(mediaId = 42, seasonNumber = 1)
        assertNull(result)
    }

    // endregion

    // region getPublicReviews

    @Test
    fun `getPublicReviews returns empty page when Firestore returns no docs`() = runTest {
        every { mockDefaultSnapshot.documents } returns emptyList()

        val result = repository.getPublicReviews(mediaId = 1, seasonNumber = 1, pageSize = 10)
        assertTrue(result.reviews.isEmpty())
        assertTrue(result == ReviewPage())
    }

    // endregion

    // region deleteReview

    @Test
    fun `deleteReview completes without error when Firestore succeeds`() = runTest {
        every { mockDefaultDocRef.delete() } returns Tasks.forResult(null)
        repository.deleteReview("reviewId")
    }

    @Test
    fun `deleteReview completes without error when Firestore fails`() = runTest {
        every { mockDefaultDocRef.delete() } returns Tasks.forException(RuntimeException("Network error"))
        repository.deleteReview("reviewId")
    }

    // endregion

    // region reportReview

    @Test
    fun `reportReview completes without error`() = runTest {
        repository.reportReview("reviewId")
    }

    // endregion
}
