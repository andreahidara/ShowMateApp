package com.andrea.showmateapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.theme.ShowMateAppTheme
import org.junit.Rule
import org.junit.Test

class HomeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
        maxPercentDifference = 0.1
    )

    private val fakeShows = listOf(
        MediaContent(
            id = 1,
            name = "Breaking Bad",
            overview = "A chemistry teacher turns to crime after a cancer diagnosis.",
            posterPath = null,
            backdropPath = null,
            voteAverage = 9.5f,
            firstAirDate = "2008-01-20",
            numberOfSeasons = 5,
            genres = emptyList(),
            popularity = 9.0f,
            affinityScore = 0.95f
        ),
        MediaContent(
            id = 2,
            name = "The Wire",
            overview = "Crime drama set in Baltimore.",
            posterPath = null,
            backdropPath = null,
            voteAverage = 9.3f,
            firstAirDate = "2002-06-02",
            numberOfSeasons = 5,
            genres = emptyList(),
            popularity = 8.5f,
            affinityScore = 0.88f
        ),
        MediaContent(
            id = 3,
            name = "Ozark",
            overview = "A financial advisor drags his family from Chicago to the Missouri Ozarks.",
            posterPath = null,
            backdropPath = null,
            voteAverage = 8.4f,
            firstAirDate = "2017-07-21",
            numberOfSeasons = 4,
            genres = emptyList(),
            popularity = 8.0f,
            affinityScore = 0.80f
        )
    )

    @Composable
    private fun SimpleFeaturedBanner(shows: List<MediaContent>) {
        val media = shows.firstOrNull() ?: return
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "%.1f ★".format(media.voteAverage),
                    color = Color(0xFFFFC107),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = media.name,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = media.overview,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun SimpleTop10Section(shows: List<MediaContent>) {
        Column {
            Text(
                text = "Top 10 esta semana",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shows.take(10), key = { it.id }) { show ->
                    val rank = shows.indexOf(show) + 1
                    Box(modifier = Modifier.width(130.dp).height(190.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2A1F3D), RoundedCornerShape(14.dp))
                        )
                        Text(
                            text = "$rank",
                            style = TextStyle(color = Color.White),
                            fontSize = if (rank < 10) 74.sp else 60.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        )
                        Text(
                            text = show.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testFeaturedBanner() {
        paparazzi.snapshot {
            ShowMateAppTheme {
                Surface(color = Color(0xFF0D0D0D)) {
                    SimpleFeaturedBanner(shows = fakeShows)
                }
            }
        }
    }

    @Test
    fun testTop10Section() {
        paparazzi.snapshot {
            ShowMateAppTheme {
                Surface(color = Color(0xFF0D0D0D)) {
                    SimpleTop10Section(shows = fakeShows)
                }
            }
        }
    }
}
