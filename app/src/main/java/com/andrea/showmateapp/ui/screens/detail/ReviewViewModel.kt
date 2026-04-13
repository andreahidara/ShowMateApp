package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.Review
import com.andrea.showmateapp.data.model.ReviewPage
import com.andrea.showmateapp.domain.repository.IReviewRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementChecker
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.util.OffensiveWordFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ReviewUiState(
    val myReview: Review? = null,
    val friendReviews: List<Review> = emptyList(),
    val publicReviews: List<Review> = emptyList(),
    val selectedSeason: Int = 0,
    val availableSeasons: List<Int> = emptyList(),
    val showWriteSheet: Boolean = false,
    val writeText: String = "",
    val writeRating: Int = 0,
    val writeSpoiler: Boolean = false,
    val isSaving: Boolean = false,
    val offensiveWarning: Boolean = false,
    val isLoadingPublic: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePublic: Boolean = false,
    val lastCursorId: String? = null,
    val myUserId: String? = null,
    val reportedReviewIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val isInitialLoading: Boolean = true
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: IReviewRepository,
    private val userRepository: IUserRepository,
    private val socialRepository: ISocialRepository,
    private val achievementChecker: AchievementChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var mediaId: Int = 0

    fun loadReviews(mediaId: Int, numberOfSeasons: Int) {
        if (this.mediaId == mediaId && !_uiState.value.isInitialLoading) return
        this.mediaId = mediaId

        val seasons = (0..numberOfSeasons).toList()
        _uiState.update { it.copy(availableSeasons = seasons, isInitialLoading = true) }

        viewModelScope.launch {
            val myUid = socialRepository.getCurrentUid()
            _uiState.update { it.copy(myUserId = myUid) }

            val friendEmails = try {
                socialRepository.getFriends().map { it.email }.filter { it.isNotBlank() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }

            refreshAllForSeason(season = 0, friendEmails = friendEmails)
            _uiState.update { it.copy(isInitialLoading = false) }
        }
    }

    fun selectSeason(season: Int) {
        if (season == _uiState.value.selectedSeason) return
        _uiState.update {
            it.copy(
                selectedSeason = season,
                publicReviews = emptyList(),
                friendReviews = emptyList(),
                lastCursorId = null,
                hasMorePublic = false,
                isLoadingPublic = true
            )
        }
        viewModelScope.launch {
            val friendEmails = try {
                socialRepository.getFriends().map { it.email }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
            refreshAllForSeason(season, friendEmails)
        }
    }

    private suspend fun refreshAllForSeason(season: Int, friendEmails: List<String>) {
        _uiState.update { it.copy(isLoadingPublic = true) }
        try {
            coroutineScope {
                val myDef = async { loadMyReview(season) }
                val friendDef = async { loadFriendReviews(season, friendEmails) }
                val publicDef = async { loadFirstPagePublic(season) }
                myDef.await()
                friendDef.await()
                publicDef.await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        } finally {
            _uiState.update { it.copy(isLoadingPublic = false) }
        }
    }

    private suspend fun loadMyReview(season: Int) {
        val review = runCatching { reviewRepository.getMyReview(mediaId, season) }.getOrNull()
        _uiState.update {
            it.copy(
                myReview = review,
                writeText = review?.text ?: "",
                writeRating = review?.rating ?: 0,
                writeSpoiler = review?.hasSpoiler ?: false
            )
        }
    }

    private suspend fun loadFriendReviews(season: Int, friendEmails: List<String>) {
        if (friendEmails.isEmpty()) return
        val list = runCatching {
            reviewRepository.getFriendReviews(mediaId, season, friendEmails)
        }.getOrDefault(emptyList())
        _uiState.update { it.copy(friendReviews = list) }
    }

    private suspend fun loadFirstPagePublic(season: Int) {
        val page = runCatching {
            reviewRepository.getPublicReviews(mediaId, season, pageSize = 10)
        }.getOrDefault(ReviewPage())
        _uiState.update {
            it.copy(
                publicReviews = page.reviews,
                hasMorePublic = page.hasMore,
                lastCursorId = page.lastCursorId
            )
        }
    }

    fun loadMorePublic() {
        val cursorId = _uiState.value.lastCursorId ?: return
        if (_uiState.value.isLoadingMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val page = runCatching {
                reviewRepository.getPublicReviews(
                    mediaId = mediaId,
                    seasonNumber = _uiState.value.selectedSeason,
                    pageSize = 10,
                    cursorId = cursorId
                )
            }.getOrDefault(ReviewPage())

            _uiState.update {
                it.copy(
                    publicReviews = it.publicReviews + page.reviews,
                    hasMorePublic = page.hasMore,
                    lastCursorId = page.lastCursorId,
                    isLoadingMore = false
                )
            }
        }
    }

    fun openWriteSheet() {
        val existing = _uiState.value.myReview
        _uiState.update {
            it.copy(
                showWriteSheet = true,
                writeText = existing?.text ?: it.writeText,
                writeRating = existing?.rating ?: it.writeRating,
                writeSpoiler = existing?.hasSpoiler ?: it.writeSpoiler,
                offensiveWarning = false
            )
        }
    }

    fun closeWriteSheet() = _uiState.update {
        it.copy(showWriteSheet = false, offensiveWarning = false)
    }

    fun onWriteTextChange(text: String) = _uiState.update {
        it.copy(writeText = text, offensiveWarning = false)
    }

    fun onWriteRatingChange(rating: Int) = _uiState.update { it.copy(writeRating = rating) }
    fun onSpoilerToggle() = _uiState.update { it.copy(writeSpoiler = !it.writeSpoiler) }

    fun submitReview() {
        val state = _uiState.value
        val text = state.writeText.trim()

        if (OffensiveWordFilter.containsOffensiveContent(text)) {
            _uiState.update { it.copy(offensiveWarning = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val existing = state.myReview
                if (existing != null) {
                    reviewRepository.updateReview(
                        reviewId = existing.id,
                        rating = state.writeRating,
                        text = text,
                        hasSpoiler = state.writeSpoiler,
                        seasonNumber = state.selectedSeason
                    )
                } else {
                    val profile = userRepository.getUserProfile()
                    val email = userRepository.getCurrentUserEmail() ?: ""
                    val uid = state.myUserId ?: return@launch
                    reviewRepository.submitReview(
                        Review(
                            mediaId = mediaId,
                            seasonNumber = state.selectedSeason,
                            userId = uid,
                            userEmail = email,
                            username = profile?.username?.ifBlank { email.substringBefore("@") }
                                ?: email.substringBefore("@"),
                            rating = state.writeRating,
                            text = text,
                            hasSpoiler = state.writeSpoiler,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                loadMyReview(state.selectedSeason)
                loadFirstPagePublic(state.selectedSeason)
                _uiState.update { it.copy(showWriteSheet = false) }
                if (state.myReview == null) {
                    achievementChecker.addXp(AchievementDefs.XP_WRITE_REVIEW)
                    val profile = runCatching { userRepository.getUserProfile() }.getOrNull()
                    runCatching {
                        reviewRepository.getMyReview(mediaId, 0)
                        null
                    }.getOrNull()
                    if (profile != null) {
                        achievementChecker.evaluate(
                            AchievementChecker.EvalContext(profile = profile)
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(errorMessage = "No se pudo guardar la reseña") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteMyReview() {
        val id = _uiState.value.myReview?.id ?: return
        viewModelScope.launch {
            runCatching { reviewRepository.deleteReview(id) }
            _uiState.update { it.copy(myReview = null, writeText = "", writeRating = 0, writeSpoiler = false) }
            loadFirstPagePublic(_uiState.value.selectedSeason)
        }
    }

    fun toggleLike(reviewId: String) {
        val uid = _uiState.value.myUserId ?: return
        val deltaSign = if (uid in (_uiState.value.publicReviews + _uiState.value.friendReviews)
                .firstOrNull { it.id == reviewId }?.likedByIds.orEmpty()
        ) {
            -1
        } else {
            1
        }

        fun List<Review>.withToggled() = map { r ->
            if (r.id != reviewId) {
                r
            } else {
                val liked = r.likedByIds.toMutableList()
                if (uid in liked) liked.remove(uid) else liked.add(uid)
                r.copy(likedByIds = liked, likeCount = (r.likeCount + deltaSign).coerceAtLeast(0))
            }
        }
        _uiState.update {
            it.copy(
                publicReviews = it.publicReviews.withToggled(),
                friendReviews = it.friendReviews.withToggled()
            )
        }

        viewModelScope.launch { runCatching { reviewRepository.toggleLike(reviewId) } }
    }

    fun reportReview(reviewId: String) {
        if (reviewId in _uiState.value.reportedReviewIds) return
        _uiState.update { it.copy(reportedReviewIds = it.reportedReviewIds + reviewId) }
        viewModelScope.launch { runCatching { reviewRepository.reportReview(reviewId) } }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
