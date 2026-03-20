package com.example.showmateapp.ui.screens.profile.allshows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.showmateapp.data.model.toDomain
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AllShowsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    val type: String = savedStateHandle.toRoute<Screen.AllShows>().type

    val shows: StateFlow<List<MediaContent>> = if (type == "watched") {
        userRepository.getWatchedShowsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        userRepository.getLikedShowsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }
}
