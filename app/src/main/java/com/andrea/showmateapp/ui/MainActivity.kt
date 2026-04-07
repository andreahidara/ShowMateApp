package com.andrea.showmateapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrea.showmateapp.data.local.ThemePreference
import com.andrea.showmateapp.ui.navigation.AppNavigation
import com.andrea.showmateapp.ui.theme.ShowMateAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreference: ThemePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themePreference.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            ShowMateAppTheme(darkTheme = isDarkTheme) {
                AppNavigation()
            }
        }
    }
}
