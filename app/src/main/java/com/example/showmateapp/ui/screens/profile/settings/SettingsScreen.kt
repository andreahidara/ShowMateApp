package com.example.showmateapp.ui.screens.profile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.PrimaryButton
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.example.showmateapp.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    SettingsScreenContent(
        onBackClick = { navController.popBackStack() },
        isDarkMode = isDarkTheme,
        onDarkModeChange = viewModel::setDarkTheme
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    var autoplayVideo by remember { mutableStateOf(false) }
    var privateMode by remember { mutableStateOf(false) }
    
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
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
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
                        icon = Icons.Default.Person,
                        onClick = { showFeedback("Abriendo perfil...") }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Cambiar Contraseña",
                        icon = Icons.Default.Lock,
                        onClick = { showFeedback("Redirigiendo a seguridad...") }
                    )
                }
            }

            item {
                SettingsSection(title = "Ajustes de Interfaz") {
                    SettingsItemSwitch(
                        title = "Modo Oscuro",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkMode,
                        onCheckedChange = {
                            onDarkModeChange(it)
                            showFeedback(if (it) "Tema oscuro activado" else "Tema claro activado")
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Idioma",
                        icon = Icons.Default.Language,
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
                        icon = Icons.Default.Notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = { 
                            notificationsEnabled = it
                            showFeedback(if (it) "Notificaciones activadas" else "Notificaciones desactivadas")
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Reproducción y Privacidad") {
                    SettingsItemSwitch(
                        title = "Autoplay de Tráilers (Solo Wi-Fi)",
                        subtitle = "Reproduce sin sonido por defecto",
                        icon = Icons.Default.PlayCircle,
                        checked = autoplayVideo,
                        onCheckedChange = { 
                            autoplayVideo = it
                            showFeedback(if (it) "Autoplay activado en Wi-Fi" else "Autoplay desactivado")
                        }
                    )
                    SettingsDivider()
                    SettingsItemSwitch(
                        title = "Navegación Privada",
                        subtitle = "Tus vistas no afectarán tus recomendaciones",
                        icon = Icons.Default.Security,
                        checked = privateMode,
                        onCheckedChange = { 
                            privateMode = it
                            showFeedback(if (it) "Modo privado activado" else "Modo privado desactivado")
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Otros") {
                    SettingsItem(
                        title = "Borrar cuenta (RGPD)",
                        icon = Icons.Default.Delete,
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
                    PrimaryButton(
                        text = "Cerrar Sesión",
                        onClick = { showFeedback("Sesión cerrada correctamente") },
                        colorOverride = HeartRed,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                    )
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
            color = Color(0xFFB4B0FF),
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
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
        supportingContent = if (subtitle != null) {
            { Text(text = subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp) }
        } else null,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
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
