package com.example.showmateapp.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.AuthRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.GetProfileStatsUseCase
import com.example.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class WatchedShowItem(
    val show: MediaContent,
    val episodesWatched: Int
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val getViewerPersonalityUseCase: GetViewerPersonalityUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _displayName = MutableStateFlow("Usuario")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _stats = MutableStateFlow(GetProfileStatsUseCase.ProfileStats(totalWatchedHours = 0, watchedCount = 0))
    val stats: StateFlow<GetProfileStatsUseCase.ProfileStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewerPersonality = MutableStateFlow<GetViewerPersonalityUseCase.PersonalityProfile?>(null)
    val viewerPersonality: StateFlow<GetViewerPersonalityUseCase.PersonalityProfile?> = _viewerPersonality.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Flow reactivo desde Room: series vistas con conteo de episodios de Firestore
    private val _watchedEpisodesMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())

    private val _customLists = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val customLists: StateFlow<Map<String, List<Int>>> = _customLists.asStateFlow()

    private val _watchedRatings = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val watchedRatings: StateFlow<Map<Int, Int>> = _watchedRatings.asStateFlow()

    val watchedShows: StateFlow<List<WatchedShowItem>> =
        combine(userRepository.getWatchedShowsFlow(), _watchedEpisodesMap) { entities, episodesMap ->
            entities.map { entity ->
                WatchedShowItem(
                    show = entity.toDomain(),
                    episodesWatched = episodesMap[entity.id.toString()]?.size ?: 0
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val likedShows: StateFlow<List<MediaContent>> =
        userRepository.getLikedShowsFlow().map { entities -> entities.map { it.toDomain() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        loadProfileData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                userRepository.syncFavoritesAndWatchedToRoom()
                val email = userRepository.getCurrentUserEmail()
                val userProfile = userRepository.getUserProfile()
                _displayName.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                    } ?: "Usuario"
                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = userRepository.getAllRatings()
                _viewerPersonality.value = userProfile?.let { getViewerPersonalityUseCase.execute(it) }
                val watched = userRepository.getWatchedShows()
                _stats.value = getProfileStatsUseCase.execute(watched, userProfile)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error refreshing profile", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sync Firestore → Room and fetch profile in parallel
                val email = userRepository.getCurrentUserEmail()
                val (userProfile, ratingsMap, watchedList) = coroutineScope {
                    val syncJob = async { userRepository.syncFavoritesAndWatchedToRoom() }
                    val profileJob = async { userRepository.getUserProfile() }
                    val ratingsJob = async { userRepository.getAllRatings() }
                    val watchedJob = async { userRepository.getWatchedShows() }
                    syncJob.await()
                    Triple(profileJob.await(), ratingsJob.await(), watchedJob.await())
                }

                _displayName.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    } ?: "Usuario"

                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = ratingsMap

                _viewerPersonality.value = userProfile?.let { getViewerPersonalityUseCase.execute(it) }

                _stats.value = getProfileStatsUseCase.execute(watchedList, userProfile)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try { userRepository.clearUserCache() } catch (_: Exception) {}
            authRepository.signOut()
            onSuccess()
        }
    }

    fun resetAlgorithmData(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.resetAlgorithmData()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
                onComplete()
            }
        }
    }

    fun updateUsername(newName: String, onComplete: () -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) { onComplete(); return }
        viewModelScope.launch {
            try {
                userRepository.updateProfile(trimmed)
                _displayName.value = trimmed  // actualiza solo el nombre visible; evita recargar todo el perfil
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
            } finally {
                onComplete()
            }
        }
    }
}
