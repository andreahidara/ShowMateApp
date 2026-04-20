package com.andrea.showmateapp.ui.screens.profile

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.di.IoDispatcher
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.IAuthRepository
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.domain.usecase.GetProfileStatsUseCase
import com.andrea.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val interactionRepository: IInteractionRepository,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val getViewerPersonalityUseCase: GetViewerPersonalityUseCase,
    private val authRepository: IAuthRepository,
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

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _achievementProgress = MutableStateFlow(0 to AchievementDefs.all.size)
    val achievementProgress: StateFlow<Pair<Int, Int>> = _achievementProgress.asStateFlow()

    init {
        viewModelScope.launch {
            loadAchievementsData()
            _isLoading.value = false
        }
    }

    private suspend fun loadAchievementsData() {
        try { interactionRepository.syncFavoritesAndWatchedToRoom() } catch (e: Exception) { Timber.e(e) }
        _xp.value = runCatching { achievementRepository.getXp() }.getOrDefault(0)
        val unlockedIds = runCatching { achievementRepository.getUnlockedIds() }.getOrDefault(emptyList())
        _achievementProgress.value = unlockedIds.size to AchievementDefs.all.size
    }

    private val userProfileFlow = userRepository.getUserProfileFlow()
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val displayName: StateFlow<String> = userProfileFlow.map { profile ->
        profile?.username?.takeIf { it.isNotBlank() }
            ?: userRepository.getCurrentUserEmail()?.substringBefore("@")?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Usuario"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cargando...")

    val photoUrl: StateFlow<String?> = userProfileFlow.map { it?.photoUrl }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val viewerPersonality: StateFlow<GetViewerPersonalityUseCase.PersonalityProfile?> = userProfileFlow.map {
        it?.let { getViewerPersonalityUseCase.execute(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val friendCount: StateFlow<Int> = userProfileFlow.map { it?.friendIds?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val userLevel: StateFlow<UserLevel> = _xp.map { xp ->
        val lvl = AchievementDefs.levelForXp(xp)
        val prog = AchievementDefs.progressInLevel(xp)
        val next = AchievementDefs.levels.getOrNull(lvl.level)?.name
        UserLevel(lvl.name, prog, next)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserLevel("Rookie", 0f, "Espectador"))

    val watchedShows: StateFlow<List<WatchedShowItem>> = interactionRepository.getWatchedShowsFlow()
        .combine(userProfileFlow.filterNotNull()) { entities, profile ->
            entities.map { entity ->
                WatchedShowItem(
                    show = entity.toDomain(),
                    episodesWatched = profile.watchedEpisodes[entity.id.toString()]?.size ?: 0
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedShows: StateFlow<List<MediaContent>> = interactionRepository.getLikedShowsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistShows: StateFlow<List<MediaContent>> = interactionRepository.getWatchlistShowsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<GetProfileStatsUseCase.ProfileStats> = combine(watchedShows, userProfileFlow) { items, profile ->
        getProfileStatsUseCase.execute(items.map { it.show }, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GetProfileStatsUseCase.ProfileStats())

    val customLists: StateFlow<Map<String, List<Int>>> = userProfileFlow.map { it?.customLists ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val watchedRatings: StateFlow<Map<Int, Int>> = userProfileFlow.map { profile ->
        profile?.ratings?.mapKeys { it.key.toIntOrNull() ?: 0 }?.mapValues { it.value.toInt() } ?: emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadAchievementsData()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error refreshing profile")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.clearUserCache()
                authRepository.signOut()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e)
            }
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
                Timber.e(e)
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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error uploading profile photo")
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }
}

