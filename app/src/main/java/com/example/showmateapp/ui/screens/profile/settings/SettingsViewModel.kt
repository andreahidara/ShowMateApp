package com.example.showmateapp.ui.screens.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.local.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreference: ThemePreference
) : ViewModel() {

    val isDarkTheme = themePreference.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            themePreference.setDarkTheme(enabled)
        }
    }
}
