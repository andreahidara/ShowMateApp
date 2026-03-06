package com.example.showmateapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.showmateapp.ui.navigation.AppNavigation
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShowMateAppTheme {
                AppNavigation()
            }
        }
    }
}
