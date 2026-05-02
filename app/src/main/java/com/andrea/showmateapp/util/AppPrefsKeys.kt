package com.andrea.showmateapp.util

import androidx.datastore.preferences.core.booleanPreferencesKey

object AppPrefsKeys {
    val KEY_CONSENT = booleanPreferencesKey("consent_given")
    val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
}
