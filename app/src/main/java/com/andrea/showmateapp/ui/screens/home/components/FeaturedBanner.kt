package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.MatchBadge
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.theme.PrimaryMagenta
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.util.TmdbUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeaturedBanner(
    shows: List<MediaContent>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent) -> Unit
) {
    if (shows.isEmpty()) return
    val tag = "hero"
    var currentIndex by remember { mutableIntStateOf(0) }
    val bannerDuration = 4500

    LaunchedEffect(shows.size) {
        while (true) {
            delay(bannerDuration.toLong())
            currentIndex = (currentIndex + 1) % shows.size
        }
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
        label = "banner_crossfade"
    ) { idx ->
        val media = shows[idx]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .clickable { onClick(media) }
        ) {
            TmdbImage(
                path = media.backdropPath ?: media.posterPath,
                contentDescription = "Featured: ${media.name}",
                size = TmdbUtils.ImageSize.W1280,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (LocalInspectionMode.current) {
                            Modifier
                        } else {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState(key = "image-${media.id}-$tag"),
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
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 0f
                        )
                    )
            )

            if (media.affinityScore > 0f) {
                MatchBadge(
                    score = media.affinityScore,
                    isAffinity = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (media.voteAverage > 0f) {
                        Text(
                            text = "${"%.1f".format(media.voteAverage)} ★",
                            color = Color(0xFFFFC107),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val year = media.firstAirDate?.take(4)
                    if (year != null) {
                        Text("·", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        Text(year, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    media.numberOfSeasons?.let { seasons ->
                        Text("·", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        Text("$seasons temp.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = media.name,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (media.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = media.overview,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (shows.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(shows.size) { dotIdx ->
                            val isActive = dotIdx == idx
                            val progressAnim = remember { Animatable(0f) }
                            LaunchedEffect(idx) {
                                if (dotIdx == idx) {
                                    progressAnim.snapTo(0f)
                                    progressAnim.animateTo(
                                        1f,
                                        tween(bannerDuration, easing = LinearEasing)
                                    )
                                } else {
                                    progressAnim.snapTo(0f)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(if (isActive) 32.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressAnim.value)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val btnInteraction = remember { MutableInteractionSource() }
                val isBtnPressed by btnInteraction.collectIsPressedAsState()
                val btnScale by animateFloatAsState(
                    targetValue = if (isBtnPressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
                    label = "btnScale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(btnScale)
                        .shadow(16.dp, RoundedCornerShape(50.dp), spotColor = PrimaryPurple)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryMagenta)))
                        .clickable(
                            interactionSource = btnInteraction,
                            indication = null
                        ) { onClick(media) }
                        .padding(horizontal = 24.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ver ahora",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }
    }
}

