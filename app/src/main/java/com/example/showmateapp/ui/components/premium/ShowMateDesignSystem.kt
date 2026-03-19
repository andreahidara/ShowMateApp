package com.example.showmateapp.ui.components.premium

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import com.example.showmateapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShowCard(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent, String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    showTitle: Boolean = true,
    tag: String = "list"
) {
    val sharedElementKey = "image-${media.id}-$tag"
    
    Column(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .clickable { onClick(media, tag) }
    ) {
        val imageUrl = media.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
        ) {
            with(sharedTransitionScope) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = media.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = sharedElementKey),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    placeholder = painterResource(R.drawable.ic_logo_placeholder),
                    error = painterResource(R.drawable.ic_logo_placeholder),
                    contentScale = ContentScale.Crop
                )
            }

            // Bottom gradient so the badge reads well
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.15f)),
                            startY = 0f
                        )
                    )
            )

            if (media.affinityScore > 0f) {
                MatchBadge(
                    score = media.affinityScore,
                    isAffinity = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            } else if (media.voteAverage > 0f) {
                MatchBadge(
                    score = media.voteAverage,
                    isAffinity = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        
        if (showTitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MatchBadge(
    score: Float,
    isAffinity: Boolean,
    modifier: Modifier = Modifier
) {
    val percentage = (score * 10).toInt().coerceIn(0, 100)
    val displayValue = if (isAffinity) "$percentage% Match" else "${"%.1f".format(score)} ★"
    
    val matchColor = when {
        isAffinity && percentage >= 80 -> Color(0xFF4CAF50) // High affinity
        isAffinity && percentage >= 50 -> Color(0xFFFFC107) // Medium affinity
        !isAffinity && score >= 7.5 -> Color(0xFFFFD700) // Quality star
        else -> Color.White.copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayValue,
            color = matchColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShowSection(
    title: String,
    items: List<MediaContent>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaContent, String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "list"
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryPurple)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.id }) { item ->
                ShowCard(
                    media = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onItemClick,
                    tag = tag
                )
            }
        }
    }
}

@Composable
fun PulseLoader(modifier: Modifier = Modifier) {
    val circles = listOf(
        remember { Animatable(0.3f) },
        remember { Animatable(0.3f) },
        remember { Animatable(0.3f) }
    )

    circles.forEachIndexed { index, animatable ->
        LaunchedEffect(Unit) {
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            circles.forEach { animatable ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(animatable.value)
                        .background(PrimaryPurple, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = PrimaryPurple.copy(alpha = 0.8f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Ups! Algo salió mal",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TextGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text("Reintentar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
