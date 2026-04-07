package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.util.TmdbUtils
import androidx.compose.ui.res.stringResource
import com.andrea.showmateapp.R

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpNextSection(
    shows: List<MediaContent>,
    progressMap: Map<Int, Float>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaContent, String) -> Unit
) {
    if (shows.isEmpty()) return
    Column {
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
                text = stringResource(R.string.home_continue_watching),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shows, key = { it.id }) { show ->
                val progress = progressMap[show.id] ?: 0f
                Column(modifier = Modifier.width(130.dp)) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onItemClick(show, "up_next") }
                    ) {
                        with(sharedTransitionScope) {
                            TmdbImage(
                                path = show.posterPath,
                                contentDescription = show.name,
                                size = TmdbUtils.ImageSize.W500,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .sharedElement(
                                        state = rememberSharedContentState(key = "image-${show.id}-up_next"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                        startY = 120f
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.home_play),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (progress > 0f) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = PrimaryPurple,
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = show.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
