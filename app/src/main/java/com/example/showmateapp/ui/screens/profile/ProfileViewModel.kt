package com.example.showmateapp.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.AuthRepository
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.GetProfileStatsUseCase
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userEmail = MutableStateFlow("Usuario")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _stats = MutableStateFlow(GetProfileStatsUseCase.ProfileStats(totalWatchedHours = 0, watchedCount = 0))
    val stats: StateFlow<GetProfileStatsUseCase.ProfileStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                _userEmail.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                    } ?: "Usuario"
                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = userRepository.getAllRatings()
                val watched = userRepository.getWatchedShows()
                _stats.value = getProfileStatsUseCase.execute(watched, userProfile)
            } catch (_: Exception) {
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
                val (userProfile) = coroutineScope {
                    val syncJob = async { userRepository.syncFavoritesAndWatchedToRoom() }
                    val profileJob = async { userRepository.getUserProfile() }
                    syncJob.await()
                    listOf(profileJob.await())
                }

                _userEmail.value = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    } ?: "Usuario"

                _watchedEpisodesMap.value = userProfile?.watchedEpisodes ?: emptyMap()
                _customLists.value = userProfile?.customLists ?: emptyMap()
                _watchedRatings.value = userRepository.getAllRatings()

                // Stats siguen usando Firestore para estimar horas (necesitan numberOfSeasons)
                val watched = userRepository.getWatchedShows()
                _stats.value = getProfileStatsUseCase.execute(watched, userProfile)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        authRepository.signOut()
        onSuccess()
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.updateProfile(newName)
                loadProfileData()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
            } finally {
                _isLoading.value = false
                onComplete()
            }
        }
    }
}
