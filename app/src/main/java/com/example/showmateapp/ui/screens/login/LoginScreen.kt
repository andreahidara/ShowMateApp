package com.example.showmateapp.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    // Conectamos nuestra pantalla con el cerebro que creaste
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Escuchamos lo que nos dice el ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoginSuccessful by viewModel.isLoginSuccessful.collectAsState()

    // Magia: Si el login tiene éxito, saltamos a la pantalla de Onboarding
    LaunchedEffect(isLoginSuccessful) {
        if (isLoginSuccessful) {
            // Por ahora vamos a un sitio temporal llamado "onboarding"
            navController.navigate("onboarding") {
                popUpTo("login") { inclusive = true } // Borramos el login del historial
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_logo_placeholder),
            contentDescription = "Logo",
            modifier = Modifier.size(60.dp),
            colorFilter = ColorFilter.tint(PrimaryPurple)
        )
        Text(text = "ShowMate", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Welcome Back!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = "Please sign in to your account.", color = TextGray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple, unfocusedBorderColor = SurfaceDark,
                focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true // Para que no se hagan saltos de línea
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = TextGray) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple, unfocusedBorderColor = SurfaceDark,
                focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Si hay un error (contraseña mal), lo mostramos en rojo
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = HeartRed, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        }

        Text(text = "Forgot Password?", color = PrimaryPurple, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.End))

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Principal
        Button(
            // Al hacer clic, le pasamos el email y contraseña al ViewModel
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading // Desactivamos el botón si está cargando
        ) {
            if (isLoading) {
                // Muestra un circulito de carga
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ShowMateAppTheme {
        LoginScreen(navController = rememberNavController())
    }
}