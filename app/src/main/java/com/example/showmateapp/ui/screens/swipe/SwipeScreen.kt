package com.example.showmateapp.ui.screens.swipe

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.navigation.Screen
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.components.premium.MatchBadge
import com.example.showmateapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SwipeScreen(navController: NavController) {
    val viewModel: SwipeViewModel = hiltViewModel()
    val showsToRate by viewModel.shows.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadShows()
    }

    SwipeScreenContent(
        showsToRate = showsToRate,
        errorMessage = errorMessage,
        isLoading = isLoading,
        onLikeShow = { viewModel.likeTopShow() },
        onSkipShow = { viewModel.skipTopShow() },
        onNavigateToHome = {
            navController.navigate(Screen.Main) { 
                popUpTo<Screen.Swipe> { inclusive = true } 
            }
        }
    )
}

@Composable
fun SwipeScreenContent(
    showsToRate: List<MediaContent>,
    errorMessage: String?,
    isLoading: Boolean,
    onLikeShow: () -> Unit,
    onSkipShow: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var ratedCount by remember { mutableIntStateOf(0) }
    val maxRatings = 10

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryPurpleLight, modifier = Modifier.size(64.dp))
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
                    text = "Discover",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Handpicked for you",
                    color = TextGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { ratedCount.DivideSafe(maxRatings) },
                    modifier = Modifier.size(56.dp),
                    color = PrimaryPurpleLight,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    text = "$ratedCount/$maxRatings",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
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
                Text("No more shows to discover", color = TextGray)
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
                                if (isLiked) onLikeShow() else onSkipShow()
                                ratedCount++ 
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
                ActionButton(
                    icon = Icons.Default.Close,
                    color = Color.White,
                    backgroundColor = SurfaceDark,
                    size = 64.dp,
                    onClick = { onSkipShow(); ratedCount++ }
                )
                ActionButton(
                    icon = Icons.Default.Favorite,
                    color = Color.White,
                    backgroundColor = PrimaryPurple,
                    size = 80.dp,
                    onClick = { onLikeShow(); ratedCount++ }
                )
            }
        }
    }
}

fun Int.DivideSafe(divisor: Int): Float = if (divisor == 0) 0f else this.toFloat() / divisor.toFloat()

@Composable
fun SuccessState(onNavigateToHome: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            color = PrimaryPurple.copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "¡Perfect Match!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your feed has been calibrated. Get ready for your next obsession.",
            color = TextGray,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToHome,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PrimaryPurple)
        ) {
            Text(
                text = "Continue to Home",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    color: Color,
    backgroundColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .shadow(16.dp, CircleShape)
            .background(backgroundColor, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.45f)
        )
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
            .padding(bottom = 16.dp)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = translateY.dp.toPx()
                scaleX = scale
                scaleY = scale
                rotationZ = offsetX.value / 25f
                this.alpha = alpha
            }
            .pointerInput(Unit) {
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
            .shadow(if (isTopCard) 12.dp else 0.dp, RoundedCornerShape(32.dp))
    ) {
        AsyncImage(
            model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/w780${media.posterPath}",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isTopCard) {
            val swipeAlpha = (offsetX.value.absoluteValue / 300f).coerceIn(0f, 1f)
            val overlayColor = if (offsetX.value > 0) PrimaryPurple else HeartRed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = swipeAlpha * 0.2f))
            )
        }

        if (media.affinityScore > 0f) {
            MatchBadge(
                affinityScore = media.affinityScore,
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
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 500f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SERIES",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Top Trend",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = media.overview,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SwipeScreenPreview() {
    val sampleMediaContent = listOf(
        MediaContent(
            id = 1, 
            name = "The Mandalorian", 
            posterPath = "/62XjU7Yic8Msd5S9vXm2q1oZ0hg.jpg", 
            overview = "After the fall of the Galactic Empire, a lone gunfighter makes his way through the outer reaches of the lawless galaxy."
        ),
        MediaContent(
            id = 2, 
            name = "Breaking Bad", 
            posterPath = "/ggm8fbIlUBYm9XDVp9qUqMvM3S0.jpg", 
            overview = "A high school chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine."
        ),
        MediaContent(
            id = 3, 
            name = "Stranger Things", 
            posterPath = "/x2LSRm21uTEx8P9uS4NiYszix9b.jpg",
            overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl."
        )
    )
    ShowMateAppTheme {
        SwipeScreenContent(
            showsToRate = sampleMediaContent,
            errorMessage = null,
            isLoading = false,
            onLikeShow = {},
            onSkipShow = {},
            onNavigateToHome = {}
        )
    }
}
