package com.example.showmateapp.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.showmateapp.R
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController


    @Composable
    fun SplashScreen(navController: NavController) { // <-- 1. Añadimos el NavController aquí

        // 2. Este bloque de código se ejecuta nada más cargar la pantalla
        LaunchedEffect(key1 = true) {
            delay(2000) // Esperamos 2000 milisegundos (2 segundos)

            // Navegamos al login, borrando el splash del historial para que
            // si el usuario le da al botón "Atrás" del móvil, no vuelva al logo
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    // Box es como una caja. Usamos fillMaxSize() para que ocupe toda la pantalla.
    // Y le ponemos el color de fondo oscuro de nuestro tema.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Usa el color de fondo del tema (BackgroundDark)
        contentAlignment = Alignment.Center // Todo lo que metamos dentro se centrará
    ) {
        // Column apila elementos verticalmente (Logo encima del Texto)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. El Logo (Usamos el icono temporal que creamos)
            Image(
                painter = painterResource(id = R.drawable.ic_logo_placeholder),
                contentDescription = "Logo ShowMate",
                modifier = Modifier.size(100.dp), // Tamaño del logo
                // Teñimos el icono de color púrpura para que se parezca a tu diseño
                colorFilter = ColorFilter.tint(PrimaryPurple)
            )

            Spacer(modifier = Modifier.height(24.dp)) // Espacio entre logo y texto

            // 2. El Título Principal
            Text(
                text = "ShowMate",
                color = Color.White, // Texto blanco
                fontSize = 36.sp, // Tamaño grande
                fontWeight = FontWeight.Bold // Negrita
            )

            Spacer(modifier = Modifier.height(8.dp)) // Espacio pequeño

            // 3. El Subtítulo
            Text(
                text = "PREMIUM SERIES GUIDE",
                color = PrimaryPurple, // Usamos el color acento púrpura
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp // Separamos un poco las letras como en el diseño
            )
        }
    }
}

// --- PREVISUALIZACIÓN ---
// Esto te permite ver cómo queda la pantalla sin ejecutar la app en el emulador.
// A veces tarda un poco en cargar la primera vez.
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    ShowMateAppTheme {
        SplashScreen(navController = rememberNavController())
    }
}