package com.andrea.showmateapp.ui.screens.consent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.di.AppPrefsDataStore
import com.andrea.showmateapp.util.AppPrefsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConsentViewModel @Inject constructor(
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val isConsentGiven = dataStore.data
        .map { prefs -> prefs[AppPrefsKeys.KEY_CONSENT] == true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun giveConsent() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[AppPrefsKeys.KEY_CONSENT] = true }
        }
    }
}
