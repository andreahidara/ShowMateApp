package com.example.showmateapp.ui.screens.swipe

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.R
import com.example.showmateapp.ui.navigation.Screen
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.components.premium.MatchBadge
import com.example.showmateapp.ui.theme.*
import com.example.showmateapp.util.GenreMapper
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun SwipeScreen(navController: NavController) {
    val viewModel: SwipeViewModel = hiltViewModel()
    val showsToRate by viewModel.shows.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val ratedCount by viewModel.ratedCount.collectAsState()
    val lastRemovedShow by viewModel.lastRemovedShow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadShows()
    }

    SwipeScreenContent(
        showsToRate = showsToRate,
        errorMessage = errorMessage,
        isLoading = isLoading,
        ratedCount = ratedCount,
        lastRemovedShow = lastRemovedShow,
        onLikeShow = { viewModel.likeTopShow() },
        onSkipShow = { viewModel.skipTopShow() },
        onUndoAction = { viewModel.undoLastAction() },
        onNavigateToHome = {
            navController.navigate(Screen.Main) {
                popUpTo(Screen.Swipe) { inclusive = true }
            }
        }
    )
}

@Composable
fun SwipeScreenContent(
    showsToRate: List<MediaContent>,
    errorMessage: String?,
    isLoading: Boolean,
    ratedCount: Int,
    lastRemovedShow: MediaContent?,
    onLikeShow: () -> Unit,
    onSkipShow: () -> Unit,
    onUndoAction: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val maxRatings = 10

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(64.dp))
        }
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage,
                color = HeartRed,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Para ti",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Desliza para calibrar tus gustos",
                    color = TextGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (ratedCount.toFloat() / maxRatings.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.size(60.dp),
                    color = PrimaryPurple,
                    strokeWidth = 5.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$ratedCount",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "/$maxRatings",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (ratedCount >= maxRatings) {
                SuccessState(onNavigateToHome)
            } else if (showsToRate.isEmpty()) {
                Text("No hay más series por ahora", color = TextGray)
            } else {
                val stackLimit = 3
                val visibleShows = showsToRate.take(stackLimit).reversed()
                
                visibleShows.forEachIndexed { index, show ->
                    val stackIndex = visibleShows.size - 1 - index
                    key(show.id) {
                        SwipeableCard(
                            media = show,
                            stackIndex = stackIndex,
                            onSwiped = { isLiked ->
                                // ratedCount is incremented inside likeTopShow/skipTopShow in ViewModel
                                if (isLiked) onLikeShow() else onSkipShow()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (ratedCount < maxRatings && showsToRate.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onSkipShow() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Paso", tint = HeartRed, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Paso", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onUndoAction() },
                        enabled = lastRemovedShow != null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.White.copy(alpha = if (lastRemovedShow != null) 0.15f else 0.05f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Deshacer",
                            tint = if (lastRemovedShow != null) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Deshacer",
                        color = if (lastRemovedShow != null) TextGray else TextGray.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onLikeShow() },
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(20.dp, CircleShape, spotColor = PrimaryPurple)
                            .background(
                                Brush.linearGradient(listOf(PrimaryPurple, Color(0xFF9C27B0))),
                                CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Me gusta", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Me gusta", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SuccessState(onNavigateToHome: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.1f))
                .border(2.dp, PrimaryPurple.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "¡Todo listo!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Hemos calibrado tus gustos. Ahora ShowMate es mucho más inteligente para ti.",
            color = TextGray,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToHome,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = "Empezar a explorar",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun SwipeableCard(
    media: MediaContent,
    stackIndex: Int,
    onSwiped: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val isTopCard = stackIndex == 0

    val scale by animateFloatAsState(targetValue = 1f - (stackIndex * 0.05f), label = "scale")
    val translateY by animateFloatAsState(targetValue = (stackIndex * -15f), label = "translateY")
    val alpha by animateFloatAsState(targetValue = 1f - (stackIndex * 0.3f), label = "alpha")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = translateY.dp.toPx()
                scaleX = scale
                scaleY = scale
                rotationZ = offsetX.value / 25f
                this.alpha = alpha
            }
            .pointerInput(isTopCard) {
                // Key = isTopCard so this block restarts when a card becomes/stops being top
                if (isTopCard) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX.value.absoluteValue > 500f) {
                                val isLiked = offsetX.value > 0
                                scope.launch {
                                    offsetX.animateTo(
                                        if (isLiked) 1500f else -1500f,
                                        tween(350)
                                    )
                                    onSwiped(isLiked)
                                }
                            } else {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        }
                    )
                }
            }
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceDark)
            .shadow(if (isTopCard) 20.dp else 0.dp, RoundedCornerShape(32.dp))
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w780${media.posterPath}",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_logo_placeholder)
        )

        if (isTopCard && offsetX.value != 0f) {
            val swipeAlpha = (offsetX.value.absoluteValue / 300f).coerceIn(0f, 0.45f)
            val isLiking = offsetX.value > 0
            val overlayColor = if (isLiking) Color(0xFF4CAF50) else HeartRed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = swipeAlpha))
            )
            val labelAlpha = ((offsetX.value.absoluteValue - 80f) / 200f).coerceIn(0f, 1f)
            if (labelAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(if (isLiking) Alignment.TopStart else Alignment.TopEnd)
                        .padding(28.dp)
                        .alpha(labelAlpha)
                        .clip(RoundedCornerShape(8.dp))
                        .border(3.dp, overlayColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isLiking) "ME GUSTA" else "PASO",
                        color = overlayColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        if (media.affinityScore > 0f) {
            MatchBadge(
                score = media.affinityScore,
                isAffinity = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 400f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val year = media.firstAirDate?.take(4)?.takeIf { it.isNotBlank() }
            val runtime = media.episodeRunTime?.firstOrNull()?.takeIf { it > 0 }?.let { "$it min" }
            val seasons = media.numberOfSeasons?.takeIf { it > 0 }?.let { "$it temp." }
            val metaParts = listOfNotNull(year, runtime, seasons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                metaParts.forEachIndexed { index, part ->
                    if (index > 0) {
                        Text("·", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                    }
                    Text(part, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                if (media.voteAverage > 0f) {
                    if (metaParts.isNotEmpty()) {
                        Text("·", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                    }
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(media.voteAverage),
                        color = StarYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Genre chips
            val genreNames = media.safeGenreIds.take(3).map { GenreMapper.getGenreName(it.toString()) }
            if (genreNames.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genreNames.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = genre,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = media.overview,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
        }
    }
}
