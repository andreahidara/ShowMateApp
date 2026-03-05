package com.example.showmateapp.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun OnboardingScreen(navController: NavController) {
    val viewModel: OnboardingViewModel = viewModel()
    val genres = viewModel.genres
    val selectedGenres by viewModel.selectedGenres.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(PrimaryPurple.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) PrimaryPurple else Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Tus gustos,\ntu experiencia",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 42.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Selecciona tus géneros favoritos para que podamos recomendarte las mejores series.",
                color = TextGray,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Genre Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenres.contains(genre.id)
                    GenreCard(
                        name = genre.name,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleGenre(genre.id) }
                    )
                }
            }

            // Bottom Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val canContinue = selectedGenres.isNotEmpty()

                Button(
                    onClick = {
                        if (canContinue) {
                            val genreString = selectedGenres.joinToString(",")
                            navController.navigate("swipe/$genreString")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canContinue) PrimaryPurple else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canContinue
                ) {
                    Text(
                        text = if (canContinue) "Continuar" else "Selecciona al menos uno",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canContinue) Color.White else TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun GenreCard(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else SurfaceDark,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple else Color.Transparent,
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = if (isSelected) Color.White else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
