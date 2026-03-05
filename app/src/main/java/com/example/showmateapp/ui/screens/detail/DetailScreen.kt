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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.showmateapp.R
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.ui.theme.HeartRed
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun DetailScreen(
    navController: NavController,
    showId: Int,
    viewModel: DetailViewModel = viewModel()
) {
    val tvShow by viewModel.tvShow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId)
    }

    DetailScreenContent(
        tvShow = tvShow,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isFavorite = isFavorite,
        onBackClick = { navController.popBackStack() },
        onFavoriteClick = { viewModel.toggleFavorite() }
    )
}

@Composable
fun DetailScreenContent(
    tvShow: TvShow?,
    isLoading: Boolean,
    errorMessage: String?,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryPurple)
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text(errorMessage, color = HeartRed, fontWeight = FontWeight.Bold)
        }
        return
    }

    val show = tvShow ?: return

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Hero Poster
        Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
            AsyncImage(
                model = "https://images.weserv.nl/?url=https://image.tmdb.org/t/p/original${show.poster_path}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
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

        // Back Action
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

        // Main Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(350.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    // Title and Match Info
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
                        MatchBadge(percentage = 87)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meta Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "2023", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        Text(text = " • ", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        Text(text = "4 Seasons", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        Text(text = " • ", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        Text(text = "In Progress", style = MaterialTheme.typography.bodyMedium, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Genres
                    val genres = listOf("Crime", "Drama", "Mystery", "Thriller")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genres) { genre -> GenreChipSmall(text = genre) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Actions: Mark as Watched & Add to Favorites
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_mark_watched), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Text(stringResource(R.string.detail_add_favorites), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Synopsis Section
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
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Your Rating Section
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
                            modifier = Modifier.clickable { }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(32.dp))
                        }
                        Icon(Icons.Default.StarBorder, contentDescription = null, tint = TextGray, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rated 4/5 based on your taste profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Top Cast Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.detail_top_cast),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.see_all),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        items(listOf(
                            CastMember("Benedict Cumberbatch", "https://image.tmdb.org/t/p/w200/6CH999o9VrTXS799699oTXS7996.jpg"),
                            CastMember("Martin Freeman", "https://image.tmdb.org/t/p/w200/6CH999o9VrTXS799699oTXS7996.jpg"),
                            CastMember("Una Stubbs", "https://image.tmdb.org/t/p/w200/6CH999o9VrTXS799699oTXS7996.jpg"),
                            CastMember("Rupert Graves", "https://image.tmdb.org/t/p/w200/6CH999o9VrTXS799699oTXS7996.jpg")
                        )) { cast ->
                            CastItem(cast)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // "Where to watch" Section (Preserved as requested)
                    Text(
                        text = stringResource(R.string.detail_available_on),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlatformIcon(color = Color(0xFFE50914), label = "Netflix")
                        PlatformIcon(color = Color(0xFF113CCF), label = "Disney+")
                        PlatformIcon(color = Color(0xFF00A8E1), label = "Prime")
                    }
                }
            }
        }
    }
}

data class CastMember(val name: String, val imageUrl: String)

@Composable
fun CastItem(cast: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        AsyncImage(
            model = cast.imageUrl,
            contentDescription = cast.name,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun MatchBadge(percentage: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(60.dp)
            .drawBehindCircle(percentage)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percentage%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "MATCH",
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )
        }
    }
}

fun Modifier.drawBehindCircle(percentage: Int): Modifier = this.then(
    Modifier.background(
        Brush.sweepGradient(
            0f to Color(0xFF6C63FF),
            percentage / 100f to Color(0xFF6C63FF),
            percentage / 100f to Color.Transparent,
            1f to Color.Transparent
        ),
        CircleShape
    ).padding(2.dp).background(Color(0xFF151522), CircleShape)
)

@Composable
fun GenreChipSmall(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PlatformIcon(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = color,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = label.take(1), color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextGray)
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    val sampleTvShow = TvShow(1, "Sherlock", "/62XjU7Yic8Msd5S9vXm2q1oZ0hg.jpg", "A modern update finds the famous sleuth and his doctor partner solving crime in 21st century London.")
    ShowMateAppTheme {
        DetailScreenContent(sampleTvShow, false, null, false, {}, {})
    }
}
