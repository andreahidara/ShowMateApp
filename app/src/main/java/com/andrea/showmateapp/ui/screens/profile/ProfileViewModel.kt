package com.andrea.showmateapp.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.domain.usecase.GetProfileStatsUseCase
import com.andrea.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

data class WatchedShowItem(
    val show: MediaContent,
    val episodesWatched: Int
)

data class UserLevel(
    val label: String,
    val progress: Float,
    val nextLabel: String?
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val getViewerPersonalityUseCase: GetViewerPersonalityUseCase,
    private val authRepository: AuthRepository,
    private val achievementRepository: IAchievementRepository,
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        val AVATAR_PALETTE = listOf(
            0xFF7C4DFF.toInt(),
            0xFFE91E63.toInt(),
            0xFF00BCD4.toInt(),
            0xFF4CAF50.toInt(),
            0xFFFF5722.toInt(),
            0xFF2196F3.toInt(),
            0xFFFFB300.toInt(),
            0xFF795548.toInt()
        )
        private const val PREFS_NAME = "profile_prefs"
        private const val KEY_AVATAR_COLOR = "avatar_color"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _avatarColorInt = MutableStateFlow(prefs.getInt(KEY_AVATAR_COLOR, AVATAR_PALETTE[0]))
    val avatarColorInt: StateFlow<Int> = _avatarColorInt.asStateFlow()

    fun updateAvatarColor(colorInt: Int) {
        _avatarColorInt.value = colorInt
        prefs.edit().putInt(KEY_AVATAR_COLOR, colorInt).apply()
    }

    private val _displayName = MutableStateFlow("Usuario")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _achievementProgress = MutableStateFlow(0 to AchievementDefs.all.size)

    /** (unlockedCount, totalCount) */
    val achievementProgress: StateFlow<Pair<Int, Int>> = _achievementProgress.asStateFlow()

    val userLevel: StateFlow<UserLevel> = _xp.map { xp ->
        val lvl = AchievementDefs.levelForXp(xp)
        val prog = AchievementDefs.progressInLevel(xp)
        val next = AchievementDefs.levels.getOrNull(lvl.level)?.name
        UserLevel(lvl.name, prog, next)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserLevel("Rookie", 0f, "Espectador"))

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewerPersonality = MutableStateFlow<GetViewerPersonalityUseCase.PersonalityProfile?>(null)
    val viewerPersonality: StateFlow<GetViewerPersonalityUseCase.PersonalityProfile?> = _viewerPersonality.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @Suppress("ktlint:standard:property-naming")
    private val _watchedEpisodesMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())

    private val _customLists = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val customLists: StateFlow<Map<String, List<Int>>> = _customLists.asStateFlow()

    private val _watchedRatings = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val watchedRatings: StateFlow<Map<Int, Int>> = _watchedRatings.asStateFlow()

    val watchedShows: StateFlow<List<WatchedShowItem>> =
        combine(interactionRepository.getWatchedShowsFlow(), _watchedEpisodesMap) { entities, episodesMap ->
            entities.map { entity ->
                WatchedShowItem(
                    show = entity.toDomain(),
                    episodesWatched = episodesMap[entity.id.toString()]?.size ?: 0
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val likedShows: StateFlow<List<MediaContent>> =
        interactionRepository.getLikedShowsFlow().map { entities -> entities.map { it.toDomain() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    @Suppress("ktlint:standard:property-naming")
    private val _userProfileFlow = MutableStateFlow<UserProfile?>(null)

    val friendCount: StateFlow<Int> = _userProfileFlow.map {
        it?.friendIds?.size ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val stats: StateFlow<GetProfileStatsUseCase.ProfileStats> =
        combine(watchedShows, _userProfileFlow) { items, profile ->
            getProfileStatsUseCase.execute(items.map { it.show }, profile)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GetProfileStatsUseCase.ProfileStats(totalHours = 0, watchedCount = 0)
        )

    init {
        loadProfileData()
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            userRepository.getUserProfileFlow().collect { userProfile ->
                if (userProfile != null) {
                    _displayName.value = userProfile.username.takeIf { it.isNotBlank() }
                        ?: userRepository.getCurrentUserEmail()?.substringBefore("@")?.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        } ?: "Usuario"
                    _photoUrl.value = userProfile.photoUrl
                    _watchedEpisodesMap.value = userProfile.watchedEpisodes
                    _customLists.value = userProfile.customLists
                    _viewerPersonality.value = getViewerPersonalityUseCase.execute(userProfile)
                    _userProfileFlow.value = userProfile
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                interactionRepository.syncFavoritesAndWatchedToRoom()
                val email = userRepository.getCurrentUserEmail()
                val userProfile = userRepository.getUserProfile()
                _displayName.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                    } ?: "Usuario"
                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = interactionRepository.getAllRatings()
                _viewerPersonality.value = userProfile?.let { getViewerPersonalityUseCase.execute(it) }
                _userProfileFlow.value = userProfile
                _xp.value = runCatching { achievementRepository.getXp() }.getOrDefault(0)
                val unlockedIds = runCatching { achievementRepository.getUnlockedIds() }.getOrDefault(emptyList())
                _achievementProgress.value = unlockedIds.size to AchievementDefs.all.size
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error refreshing profile")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val email = userRepository.getCurrentUserEmail()
                val (userProfile, ratingsMap) = coroutineScope {
                    val syncJob = async { interactionRepository.syncFavoritesAndWatchedToRoom() }
                    val profileJob = async { userRepository.getUserProfile() }
                    val ratingsJob = async { interactionRepository.getAllRatings() }
                    syncJob.await()
                    Pair(profileJob.await(), ratingsJob.await())
                }

                _displayName.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    } ?: "Usuario"
                _photoUrl.value = userProfile?.photoUrl

                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = ratingsMap

                _viewerPersonality.value = userProfile?.let { getViewerPersonalityUseCase.execute(it) }
                _userProfileFlow.value = userProfile
                _xp.value = runCatching { achievementRepository.getXp() }.getOrDefault(0)
                val unlockedIds = runCatching { achievementRepository.getUnlockedIds() }.getOrDefault(emptyList())
                _achievementProgress.value = unlockedIds.size to AchievementDefs.all.size
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error loading profile data")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.clearUserCache()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            authRepository.signOut()
            onSuccess()
        }
    }

    fun resetAlgorithmData(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.resetAlgorithmData()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                _isLoading.value = false
                onComplete()
            }
        }
    }

    fun updateUsername(newName: String, onComplete: () -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onComplete()
            return
        }
        viewModelScope.launch {
            try {
                userRepository.updateProfile(trimmed)
                _displayName.value = trimmed
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error updating profile")
            } finally {
                onComplete()
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch(ioDispatcher) {
            _isUploadingPhoto.value = true
            try {
                val ref = firebaseStorage.reference.child("avatars/$uid.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                userRepository.updateProfilePhoto(url)
                _photoUrl.value = url
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error uploading profile photo")
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }
}
