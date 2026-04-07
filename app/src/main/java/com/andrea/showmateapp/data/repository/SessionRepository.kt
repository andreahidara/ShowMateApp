package com.andrea.showmateapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.andrea.showmateapp.di.SecurityDataStore
import com.andrea.showmateapp.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    @SecurityDataStore private val dataStore: DataStore<Preferences>,
    private val authRepository: IAuthRepository
) {
    companion object {
        private val KEY_LAST_ACTIVITY = longPreferencesKey("last_activity_ms")
        private const val SESSION_TIMEOUT_MS = 30L * 24 * 60 * 60 * 1000
    }

    suspend fun updateLastActivity() {
        dataStore.edit { it[KEY_LAST_ACTIVITY] = System.currentTimeMillis() }
    }

    suspend fun checkSessionAndLogoutIfExpired(): Boolean {
        if (authRepository.getCurrentUser() == null) return true

        val prefs = dataStore.data.first()
        val lastActivity = prefs[KEY_LAST_ACTIVITY]

        if (lastActivity == null) {
            updateLastActivity()
            return true
        }

        val elapsed = System.currentTimeMillis() - lastActivity
        if (elapsed > SESSION_TIMEOUT_MS) {
            authRepository.signOut()
            return false
        }
        return true
    }
}
