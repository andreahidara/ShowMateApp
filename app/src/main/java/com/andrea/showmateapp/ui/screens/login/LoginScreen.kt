@file:Suppress("DEPRECATION")
package com.andrea.showmateapp.ui.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.components.AuthBackground
import com.andrea.showmateapp.ui.components.PrimaryButton
import com.andrea.showmateapp.ui.components.PrimaryTextField
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.PrimaryMagenta
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.TextGray
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = runCatching {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
            }.getOrNull()
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.onGoogleSignInFailed()
            }
        }
    }

    val googleSignInClient = remember(context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val destination = if (state.isNewGoogleUser || !state.isOnboardingCompleted) {
                Screen.Onboarding
            } else {
                Screen.Main
            }
            navController.navigate(destination) {
                popUpTo(Screen.Login) { inclusive = true }
            }
        }
    }

    LoginScreenContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::onLoginClick,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onSendPasswordReset = viewModel::sendPasswordReset,
        onDismissResetDialog = viewModel::dismissResetDialog,
        onNavigateToSignUp = { navController.navigate(Screen.SignUp) },
        onGoogleSignIn = { googleLauncher.launch(googleSignInClient.signInIntent) }
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSendPasswordReset: (String) -> Unit,
    onDismissResetDialog: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onGoogleSignIn: () -> Unit = {}
) {
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
                onDismissResetDialog()
                forgotEmail = ""
            },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text(stringResource(R.string.login_recover_password), color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.login_reset_instructions),
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
                                stringResource(R.string.login_email_sent),
                                color = Color(0xFF4CAF50),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    state.resetError?.let { err ->
                        Text(err.asString(), color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (!state.resetEmailSent) {
                    Button(
                        onClick = { onSendPasswordReset(forgotEmail.ifBlank { state.email }) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { Text(stringResource(R.string.login_send), fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = {
                        showForgotDialog = false
                        onDismissResetDialog()
                        forgotEmail = ""
                    }) {
                        Text(stringResource(R.string.close), color = PrimaryPurpleLight)
                    }
                }
            },
            dismissButton = {
                if (!state.resetEmailSent) {
                    TextButton(onClick = {
                        showForgotDialog = false
                        onDismissResetDialog()
                        forgotEmail = ""
                    }) {
                        Text(stringResource(R.string.cancel), color = TextGray)
                    }
                }
            }
        )
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.showmate),
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryMagenta)),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.login_welcome_back),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        color = TextGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PrimaryTextField(
                        value = state.email,
                        onValueChange = onEmailChanged,
                        label = "Email",
                        leadingIcon = Icons.Default.Email,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryTextField(
                        value = state.password,
                        onValueChange = onPasswordChanged,
                        label = stringResource(R.string.login_password),
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (state.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    painter = painterResource(
                                        id = if (state.isPasswordVisible) {
                                            R.drawable.ic_visibility
                                        } else {
                                            R.drawable.ic_visibility_off
                                        }
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
                        Text(stringResource(R.string.login_forgot_password), color = PrimaryPurpleLight, fontSize = 13.sp)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = stringResource(R.string.sign_in),
                        onClick = onLoginClick,
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_no_account),
                            color = TextGray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.login_register),
                            color = PrimaryPurpleLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToSignUp() }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                        Text(
                            text = stringResource(R.string.login_or_continue_with),
                            color = TextGray,
                            fontSize = 12.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .then(
                                if (state.isGoogleLoading) {
                                    Modifier
                                } else {
                                    Modifier.clickable { onGoogleSignIn() }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isGoogleLoading) {
                            CircularProgressIndicator(
                                color = PrimaryPurple,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.login_continue_with_google),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
