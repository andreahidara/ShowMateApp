package com.andrea.showmateapp.util

import androidx.datastore.preferences.core.booleanPreferencesKey

object NotificationPrefsKeys {
    val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    val NOTIF_NEW_EPISODES = booleanPreferencesKey("notif_new_episodes")
    val NOTIF_FRIENDS = booleanPreferencesKey("notif_friends")
    val NOTIF_RECOMMENDATIONS = booleanPreferencesKey("notif_recommendations")
}
