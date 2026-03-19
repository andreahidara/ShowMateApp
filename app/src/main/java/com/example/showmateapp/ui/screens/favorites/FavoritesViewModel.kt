package com.example.showmateapp.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val favorites: StateFlow<List<MediaContent>> = userRepository
        .getLikedShowsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Sync Firestore → Room on startup so favorites are visible even after
        // a fresh install or after the local database was wiped
        viewModelScope.launch {
            userRepository.syncFavoritesAndWatchedToRoom()
        }
    }
}
