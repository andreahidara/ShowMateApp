package com.example.showmateapp.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.showmateapp.R
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    showId: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val media by viewModel.media.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isWatched by viewModel.isWatched.collectAsState()
    val userRating by viewModel.userRating.collectAsState()

    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId)
    }

    DetailScreenContent(
        media = media,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isFavorite = isFavorite,
        isWatched = isWatched,
        userRating = userRating,
        onBackClick = { navController.popBackStack() },
        onFavoriteClick = { viewModel.toggleFavorite() },
        onWatchedClick = { viewModel.toggleWatched() },
        onRateClick = { viewModel.rateShow(it) },
        onClearRateClick = { viewModel.clearRating() },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreenContent(
    media: MediaContent?,
    isLoading: Boolean,
    errorMessage: String?,
    isFavorite: Boolean,
    isWatched: Boolean,
    userRating: Int?,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (isLoading) {
        PulseLoader()
        return
    }

    if (errorMessage != null) {
        ErrorView(message = errorMessage, onRetry = {})
        return
    }

    val show = media ?: return

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
            val imageUrl = show.posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            
            with(sharedTransitionScope) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = "image-${show.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    placeholder = painterResource(R.drawable.ic_logo_placeholder),
                    error = painterResource(R.drawable.ic_logo_placeholder),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(350.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = show.name,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = 36.sp),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (show.affinityScore > 0f) {
                            MatchBadge(affinityScore = show.affinityScore)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val year = show.firstAirDate?.take(4) ?: "N/A"
                    val seasons = show.numberOfSeasons?.let { "$it ${if (it == 1) "Season" else "Seasons"}" } ?: "N/A"
                    val status = show.status ?: "Unknown"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = year, style = MaterialTheme.typography.titleSmall, color = TextGray)
                        Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = TextGray)
                        Text(text = seasons, style = MaterialTheme.typography.titleSmall, color = TextGray)
                        Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = TextGray)
                        Text(text = status, style = MaterialTheme.typography.titleSmall, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val genresList = show.genres?.map { it.name } ?: emptyList()
                    if (genresList.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genresList) { genre -> GenreChipSmall(text = genre) }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val watchedContainerColor = if (isWatched) Color(0xFF4CAF50) else PrimaryPurple
                        val watchedIcon = Icons.Default.Check
                        
                        Button(
                            onClick = onWatchedClick,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = watchedContainerColor)
                        ) {
                            Icon(watchedIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWatched) stringResource(R.string.detail_watched) else stringResource(R.string.detail_mark_watched), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) HeartRed else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFavorite) stringResource(R.string.detail_remove_favorites) else stringResource(R.string.detail_add_favorites), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    var isSynopsisExpanded by remember { mutableStateOf(false) }
                    
                    Text(
                        text = stringResource(R.string.detail_synopsis),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = show.overview.ifEmpty { stringResource(R.string.detail_no_synopsis) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextGray,
                        lineHeight = 24.sp,
                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                        modifier = Modifier
                            .animateContentSize(animationSpec = tween(durationMillis = 300))
                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                    )
                    
                    if (!isSynopsisExpanded && show.overview.isNotEmpty()) {
                        Text(
                            text = "Read more",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { isSynopsisExpanded = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.detail_rate),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.detail_edit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onClearRateClick() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { index ->
                            val starIndex = index + 1
                            val isSelected = (userRating ?: 0) >= starIndex
                            IconButton(
                                onClick = { onRateClick(starIndex) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate $starIndex stars",
                                    tint = if (isSelected) StarYellow else TextGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (userRating != null) "Tú puntuación: $userRating/5" else "Aún no has puntuado esta serie",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Cast",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        val cast = show.credits?.cast ?: emptyList()
                        items(cast) { member ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(80.dp)
                            ) {
                                val castImageUrl = member.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(castImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = member.name,
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    placeholder = painterResource(R.drawable.ic_logo_placeholder),
                                    error = painterResource(R.drawable.ic_logo_placeholder),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            onClick = onBackClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun GenreChipSmall(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
