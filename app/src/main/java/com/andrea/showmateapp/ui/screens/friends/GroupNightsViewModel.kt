package com.andrea.showmateapp.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.data.model.GroupSession
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.repository.ShowRepository
import com.andrea.showmateapp.domain.repository.IGroupSessionRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NightEntry(
    val session: GroupSession,
    val matchedMedia: MediaContent?
)

@HiltViewModel
class GroupNightsViewModel @Inject constructor(
    private val groupSessionRepository: IGroupSessionRepository,
    private val userRepository: IUserRepository,
    private val showRepository: ShowRepository
) : ViewModel() {

    private val _entries   = MutableStateFlow<List<NightEntry>>(emptyList())
    val entries: StateFlow<List<NightEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val email = userRepository.getCurrentUserEmail() ?: run {
                _isLoading.value = false
                return@launch
            }
            try {
                val sessions = groupSessionRepository.getPastNights(email)
                _entries.value = sessions.map { session ->
                    val media = if (session.matchedMediaId != 0) {
                        (showRepository.getShowDetails(session.matchedMediaId) as? Resource.Success)?.data
                    } else null
                    NightEntry(session, media)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            _isLoading.value = false
        }
    }
}
