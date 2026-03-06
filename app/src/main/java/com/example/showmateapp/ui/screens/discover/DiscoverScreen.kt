package com.example.showmateapp.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.ui.components.BottomNavBar
import com.example.showmateapp.ui.screens.home.SeriesRow
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.ShowMateAppTheme
import com.example.showmateapp.ui.theme.StarYellow

@Composable
fun DiscoverScreen(
    globalNavController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val heroShow by viewModel.heroShow.collectAsState()
    val euphoria by viewModel.euphoriaRecommendations.collectAsState()
    val hiddenGems by viewModel.hiddenGems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    DiscoverScreenContent(
        heroShow = heroShow,
        euphoria = euphoria,
        hiddenGems = hiddenGems,
        isLoading = isLoading,
        onTvShowClick = { tvShow -> navigateToDetail(globalNavController, tvShow) }
    )
}

@Composable
fun DiscoverScreenContent(
    heroShow: TvShow?,
    euphoria: List<TvShow>,
    hiddenGems: List<TvShow>,
    isLoading: Boolean,
    onTvShowClick: (TvShow) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = PrimaryPurple,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    heroShow?.let {
                        DiscoverHeroSection(it) { tvShow ->
                            onTvShowClick(tvShow)
                        }
                    }
                }

                item {
                    Text(
                        text = "Porque te gustó Euphoria",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                    )
                    SeriesRow(euphoria) { tvShow ->
                        onTvShowClick(tvShow)
                    }
                }

                item {
                    Text(
                        text = "Joyas Ocultas",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                    )
                    SeriesRow(hiddenGems) { tvShow ->
                        onTvShowClick(tvShow)
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun DiscoverHeroSection(tvShow: TvShow, onClick: (TvShow) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .clickable { onClick(tvShow) }
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/original${tvShow.posterPath}",
            contentDescription = "Hero: ${tvShow.name}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = StarYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Recomendación del Día",
                    color = StarYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tvShow.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onClick(tvShow) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Detalles", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun navigateToDetail(navController: NavController, tvShow: TvShow) {
    navController.navigate("detail/${tvShow.id}")
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiscoverScreenPreview() {
    val sampleTvShows = listOf(
        TvShow(
            id = 1,
            name = "The Mandalorian",
            posterPath = "/62XjU7Yic8Msd5S9vXm2q1oZ0hg.jpg",
            overview = "After the fall of the Galactic Empire, a lone gunfighter makes his way through the outer reaches of the lawless galaxy."
        ),
        TvShow(
            id = 2,
            name = "Breaking Bad",
            posterPath = "/ggm8fbIlUBYm9XDVp9qUqMvM3S0.jpg",
            overview = "A high school chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine."
        ),
        TvShow(
            id = 3,
            name = "Stranger Things",
            posterPath = "/x2LSRm21uTEx8P9uS4NiYszix9b.jpg",
            overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl."
        )
    )
    val navController = rememberNavController()
    ShowMateAppTheme {
        Scaffold(
            bottomBar = { BottomNavBar(navController = navController) }
        ) { paddingValues ->
            DiscoverScreenContent(
                heroShow = sampleTvShows.first(),
                euphoria = sampleTvShows,
                hiddenGems = sampleTvShows.reversed(),
                isLoading = false,
                onTvShowClick = {},
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiscoverHeroSectionPreview() {
    val sampleTvShow = TvShow(
        id = 1,
        name = "The Mandalorian",
        posterPath = "/62XjU7Yic8Msd5S9vXm2q1oZ0hg.jpg",
        overview = "After the fall of the Galactic Empire, a lone gunfighter makes his way through the outer reaches of the lawless galaxy."
    )
    ShowMateAppTheme {
        DiscoverHeroSection(tvShow = sampleTvShow, onClick = {})
    }
}
