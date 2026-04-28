package com.andrea.showmateapp.ui.screens.swipe

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.MatchBadge
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.components.shimmerBrush
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.TmdbUtils
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(navController: NavController) {
    val viewModel: SwipeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadShows()
    }

    SwipeScreenContent(
        showsToRate = uiState.shows,
        errorMessage = uiState.errorMessage,
        isLoading = uiState.isLoading,
        ratedCount = uiState.ratedCount,
        lastAction = uiState.lastAction,
        onLikeShow = { viewModel.likeTopShow() },
        onSkipShow = { viewModel.skipTopShow() },
        onEssentialShow = { viewModel.essentialTopShow() },
        onUndoAction = { viewModel.undoLastAction() },
        onNavigateToHome = {
            navController.navigate(Screen.Main) {
                popUpTo(Screen.Swipe) { inclusive = true }
            }
        }
    )
}

@Composable
fun SwipeScreenContent(
    showsToRate: List<MediaContent>,
    errorMessage: String?,
    isLoading: Boolean,
    ratedCount: Int,
    lastAction: SwipeUiState.SwipeAction?,
    onLikeShow: () -> Unit,
    onSkipShow: () -> Unit,
    onEssentialShow: () -> Unit = {},
    onUndoAction: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val maxRatings = 10
    val haptic = LocalHapticFeedback.current

    if (isLoading) {
        SwipeCardSkeleton()
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage,
                color = HeartRed,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.swipe_for_you),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = stringResource(R.string.swipe_calibrate_subtitle),
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.22f), PrimaryPurpleDark.copy(alpha = 0.14f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$ratedCount",
                        color = PrimaryPurpleLight,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = stringResource(R.string.swipe_of_max_ratings, maxRatings),
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(maxRatings) { index ->
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (index < ratedCount) {
                                Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (ratedCount >= maxRatings) {
                SuccessState(onNavigateToHome)
            } else if (showsToRate.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryPurple.copy(alpha = 0.20f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(stringResource(R.string.swipe_no_more_shows), color = TextGray, fontSize = 15.sp)
                }
            } else {
                val stackLimit = 3
                val visibleShows = showsToRate.take(stackLimit).reversed()

                visibleShows.forEachIndexed { index, show ->
                    val stackIndex = visibleShows.size - 1 - index
                    key(show.id) {
                        SwipeableCard(
                            media = show,
                            stackIndex = stackIndex,
                            onSwiped = { isLiked ->
                                haptic.performHapticFeedback(
                                    if (isLiked) {
                                        HapticFeedbackType.LongPress
                                    } else {
                                        HapticFeedbackType.TextHandleMove
                                    }
                                )
                                if (isLiked) onLikeShow() else onSkipShow()
                            }
                        )
                    }
                }
            }
        }

        if (ratedCount < maxRatings && showsToRate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .semantics { role = Role.Button }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipShow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.swipe_cd_skip),
                        tint = HeartRed.copy(alpha = 0.9f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                val undoActive = lastAction != null
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (undoActive) {
                                Color.White.copy(alpha = 0.08f)
                            } else {
                                Color.White.copy(alpha = 0.03f)
                            }
                        )
                        .border(
                            1.dp,
                            if (undoActive) {
                                Color.White.copy(alpha = 0.15f)
                            } else {
                                Color.White.copy(alpha = 0.05f)
                            },
                            CircleShape
                        )
                        .semantics { role = Role.Button }
                        .clickable(enabled = undoActive) { onUndoAction() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.swipe_cd_undo),
                        tint = if (undoActive) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .shadow(20.dp, CircleShape, spotColor = PrimaryPurple.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryMagenta),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(100f, 100f)
                            )
                        )
                        .semantics { role = Role.Button }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeShow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.swipe_cd_like),
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(12.dp, CircleShape, spotColor = StarYellow.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .background(StarYellow.copy(alpha = 0.12f))
                        .border(1.dp, StarYellow.copy(alpha = 0.35f), CircleShape)
                        .semantics { role = Role.Button }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEssentialShow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(R.string.swipe_cd_essential),
                        tint = StarYellow,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessState(onNavigateToHome: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "successGlow")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                    }
                    .alpha(ringAlpha)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPurple.copy(alpha = 0.25f), PrimaryPurpleDark.copy(alpha = 0.15f))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(PrimaryPurpleLight.copy(alpha = 0.6f), PrimaryPurple.copy(alpha = 0.3f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.swipe_all_ready),
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.swipe_all_ready_desc),
            color = TextGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(PrimaryPurple, Color(0xFF9C27B0)))
                )
                .clickable { onNavigateToHome() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.swipe_start_exploring),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun SwipeableCard(media: MediaContent, stackIndex: Int, onSwiped: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val isTopCard = stackIndex == 0

    val scale by animateFloatAsState(targetValue = 1f - (stackIndex * 0.05f), label = "scale")
    val translateY by animateFloatAsState(targetValue = (stackIndex * -16f), label = "translateY")
    val alpha by animateFloatAsState(targetValue = 1f - (stackIndex * 0.28f), label = "alpha")

    val cardGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.42f to Color.Transparent,
                0.70f to Color.Black.copy(alpha = 0.75f),
                1f to Color.Black.copy(alpha = 0.98f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = translateY.dp.toPx()
                scaleX = scale
                scaleY = scale
                rotationZ = (offsetX.value / 28f).coerceIn(-18f, 18f)
                this.alpha = alpha
            }
            .pointerInput(isTopCard) {
                if (isTopCard) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX.value.absoluteValue > 500f) {
                                val isLiked = offsetX.value > 0
                                scope.launch {
                                    offsetX.animateTo(
                                        if (isLiked) 1500f else -1500f,
                                        tween(350)
                                    )
                                    onSwiped(isLiked)
                                }
                            } else {
                                scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        }
                    )
                }
            }
            .clip(RoundedCornerShape(32.dp))
            .shadow(
                if (isTopCard) 24.dp else 0.dp,
                RoundedCornerShape(32.dp),
                spotColor = PrimaryPurple.copy(alpha = 0.25f)
            )
    ) {
        TmdbImage(
            path = media.posterPath,
            contentDescription = null,
            size = TmdbUtils.ImageSize.W780,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardGradient)
        )

        if (isTopCard && offsetX.value != 0f) {
            val swipeAlpha = (offsetX.value.absoluteValue / 280f).coerceIn(0f, 0.50f)
            val isLiking = offsetX.value > 0
            val overlayColor = if (isLiking) Color(0xFF4CAF50) else HeartRed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = swipeAlpha))
            )
            val labelAlpha = ((offsetX.value.absoluteValue - 80f) / 180f).coerceIn(0f, 1f)
            if (labelAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(if (isLiking) Alignment.TopStart else Alignment.TopEnd)
                        .padding(28.dp)
                        .alpha(labelAlpha)
                        .graphicsLayer { rotationZ = if (isLiking) -14f else 14f }
                        .clip(RoundedCornerShape(8.dp))
                        .border(3.dp, overlayColor, RoundedCornerShape(8.dp))
                        .background(overlayColor.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isLiking) stringResource(R.string.swipe_label_like) else stringResource(R.string.swipe_label_skip),
                        color = overlayColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        if (media.affinityScore > 0f) {
            MatchBadge(
                score = media.affinityScore,
                isAffinity = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 26.dp, vertical = 26.dp)
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val year = media.firstAirDate?.take(4)?.takeIf { it.isNotBlank() }
            val seasons = media.numberOfSeasons?.takeIf { it > 0 }?.let { "$it temp." }
            val metaParts = listOfNotNull(year, seasons)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                metaParts.forEachIndexed { index, part ->
                    if (index > 0) {
                        Text("·", color = Color.White.copy(alpha = 0.40f), fontSize = 12.sp)
                    }
                    Text(
                        part,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (media.voteAverage > 0f) {
                    if (metaParts.isNotEmpty()) {
                        Text("·", color = Color.White.copy(alpha = 0.40f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StarYellow.copy(alpha = 0.20f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = StarYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "%.1f".format(media.voteAverage),
                                color = StarYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val genreNames = media.safeGenreIds.take(3).map { GenreMapper.getGenreName(it.toString()) }
            if (genreNames.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    genreNames.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.42f))
                                .border(
                                    0.5.dp,
                                    Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = genre,
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = media.overview,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SwipeCardSkeleton() {
    val shimmer = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(160.dp).height(32.dp).clip(RoundedCornerShape(14.dp)).background(shimmer))
                Box(Modifier.width(200.dp).height(16.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
            }
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(14.dp)).background(shimmer))
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(10) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(shimmer)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(shimmer)
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
