package com.andrea.showmateapp.ui.screens.consent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrea.showmateapp.di.AppPrefsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_CONSENT = booleanPreferencesKey("consent_given")
    }

    val isConsentGiven = dataStore.data
        .map { prefs -> prefs[KEY_CONSENT] == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun giveConsent() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_CONSENT] = true }
        }
    }
}
