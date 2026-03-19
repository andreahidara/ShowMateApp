package com.example.showmateapp.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.components.premium.PrimaryButton
import com.example.showmateapp.ui.components.premium.PrimaryTextField
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurpleLight

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Main) {
                popUpTo(Screen.Login) { inclusive = true }
            }
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logosm),
                contentDescription = "ShowMate Logo",
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
            )

            Text(
                text = "Bienvenido de nuevo",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryTextField(
                value = state.email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                            contentDescription = if (state.isPasswordVisible) "Ocultar Contraseña" else "Mostrar Contraseña",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it.asString(), color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Iniciar Sesión",
                onClick = { viewModel.onLoginClick() },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = { navController.navigate(Screen.SignUp) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("¿No tienes cuenta? Regístrate", color = PrimaryPurpleLight)
            }
        }
    }
}