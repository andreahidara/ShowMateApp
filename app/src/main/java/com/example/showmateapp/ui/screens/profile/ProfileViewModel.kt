package com.example.showmateapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _userEmail = MutableStateFlow<String>("User")
    val userEmail: StateFlow<String> = _userEmail

    private val _favoritesCount = MutableStateFlow(0)
    val favoritesCount: StateFlow<Int> = _favoritesCount

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            val email = repository.getCurrentUserEmail()
            _userEmail.value = email?.substringBefore("@")?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Usuario"
            _favoritesCount.value = repository.getFavorites().size
        }
    }
}
