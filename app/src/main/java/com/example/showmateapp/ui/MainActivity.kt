package com.example.showmateapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.showmateapp.data.local.ThemePreference
import com.example.showmateapp.ui.navigation.AppNavigation
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreference: ThemePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by themePreference.isDarkTheme.collectAsState(initial = true)
            ShowMateAppTheme(darkTheme = isDarkTheme) {
                AppNavigation()
            }
        }
    }
}
