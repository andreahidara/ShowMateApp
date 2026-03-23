package com.example.showmateapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.example.showmateapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun isLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}
