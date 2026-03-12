package com.example.showmateapp.ui.screens.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.components.premium.PrimaryButton
import com.example.showmateapp.ui.components.premium.PrimaryTextField
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Este bloque detecta cuando el registro ha funcionado para saltar de pantalla
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Main) {
                popUpTo<Screen.SignUp> { inclusive = true }
                popUpTo<Screen.Login> { inclusive = true }
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
                modifier = Modifier.size(100.dp).padding(bottom = 16.dp)
            )

            Text(
                text = "Crea tu cuenta",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo Nombre de Usuario
            PrimaryTextField(
                value = state.username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = "Nombre de usuario",
                leadingIcon = Icons.Default.Person,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Email
            PrimaryTextField(
                value = state.email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Contraseña
            PrimaryTextField(
                value = state.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Contraseña",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    val iconText = if (state.isPasswordVisible) "Ocultar" else "Ver"
                    TextButton(onClick = { viewModel.togglePasswordVisibility() }) {
                        Text(iconText, color = TextGray, fontSize = 12.sp)
                    }
                },
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Confirmar Contraseña
            PrimaryTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                label = "Confirmar Contraseña",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    val iconText = if (state.isConfirmPasswordVisible) "Ocultar" else "Ver"
                    TextButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                        Text(iconText, color = TextGray, fontSize = 12.sp)
                    }
                },
                visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // Mostrar error si existe
            state.error?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Registrarse",
                onClick = { viewModel.onSignUpClick() },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = PrimaryPurpleLight)
            }
        }
    }
}
