package com.example.showmateapp.ui.screens.main

import androidx.lifecycle.ViewModel
import com.example.showmateapp.data.repository.AuthRepository
import com.example.showmateapp.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    val networkMonitor: NetworkMonitor,
    val authRepository: AuthRepository
) : ViewModel()
