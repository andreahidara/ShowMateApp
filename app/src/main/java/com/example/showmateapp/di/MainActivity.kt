package com.example.showmateapp.di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.showmateapp.ui.navigation.AppNavigation
import com.example.showmateapp.ui.theme.ShowMateAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Cargamos tu tema de colores
            ShowMateAppTheme {
                // 2. Mostramos tu pantalla inicial
                AppNavigation()
            }
        }
    }
}