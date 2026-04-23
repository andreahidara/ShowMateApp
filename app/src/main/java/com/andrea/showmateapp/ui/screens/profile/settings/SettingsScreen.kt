package com.andrea.showmateapp.ui.screens.profile.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.HeartRed
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleDark
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.SurfaceVariantDark
import com.andrea.showmateapp.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    val accountDeleted by viewModel.accountDeleted.collectAsStateWithLifecycle()
    val currentEmail by viewModel.currentEmail.collectAsStateWithLifecycle()
    val notifEnabled by viewModel.notifEnabled.collectAsStateWithLifecycle()
    val isResetting by viewModel.isResetting.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) {
        if (loggedOut) navController.navigate(Screen.Login) { popUpTo(0) { inclusive = true } }
    }
    LaunchedEffect(accountDeleted) {
        if (accountDeleted) navController.navigate(Screen.Login) { popUpTo(0) { inclusive = true } }
    }

    SettingsScreenContent(
        onBackClick = { navController.popBackStack() },
        currentEmail = currentEmail,
        onUpdateDisplayName = { name, cb -> viewModel.updateDisplayName(name, cb) },
        onSendPasswordReset = { cb -> viewModel.sendPasswordReset(cb) },
        notifEnabled = notifEnabled,
        onNotifEnabledChange = viewModel::setNotifEnabled,
        onDeleteAccount = { cb -> viewModel.deleteAccount(cb) },
        onResetAlgorithm = { onResult ->
            viewModel.resetAlgorithmData { success ->
                if (success) {
                    navController.navigate(Screen.Onboarding) { popUpTo(0) { inclusive = true } }
                }
                onResult(success)
            }
        },
        isResetting = isResetting
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    onBackClick: () -> Unit,
    currentEmail: String = "",
    onUpdateDisplayName: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onSendPasswordReset: ((Boolean) -> Unit) -> Unit = {},
    notifEnabled: Boolean = true,
    onNotifEnabledChange: (Boolean) -> Unit = {},
    onDeleteAccount: ((Boolean) -> Unit) -> Unit = {},
    onResetAlgorithm: ((Boolean) -> Unit) -> Unit = {},
    isResetting: Boolean = false
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showFeedback(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = HeartRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar cuenta", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Tu cuenta y todos tus datos personales asociados serán eliminados permanentemente.",
                    color = TextGray,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount { success ->
                            if (!success) {
                                showFeedback(
                                    "Error al eliminar la cuenta. Vuelve a iniciar sesión e inténtalo de nuevo."
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = TextGray) }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Reiniciar gustos", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Esto borrará todos los datos de tu cuenta (preferencias, amigos, experiencia) y tendrás que volver a pasar por el onboarding para empezar de nuevo.",
                        color = TextGray
                    )
                    if (isResetting) {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = PrimaryPurple
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAlgorithm { success ->
                            if (success) {
                                showResetDialog = false
                            } else {
                                showFeedback("Error al reiniciar los datos. Comprueba tu conexión.")
                                showResetDialog = false
                            }
                        }
                    },
                    enabled = !isResetting,
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) {
                    if (isResetting) {
                        Text("Reiniciando...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Reiniciar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isResetting
                ) { Text("Cancelar", color = TextGray) }
            }
        )
    }


    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Editar nombre", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(currentEmail, color = TextGray, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editNameValue,
                        onValueChange = { editNameValue = it },
                        label = { Text("Nombre de usuario", color = TextGray) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrimaryPurple,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editNameValue.trim()
                        if (name.isNotBlank()) {
                            onUpdateDisplayName(name) { success ->
                                showEditProfileDialog = false
                                showFeedback(
                                    if (success) "Nombre actualizado correctamente" else "Error al actualizar el nombre"
                                )
                            }
                        }
                    },
                    enabled = editNameValue.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) { Text("Guardar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancelar", color = TextGray) }
            }
        )
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                SettingsSection(title = "Cuenta") {
                    SettingsItem(
                        title = "Editar nombre de usuario",
                        icon = Icons.Default.Edit,
                        iconTint = Color(0xFF7C4DFF),
                        onClick = {
                            editNameValue = ""
                            showEditProfileDialog = true
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Cambiar contraseña",
                        subtitle = "Te enviaremos un enlace por email",
                        icon = Icons.Default.Lock,
                        iconTint = Color(0xFF2196F3),
                        onClick = {
                            onSendPasswordReset { success ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) {
                                            "Email de recuperación enviado a $currentEmail."
                                        } else {
                                            "Error al enviar el email."
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Notificaciones") {
                    SettingsItemSwitch(
                        title = "Notificaciones push",
                        subtitle = if (notifEnabled) "Activas" else "Desactivadas",
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFFE91E63),
                        checked = notifEnabled,
                        onCheckedChange = {
                            onNotifEnabledChange(it)
                            showFeedback(if (it) "Notificaciones activadas" else "Notificaciones desactivadas")
                        }
                    )
                }
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingsSection(title = "Soporte") {
                    SettingsItem(
                        title = "Reportar un problema",
                        subtitle = "Sugerencias o errores",
                        icon = Icons.Default.BugReport,
                        iconTint = Color(0xFFFF5722),
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                val uriString = "mailto:soporte@showmate.app?subject=Reporte ShowMate"
                                data = android.net.Uri.parse(uriString)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showFeedback("No hay apps de correo instaladas")
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Zona de riesgo") {
                    SettingsItem(
                        title = "Reiniciar mis gustos",
                        subtitle = "Borra todos tus datos y reinicia tu cuenta",
                        icon = Icons.Default.Update,
                        iconTint = PrimaryPurpleLight,
                        onClick = { showResetDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Eliminar cuenta",
                        subtitle = "Acción irreversible",
                        icon = Icons.Default.DeleteForever,
                        iconTint = HeartRed,
                        showChevron = false,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ShowMate · Versión 1.0.0",
                        color = TextGray.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.padding(top = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(PrimaryPurple, PrimaryPurpleDark)))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = Color(0xFFB4B0FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariantDark)
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
        color = Color.White.copy(alpha = 0.06f)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = PrimaryPurple,
    value: String? = null,
    subtitle: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = Color.White, fontSize = 15.sp) },
        supportingContent = if (subtitle != null) {
            { Text(text = subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp) }
        } else {
            null
        },
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
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick, onClickLabel = "Abrir $title")
                } else {
                    Modifier
                }
            )
    )
}

@Composable
fun SettingsItemSwitch(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = PrimaryPurple,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(text = title, color = Color.White, fontSize = 15.sp) },
        supportingContent = if (subtitle != null) {
            { Text(text = subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp) }
        } else {
            null
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
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
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
    )
}
