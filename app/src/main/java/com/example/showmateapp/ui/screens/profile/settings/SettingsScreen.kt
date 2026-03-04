package com.example.showmateapp.ui.screens.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var dataSaverEnabled by remember { mutableStateOf(false) }
    var useCellularData by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SettingsCategoryTitle("Cuenta")
                SettingsItemText("Editar Perfil")
                SettingsItemText("Cambiar Contraseña")
                SettingsItemText("Gestionar Suscripción", value = "Standard Plan")
            }

            item {
                SettingsCategoryTitle("Preferencias de Video")
                SettingsItemSwitch(
                    title = "Descargas solo por Wi-Fi",
                    checked = dataSaverEnabled,
                    onCheckedChange = { dataSaverEnabled = it }
                )
                SettingsItemSwitch(
                    title = "Reproducción automática",
                    checked = useCellularData,
                    onCheckedChange = { useCellularData = it }
                )
                SettingsItemText("Calidad de Descarga", value = "Alta (1080p)")
            }

            item {
                SettingsCategoryTitle("Notificaciones")
                SettingsItemSwitch(
                    title = "Permitir Notificaciones Push",
                    subtitle = "Nuevos episodios y recomendaciones",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }

            item {
                SettingsCategoryTitle("Acerca de ShowMate")
                SettingsItemText("Términos de Servicio")
                SettingsItemText("Política de Privacidad")
                SettingsItemText("Versión", value = "v1.0.2")
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { /* Logout */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = com.example.showmateapp.ui.theme.HeartRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        color = PrimaryPurple,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItemText(title: String, value: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        
        if (value != null) {
            Text(text = value, color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsItemSwitch(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = com.example.showmateapp.ui.theme.SurfaceDark
            )
        )
    }
}
