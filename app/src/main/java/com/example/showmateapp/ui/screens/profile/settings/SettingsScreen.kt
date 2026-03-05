package com.example.showmateapp.ui.screens.profile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    SettingsScreenContent(onBackClick = { navController.popBackStack() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(onBackClick: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(true) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showFeedback(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Ajustes", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsSection(title = "Cuenta") {
                    SettingsItem(
                        title = "Editar Perfil",
                        iconRes = R.drawable.ic_person,
                        onClick = { showFeedback("Abriendo perfil...") }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Cambiar Contraseña",
                        iconRes = R.drawable.ic_lock,
                        onClick = { showFeedback("Redirigiendo a seguridad...") }
                    )
                }
            }

            item {
                SettingsSection(title = "Ajustes de Interfaz") {
                    SettingsItemSwitch(
                        title = "Modo Oscuro",
                        iconRes = R.drawable.ic_dark_mode,
                        checked = isDarkMode,
                        onCheckedChange = { 
                            isDarkMode = it
                            showFeedback(if (it) "Tema oscuro activado" else "Tema claro activado")
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Idioma",
                        iconRes = R.drawable.ic_language,
                        value = "Español",
                        onClick = { showFeedback("Seleccionando idioma...") }
                    )
                }
            }

            item {
                SettingsSection(title = "Notificaciones") {
                    SettingsItemSwitch(
                        title = "Permitir Notificaciones Push",
                        subtitle = "Nuevos episodios y recomendaciones",
                        iconRes = R.drawable.ic_notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = { 
                            notificationsEnabled = it
                            showFeedback(if (it) "Notificaciones activadas" else "Notificaciones desactivadas")
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Otros") {
                    SettingsItem(
                        title = "Borrar cuenta (RGPD)",
                        iconRes = R.drawable.ic_delete,
                        onClick = { showFeedback("Solicitud de borrado en proceso...") }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { showFeedback("Sesión cerrada correctamente") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeartRed.copy(alpha = 0.15f),
                            contentColor = HeartRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = title.uppercase(),
            color = Color(0xFFB4B0FF), // Very light purple for high contrast (>7:1)
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .semantics { heading() }
                .padding(start = 24.dp, bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = SurfaceDark.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.semantics { contentDescription = "Sección $title" }) {
                content()
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.08f)
    )
}

@Composable
fun SettingsItem(
    title: String,
    iconRes: Int,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = Color.White, fontSize = 15.sp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        color = TextGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                if (showChevron) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = onClick,
                        onClickLabel = "Abrir $title"
                    )
                } else Modifier
            )
    )
}

@Composable
fun SettingsItemSwitch(
    title: String,
    iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = Color.White, fontSize = 15.sp) },
        supportingContent = if (subtitle != null) {
            { Text(text = subtitle, color = TextGray, fontSize = 12.sp) }
        } else null,
        leadingContent = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by toggleable row
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryPurple,
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.5f)
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ShowMateAppTheme {
        SettingsScreenContent(onBackClick = {})
    }
}
