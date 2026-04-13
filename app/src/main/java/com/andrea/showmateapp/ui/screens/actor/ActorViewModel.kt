package com.andrea.showmateapp.ui.screens.actor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.PersonResponse
import com.andrea.showmateapp.domain.repository.IShowRepository
import com.andrea.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class ActorUiState(
    val isLoading: Boolean = false,
    val person: PersonResponse? = null,
    val credits: List<MediaContent> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ActorViewModel @Inject constructor(
    private val showRepository: IShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActorUiState())
    val uiState: StateFlow<ActorUiState> = _uiState.asStateFlow()

    fun loadActor(personId: Int) {
        if (_uiState.value.person?.id == personId) return
        _uiState.value = ActorUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val personDeferred = async { showRepository.getPersonDetails(personId) }
                val creditsDeferred = async { showRepository.getPersonTvCredits(personId) }

                val personRes = personDeferred.await()
                val creditsRes = creditsDeferred.await()

                _uiState.value = ActorUiState(
                    isLoading = false,
                    person = if (personRes is Resource.Success) personRes.data else null,
                    credits = if (creditsRes is Resource.Success) creditsRes.data else emptyList(),
                    error = if (personRes is Resource.Error) personRes.message else null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = ActorUiState(isLoading = false, error = e.message)
            }
        }
    }
}
