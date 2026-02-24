package com.example.showmateapp.ui.screens.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun SwipeScreen(navController: NavController, selectedGenres: String) {
    val viewModel: SwipeViewModel = viewModel()
    val showsToRate by viewModel.shows.collectAsState()

    var ratedCount by remember { mutableIntStateOf(0) }
    val maxRatings = 5

    LaunchedEffect(selectedGenres) {
        viewModel.loadShows(selectedGenres)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Discover Series", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Personalized for you", color = TextGray, fontSize = 12.sp)
            }
            Text("$ratedCount/$maxRatings", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { ratedCount.toFloat() / maxRatings.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = PrimaryPurple,
            trackColor = SurfaceDark
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f), contentAlignment = Alignment.Center) {
            if (ratedCount >= maxRatings) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¡Awesome!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("We've tailored your feed.", color = TextGray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("home") { popUpTo("swipe") { inclusive = true } } },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Home", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (showsToRate.isEmpty()) {
                CircularProgressIndicator(color = PrimaryPurple)
            } else {
                showsToRate.reversed().forEach { show ->
                    key(show.id) {
                        SwipeableCard(show = show, onSwiped = { viewModel.removeTopShow(); ratedCount++ })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (ratedCount < maxRatings) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { if (showsToRate.isNotEmpty()) { viewModel.removeTopShow(); ratedCount++ } }, modifier = Modifier.size(64.dp).background(SurfaceDark, CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { if (showsToRate.isNotEmpty()) { viewModel.removeTopShow(); ratedCount++ } }, modifier = Modifier.size(64.dp).background(PrimaryPurple, CircleShape)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(show: Movie, onSwiped: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = offsetX.value; rotationZ = offsetX.value / 25f }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX.value.absoluteValue > 400f) {
                            scope.launch {
                                offsetX.animateTo(if (offsetX.value > 0) 1500f else -1500f, tween(400))
                                onSwiped()
                            }
                        } else { scope.launch { offsetX.animateTo(0f, tween(400)) } }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                    }
                )
            }
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
    ) {
        AsyncImage(
            model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/w500${show.poster_path}",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 700f)))
        Text(text = show.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(24.dp))
    }
}