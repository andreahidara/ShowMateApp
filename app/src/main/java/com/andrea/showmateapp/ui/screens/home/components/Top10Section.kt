package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Top10Section(
    shows: List<MediaContent>,
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
                    .background(Color(0xFFE040FB))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_top_10),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shows.take(10), key = { it.id }) { show ->
                val rank = shows.indexOf(show) + 1
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(190.dp)
                        .animateItem()
                        .clickable { onItemClick(show, "top10") }
                ) {
                    with(sharedTransitionScope) {
                        TmdbImage(
                            path = show.posterPath,
                            contentDescription = show.name,
                            size = TmdbUtils.ImageSize.W500,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .let {
                                    if (LocalInspectionMode.current) {
                                        it
                                    } else {
                                        it.sharedElement(
                                            state = rememberSharedContentState(key = "image-${show.id}-top10"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-4).dp, y = 10.dp)
                    ) {
                        Text(
                            text = "$rank",
                            style = TextStyle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                            fontSize = if (rank < 10) 74.sp else 60.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 74.sp
                        )
                    }
                }
            }
        }
    }
}

