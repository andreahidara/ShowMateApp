package com.andrea.showmateapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    fun updateWatchlistInfo(nextShowName: String?, watchlistCount: Int) {
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit().apply {
            if (nextShowName != null) {
                putString("next_show_name", nextShowName)
            } else {
                remove("next_show_name")
            }
            putInt("watchlist_count", watchlistCount)
            apply()
        }
        scope.launch {
            try {
                ShowMateWidget().updateAll(context)
            } catch (e: Exception) {
                Timber.w(e, "Widget update failed")
            }
        }
    }
}
