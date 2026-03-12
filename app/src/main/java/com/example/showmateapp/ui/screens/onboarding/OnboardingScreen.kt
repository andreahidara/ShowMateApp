package com.example.showmateapp.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.components.premium.PrimaryButton
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onFinish()
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "¿Qué te gusta ver?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Selecciona al menos 3 géneros para personalizar tu cartelera",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Cuadrícula fluida de géneros
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                state.availableGenres.forEach { (genreId, genreName) ->
                    val isSelected = state.selectedGenres.contains(genreId)
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryPurpleLight else SurfaceDark)
                            .clickable { viewModel.toggleGenre(genreId) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genreName,
                            color = if (isSelected) Color.White else TextGray,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            PrimaryButton(
                text = "Empezar a explorar",
                onClick = { viewModel.saveInterests() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                enabled = state.selectedGenres.size >= 3 && !state.isLoading,
                isLoading = state.isLoading
            )
        }
    }
}
