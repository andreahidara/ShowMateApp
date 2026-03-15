package com.example.showmateapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.domain.usecase.GetProfileStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import com.example.showmateapp.data.network.MediaContent
import com.google.firebase.auth.FirebaseAuth

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userEmail = MutableStateFlow<String>("User")
    val userEmail: StateFlow<String> = _userEmail

    private val _favoritesCount = MutableStateFlow(0)
    val favoritesCount: StateFlow<Int> = _favoritesCount

    private val _favoriteShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val favoriteShows: StateFlow<List<MediaContent>> = _favoriteShows

    private val _watchedShows = MutableStateFlow<List<MediaContent>>(emptyList())
    val watchedShows: StateFlow<List<MediaContent>> = _watchedShows

    private val _totalWatchedHours = MutableStateFlow(0)
    val totalWatchedHours: StateFlow<Int> = _totalWatchedHours

    private val _watchedCount = MutableStateFlow(0)
    val watchedCount: StateFlow<Int> = _watchedCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val email = userRepository.getCurrentUserEmail()
                _userEmail.value = email?.substringBefore("@")?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } ?: "Usuario"
                
                val favorites = userRepository.getFavorites()
                _favoriteShows.value = favorites
                _favoritesCount.value = favorites.size

                val watched = userRepository.getWatchedShows()
                _watchedShows.value = watched
                _watchedCount.value = watched.size
                
                val stats = getProfileStatsUseCase.execute(watched)
                _totalWatchedHours.value = stats.totalWatchedHours
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        auth.signOut()
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
}
