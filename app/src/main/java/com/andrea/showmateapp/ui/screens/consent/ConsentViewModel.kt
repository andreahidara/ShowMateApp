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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConsentViewModel @Inject constructor(
    @AppPrefsDataStore private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _consentAccepted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val consentAccepted: SharedFlow<Unit> = _consentAccepted.asSharedFlow()

    fun giveConsent() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[AppPrefsKeys.KEY_CONSENT] = true }
            _consentAccepted.emit(Unit)
        }
    }
}
