package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.theme.AccentBlue
import com.andrea.showmateapp.ui.theme.StarYellow
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ThisWeekSection(
    allShows: List<MediaContent>,
    selectedPlatform: String?,
    platformShows: Map<String, List<MediaContent>>,
    isPlatformLoading: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMediaClick: (MediaContent, String) -> Unit,
    onPlatformSelected: (String?) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false
) {
    if (allShows.isEmpty()) return
    val platforms = remember { listOf("Netflix", "Prime", "Disney+", "Max", "Apple TV+") }
    val displayedShows = if (selectedPlatform != null) {
        platformShows[selectedPlatform] ?: emptyList()
    } else {
        allShows
    }

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
                    .background(AccentBlue)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.home_platforms_this_week),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            modifier = Modifier.padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(platforms, key = { it }) { platform ->
                val isSelected = selectedPlatform == platform
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                AccentBlue
                            } else {
                                Color.White.copy(alpha = 0.07f)
                            }
                        )
                        .then(
                            if (isSelected) {
                                Modifier
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        )
                        .clickable { onPlatformSelected(platform) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = platform,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (isPlatformLoading && selectedPlatform != null && !platformShows.containsKey(selectedPlatform)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(28.dp))
            }
        } else if (displayedShows.isEmpty() && selectedPlatform != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.home_no_platform_news, selectedPlatform),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        } else {
            val thisWeekListState = rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    if (selectedPlatform != null) {
                        false
                    } else {
                        val last = thisWeekListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        val total = thisWeekListState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 3
                    }
                }
            }
            LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

            LazyRow(
                state = thisWeekListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedShows, key = { it.id }) { show ->
                    ThisWeekCard(
                        media = show,
                        modifier = Modifier.animateItem(),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onMediaClick(show, "thisweek") }
                    )
                }
                if (isLoadingMore && selectedPlatform == null) {
                    item {
                        Box(
                            modifier = Modifier.width(176.dp).height(104.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ThisWeekCard(
    media: MediaContent,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(176.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
    ) {
        TmdbImage(
            path = media.backdropPath ?: media.posterPath,
            contentDescription = media.name,
            size = TmdbUtils.ImageSize.W500,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (LocalInspectionMode.current) {
                        Modifier
                    } else {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "image-${media.id}-thisweek"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 40f
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AccentBlue)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.home_on_air),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (media.voteAverage > 0f) {
                Text(
                    text = "${"%.1f".format(media.voteAverage)} ★",
                    color = StarYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

