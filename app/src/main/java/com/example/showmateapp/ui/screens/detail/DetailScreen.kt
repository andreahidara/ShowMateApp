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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import com.example.showmateapp.data.network.Provider
import com.example.showmateapp.ui.components.premium.*
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalUriHandler
import com.example.showmateapp.data.network.CountryProviders
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    showId: Int,
    sharedElementTag: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId)
    }

    DetailScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onLikeClick = { viewModel.toggleLiked() },
        onEssentialClick = { viewModel.toggleEssential() },
        onWatchedClick = { viewModel.toggleWatched() },
        onRateClick = { viewModel.rateShow(it) },
        onClearRateClick = { viewModel.clearRating() },
        onRetry = { viewModel.loadShowDetails(showId) },
        onSimilarShowClick = { show, tag ->
            navController.navigate(Screen.Detail(showId = show.id, sharedElementTag = tag))
        },
        onEpisodeToggle = { viewModel.toggleEpisodeWatched(it) },
        onSeasonChange = { showId, seasonNum -> viewModel.loadSeasonDetails(showId, seasonNum) },
        onClearActionError = { viewModel.clearActionError() },
        onReviewTextChange = { viewModel.onReviewTextChange(it) },
        onSaveReview = { viewModel.saveReview() },
        onDeleteReview = { viewModel.deleteReview() },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedElementTag = sharedElementTag
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreenContent(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEssentialClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    onRetry: () -> Unit,
    onSimilarShowClick: (MediaContent, String) -> Unit,
    onEpisodeToggle: (Int) -> Unit,
    onSeasonChange: (Int, Int) -> Unit,
    onClearActionError: () -> Unit,
    onReviewTextChange: (String) -> Unit = {},
    onSaveReview: () -> Unit = {},
    onDeleteReview: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedElementTag: String?
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                onClearActionError()
            }
        }
    }

    if (uiState.isLoading) {
        PulseLoader()
        return
    }

    if (uiState.errorMessage != null) {
        ErrorView(message = uiState.errorMessage, onRetry = onRetry)
        return
    }

    val show = uiState.media ?: return

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Imagen de fondo con degradado
        Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
            val imageUrl = remember(show.backdropPath, show.posterPath) {
                (show.backdropPath ?: show.posterPath)?.let { "https://image.tmdb.org/t/p/w1280$it" }
            }
            
            with(sharedTransitionScope) {
                val sharedElementKey = if (sharedElementTag != null) {
                    "image-${show.id}-$sharedElementTag"
                } else {
                    "image-${show.id}"
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
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
            
            val backgroundGradient = remember {
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(backgroundGradient)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(320.dp))
            }
            
            item {
                // Panel de contenido con fondo sólido
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
                        // Título y Badge de Afinidad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = show.name,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = 34.sp),
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (show.affinityScore > 0f) {
                                MatchBadge(score = show.affinityScore, isAffinity = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Info básica
                        val year = remember(show.firstAirDate) { show.firstAirDate?.take(4) ?: "N/A" }
                        val seasons = remember(show.numberOfSeasons) { show.numberOfSeasons?.let { "$it ${if (it == 1) "Temporada" else "Temporadas"}" } ?: "N/A" }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = year, style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = seasons, style = MaterialTheme.typography.titleSmall, color = TextGray)
                            Text(text = " • ", style = MaterialTheme.typography.titleSmall, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botones de acción principales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // BOTÓN VISTO
                            val watchedColor by animateColorAsState(
                                targetValue = if (uiState.isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f),
                                label = "watchedColor"
                            )
                            ActionIconButton(
                                icon = Icons.Default.Check,
                                text = if (uiState.isWatched) "Vista" else "Marcar vista",
                                containerColor = watchedColor,
                                contentColor = if (uiState.isWatched) Color.White else TextGray,
                                modifier = Modifier.weight(1.2f),
                                onClick = onWatchedClick
                            )

                            // BOTÓN ME GUSTA
                            val likeColor by animateColorAsState(
                                targetValue = if (uiState.isLiked) HeartRed else Color.White.copy(alpha = 0.1f),
                                label = "likeColor"
                            )
                            ActionIconButton(
                                icon = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                text = "Me gusta",
                                containerColor = likeColor,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = onLikeClick
                            )

                            // BOTÓN IMPRESCINDIBLE (Estrella)
                            val essentialColor by animateColorAsState(
                                targetValue = if (uiState.isEssential) StarYellow else Color.White.copy(alpha = 0.1f),
                                label = "essentialColor"
                            )
                            ActionIconButton(
                                icon = if (uiState.isEssential) Icons.Default.Star else Icons.Default.StarBorder,
                                text = "Top",
                                containerColor = essentialColor,
                                contentColor = Color.White,
                                modifier = Modifier.weight(0.8f),
                                onClick = onEssentialClick
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Botón de Tráiler Oficial
                        val trailerKey = show.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.key
                        if (trailerKey != null) {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Button(
                                onClick = { uriHandler.openUri("https://www.youtube.com/watch?v=$trailerKey") },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver Tráiler Oficial", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Dónde ver (Watch Providers via JustWatch / TMDB)
                        val esProviders = show.watchProviders?.results?.get("ES")
                        if (esProviders != null &&
                            (!esProviders.flatrate.isNullOrEmpty() ||
                             !esProviders.rent.isNullOrEmpty() ||
                             !esProviders.buy.isNullOrEmpty())
                        ) {
                            WatchProvidersSection(providers = esProviders, showName = show.name)
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Sinopsis
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
                                .animateContentSize()
                                .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Reseña del usuario
                        ReviewSection(
                            reviewText = uiState.userReview,
                            userRating = uiState.userRating,
                            isSaving = uiState.isSavingReview,
                            isReviewSaved = uiState.isReviewSaved,
                            onTextChange = onReviewTextChange,
                            onSave = onSaveReview,
                            onDelete = onDeleteReview,
                            onRateClick = onRateClick,
                            onClearRateClick = onClearRateClick
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Registro de episodios (Solo si hay temporadas)
                        if (!show.seasons.isNullOrEmpty()) {
                            EpisodesSection(
                                seasons = show.seasons,
                                selectedSeason = uiState.selectedSeason,
                                watchedEpisodes = uiState.watchedEpisodes,
                                onEpisodeToggle = onEpisodeToggle,
                                onSeasonChange = { seasonNum -> 
                                    (uiState.media?.id)?.let { id -> 
                                        onSeasonChange(id, seasonNum)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Reparto
                        Text(
                            text = stringResource(R.string.detail_top_cast),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val cast = show.credits?.cast ?: emptyList()
                            items(cast.take(10), key = { it.id }) { member ->
                                CastMemberItem(member)
                            }
                        }

                        if (uiState.similarShows.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Te puede gustar",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(uiState.similarShows, key = { it.id }) { similarShow ->
                                    ShowCard(
                                        media = similarShow,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onClick = { s, t -> onSimilarShowClick(s, t) },
                                        tag = "similar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botón atrás
        Surface(
            onClick = onBackClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Maps a TMDB provider ID to its direct search URL for a given show name. */
private fun providerSearchUrl(providerId: Int, showName: String): String? {
    val encoded = java.net.URLEncoder.encode(showName, "UTF-8")
    return when (providerId) {
        8, 213 -> "https://www.netflix.com/search?q=$encoded"           // Netflix
        9, 119  -> "https://www.primevideo.com/search?k=$encoded"        // Amazon Prime Video
        337     -> "https://www.disneyplus.com/es-es/search/$encoded"    // Disney+
        384, 29 -> "https://play.max.com/search?q=$encoded"              // Max (HBO)
        2, 350  -> "https://tv.apple.com/search?term=$encoded"           // Apple TV+
        531     -> "https://www.paramountplus.com/es/search/$encoded/"   // Paramount+
        1773    -> "https://www.skyshowtime.com/es/search?q=$encoded"    // SkyShowtime
        63      -> "https://www.filmin.es/buscar?q=$encoded"             // Filmin
        149     -> "https://ver.movistarplus.es/buscar/?q=$encoded"      // Movistar+
        35, 105 -> "https://www.rakuten.tv/es/search?q=$encoded"         // Rakuten TV
        541     -> "https://www.atresplayer.com/buscar/?q=$encoded"      // Atresplayer
        566     -> "https://www.rtve.es/play/buscar/?q=$encoded"         // RTVE Play
        188     -> "https://www.youtube.com/results?search_query=$encoded" // YouTube Premium
        else    -> null
    }
}

@Composable
fun WatchProvidersSection(providers: CountryProviders, showName: String) {
    val uriHandler = LocalUriHandler.current
    val jwLink = providers.link

    Column {
        Text(
            text = "¿Dónde verlo?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!providers.flatrate.isNullOrEmpty()) {
            ProviderTypeRow(
                label = "En suscripción",
                providers = providers.flatrate,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        if (!providers.rent.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = "De alquiler",
                providers = providers.rent,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        if (!providers.buy.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = "De compra",
                providers = providers.buy,
                showName = showName,
                fallbackUrl = jwLink
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Datos de disponibilidad: JustWatch",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
        )
    }
}

@Composable
fun ProviderTypeRow(
    label: String,
    providers: List<Provider>,
    showName: String,
    fallbackUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = TextGray,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(providers, key = { it.providerId }) { provider ->
            val url = providerSearchUrl(provider.providerId, showName) ?: fallbackUrl
            ProviderLogo(
                provider = provider,
                onClick = { url?.let { uriHandler.openUri(it) } }
            )
        }
    }
}

@Composable
fun ProviderLogo(provider: Provider, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    AsyncImage(
        model = "https://image.tmdb.org/t/p/original${provider.logoPath}",
        contentDescription = provider.providerName,
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun CastMemberItem(member: com.example.showmateapp.data.network.CastMember, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(80.dp)) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w185${member.profilePath}",
            contentDescription = member.name,
            modifier = Modifier.size(80.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_logo_placeholder)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = member.name, style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}

private const val REVIEW_MAX_CHARS = 500

@Composable
fun ReviewSection(
    reviewText: String,
    userRating: Int?,
    isSaving: Boolean,
    isReviewSaved: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Star rating: 5 visual stars → stored as 1-10 internally (star N = N*2)
    val starCount = 5
    val visualRating = userRating?.let { (it + 1) / 2 } ?: 0

    Column(modifier = modifier) {
        Text(
            text = "Mi valoración",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Star selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(starCount) { index ->
                        val filled = index < visualRating
                        Icon(
                            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Estrella ${index + 1}",
                            tint = if (filled) StarYellow else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    val newVisual = index + 1
                                    if (newVisual == visualRating) onClearRateClick()
                                    else onRateClick(newVisual * 2)
                                }
                        )
                    }
                    if (userRating != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$userRating/10",
                            color = StarYellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            text = "Sin valorar",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(20.dp))

                // Text field
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { if (it.length <= REVIEW_MAX_CHARS) onTextChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Escribe tu opinión sobre esta serie...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 7,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PrimaryPurple,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
                )

                // Counter + actions row
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Character counter
                    Text(
                        text = "${reviewText.length}/$REVIEW_MAX_CHARS",
                        color = if (reviewText.length > REVIEW_MAX_CHARS * 0.9) StarYellow else Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Delete button (only if there's saved text)
                    if (reviewText.isNotBlank()) {
                        TextButton(onClick = onDelete) {
                            Text("Borrar", color = Color.Red.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Save button
                    Button(
                        onClick = onSave,
                        enabled = reviewText.isNotBlank() && !isSaving && !isReviewSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReviewSaved) Color(0xFF4CAF50) else PrimaryPurple,
                            disabledContainerColor = if (isReviewSaved) Color(0xFF4CAF50).copy(alpha = 0.7f)
                                                     else PrimaryPurple.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        when {
                            isSaving -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            isReviewSaved -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardada", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            else -> Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodesSection(
    seasons: List<com.example.showmateapp.data.network.Season>,
    selectedSeason: com.example.showmateapp.data.network.SeasonResponse?,
    watchedEpisodes: List<Int>,
    modifier: Modifier = Modifier,
    onEpisodeToggle: (Int) -> Unit,
    onSeasonChange: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = "Episodios",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Selector de Temporadas
        var selectedTabIndex by remember(selectedSeason) { 
            mutableIntStateOf(seasons.indexOfFirst { it.seasonNumber == selectedSeason?.season_number }.coerceAtLeast(0)) 
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryPurple
                    )
                }
            },
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
        ) {
            seasons.filter { it.seasonNumber > 0 }.forEachIndexed { index, season ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        onSeasonChange(season.seasonNumber)
                    },
                    text = { 
                        Text(
                            text = "T${season.seasonNumber}", 
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) PrimaryPurple else TextGray
                        ) 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Season progress bar
        if (selectedSeason != null && selectedSeason.episodes.isNotEmpty()) {
            val totalEps = selectedSeason.episodes.size
            val watchedInSeason = selectedSeason.episodes.count { watchedEpisodes.contains(it.id) }
            val progress = watchedInSeason.toFloat() / totalEps.toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso de temporada",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$watchedInSeason / $totalEps ep",
                        color = if (watchedInSeason == totalEps) Color(0xFF4CAF50) else PrimaryPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (watchedInSeason == totalEps) Color(0xFF4CAF50) else PrimaryPurple,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista de episodios
        if (selectedSeason != null) {
            selectedSeason.episodes.forEach { episode ->
                val isWatched = watchedEpisodes.contains(episode.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onEpisodeToggle(episode.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w300${episode.still_path}",
                        contentDescription = null,
                        modifier = Modifier.size(width = 100.dp, height = 60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${episode.episode_number}. ${episode.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${episode.runtime ?: 0} min", color = TextGray, fontSize = 12.sp)
                    }
                    Checkbox(
                        checked = isWatched,
                        onCheckedChange = { onEpisodeToggle(episode.id) },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                    )
                }
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryPurple)
        }
    }
}
