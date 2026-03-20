package com.example.showmateapp.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

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
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Personaliza tu experiencia",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Selecciona al menos 3 géneros que te apasionen para calibrar tu algoritmo.",
                color = TextGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            GenreProgressIndicator(state.selectedGenres.size)

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.availableGenres.toList()) { (id, name) ->
                    val isSelected = state.selectedGenres.contains(id)
                    val posterPath = state.genrePosters[id]
                    GenreCard(
                        name = name,
                        isSelected = isSelected,
                        posterPath = posterPath,
                        onClick = { viewModel.toggleGenre(id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveInterests() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(bottom = 8.dp),
                enabled = state.selectedGenres.size >= 3 && !state.isLoading,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Continuar",
                        color = if (state.selectedGenres.size >= 3) Color.Black else Color.White.copy(alpha = 0.3f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
fun GenreCard(
    name: String,
    isSelected: Boolean,
    posterPath: String? = null,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple else Color.Transparent,
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster background image
            if (posterPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://image.tmdb.org/t/p/w342$posterPath")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                )
            }

            // Dark scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = if (isSelected) 0.35f else 0.55f),
                                Color.Black.copy(alpha = if (isSelected) 0.65f else 0.75f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            // Text + check overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun GenreProgressIndicator(selectedCount: Int) {
    val progress = (selectedCount.toFloat() / 3f).coerceIn(0f, 1f)
    val animatedProgress by animateDpAsState(
        targetValue = (progress * 100).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$selectedCount",
                color = if (selectedCount >= 3) Color(0xFF4CAF50) else PrimaryPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedCount >= 3) "seleccionados · ¡Perfecto!" else "seleccionados (mínimo 3)",
                color = TextGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(animatedProgress * 2)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryPurple, Color(0xFF9C27B0))
                        )
                    )
            )
        }
    }
}
