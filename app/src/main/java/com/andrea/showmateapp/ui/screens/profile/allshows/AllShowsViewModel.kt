package com.andrea.showmateapp.ui.screens.profile.allshows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.andrea.showmateapp.data.model.toDomain
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.domain.repository.IInteractionRepository
import com.andrea.showmateapp.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AllShowsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val interactionRepository: IInteractionRepository
) : ViewModel() {

    val type: String = savedStateHandle.toRoute<Screen.AllShows>().type

    val shows: StateFlow<List<MediaContent>> = if (type == "watched") {
        interactionRepository.getWatchedShowsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        interactionRepository.getLikedShowsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }
}
