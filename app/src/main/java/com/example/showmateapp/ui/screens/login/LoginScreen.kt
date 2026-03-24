package com.example.showmateapp.ui.screens.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.components.premium.PrimaryButton
import com.example.showmateapp.ui.components.premium.PrimaryTextField
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleDark
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false; viewModel.dismissResetDialog(); forgotEmail = "" },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text("Recuperar contraseña", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Introduce tu email y te enviaremos un enlace para restablecer tu contraseña.",
                        color = TextGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    OutlinedTextField(
                        value = if (state.resetEmailSent) "" else forgotEmail.ifBlank { state.email },
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email", color = TextGray) },
                        singleLine = true,
                        enabled = !state.resetEmailSent,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrimaryPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.resetEmailSent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "¡Correo enviado! Revisa tu bandeja de entrada.",
                                color = Color(0xFF4CAF50),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    state.resetError?.let { err ->
                        Text(err, color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (!state.resetEmailSent) {
                    Button(
                        onClick = { viewModel.sendPasswordReset(forgotEmail.ifBlank { state.email }) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { Text("Enviar", fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { showForgotDialog = false; viewModel.dismissResetDialog(); forgotEmail = "" }) {
                        Text("Cerrar", color = PrimaryPurpleLight)
                    }
                }
            },
            dismissButton = {
                if (!state.resetEmailSent) {
                    TextButton(onClick = { showForgotDialog = false; viewModel.dismissResetDialog(); forgotEmail = "" }) {
                        Text("Cancelar", color = TextGray)
                    }
                }
            }
        )
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Main) {
                popUpTo(Screen.Login) { inclusive = true }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "authGlow")
    val orb1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val orb2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.20f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing, delayMillis = 800), RepeatMode.Reverse),
        label = "orb2"
    )

    AuthBackground {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .alpha(orb1Alpha)
                .blur(80.dp)
                .background(PrimaryPurple.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .alpha(orb2Alpha)
                .blur(80.dp)
                .background(PrimaryPurpleDark.copy(alpha = 0.6f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .alpha(0.30f)
                                .blur(24.dp)
                                .background(PrimaryPurple.copy(alpha = 0.7f), CircleShape)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.logosm),
                            contentDescription = "ShowMate Logo",
                            modifier = Modifier.size(90.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Bienvenido de nuevo",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Inicia sesión para continuar",
                        color = TextGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PrimaryTextField(
                        value = state.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = "Email",
                        leadingIcon = Icons.Default.Email,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryTextField(
                        value = state.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = "Contraseña",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (state.isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                    ),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = PrimaryPurpleLight, fontSize = 13.sp)
                    }

                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = error.asString(),
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(
                        text = "Iniciar Sesión",
                        onClick = { viewModel.onLoginClick() },
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿No tienes cuenta?  ",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Regístrate",
                            color = PrimaryPurpleLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate(Screen.SignUp)
                            }
                        )
                    }
                }
            }
        }
    }
}
