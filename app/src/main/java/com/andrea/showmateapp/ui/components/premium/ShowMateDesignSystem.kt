package com.andrea.showmateapp.ui.components.premium

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.ReasonType
import com.andrea.showmateapp.data.model.RecommendationReason
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils
import kotlinx.coroutines.delay
import timber.log.Timber

object ShowMateSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object ShowMateMotion {
    val pressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val tabSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val colorTween: TweenSpec<Color> = tween(durationMillis = 180)
}

object ShowMateElevation {
    val card = 2.dp
    val raised = 4.dp
    val sheet = 8.dp
    val dialog = 12.dp
}

private val showCardOverlayGradient = Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.15f)),
    startY = 0f
)

@Composable
fun TmdbImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: TmdbUtils.ImageSize = TmdbUtils.ImageSize.W342,
    contentScale: ContentScale = ContentScale.Crop,
    error: Int = R.drawable.ic_logo_placeholder,
    crossfade: Boolean = true
) {
    val url = remember(path, size) { TmdbUtils.buildImageUrl(path, size) }

    if (url == null) {
        Box(modifier = modifier.background(SurfaceDark))
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(if (crossfade) 250 else 0)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .listener(
                onError = { _, result ->
                    Timber.e("TmdbImage error url=$url: ${result.throwable}")
                }
            )
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shimmerBrush())
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceDark)
            )
        },
        success = { SubcomposeAsyncImageContent() }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShowCard(
    media: MediaContent,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (MediaContent, String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    showTitle: Boolean = true,
    tag: String = "list"
) {
    val sharedElementKey = "image-${media.id}-$tag"

    val isHighScore = media.affinityScore >= 0.8f || media.voteAverage >= 8.0f

    val glowAlpha = if (isHighScore) {
        val glowTransition = rememberInfiniteTransition(label = "glow")
        val glowPulse by glowTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                tween(1800, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowPulse"
        )
        glowPulse
    } else {
        0f
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ShowMateMotion.pressSpring,
        label = "cardPress"
    )

    Column(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = media.name
            ) { onClick(media, tag) }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 2.8f)
                .fillMaxWidth()
                .shadow(
                    elevation = if (isHighScore) 8.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = if (media.affinityScore >= 0.8f) {
                        MatchGreen.copy(alpha = glowAlpha)
                    } else {
                        GoldAccent.copy(alpha = glowAlpha)
                    }
                )
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
        ) {
            TmdbImage(
                path = media.posterPath,
                contentDescription = media.name,
                size = TmdbUtils.ImageSize.W185,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (LocalInspectionMode.current) {
                            Modifier
                        } else {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = sharedElementKey),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(showCardOverlayGradient)
            )

            if (media.affinityScore > 0f) {
                MatchBadge(
                    score = media.affinityScore,
                    isAffinity = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            if (media.voteAverage > 0f) {
                MatchBadge(
                    score = media.voteAverage,
                    isAffinity = false,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

        }

        if (showTitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MatchBadge(score: Float, isAffinity: Boolean, modifier: Modifier = Modifier) {
    val percentage = (score * 10).toInt().coerceIn(0, 100)
    val displayValue = remember(score, isAffinity) {
        if (isAffinity) "$percentage%" else "${"%.1f".format(score)}★"
    }
    val matchColor = when {
        isAffinity && percentage >= 80 -> MatchGreen
        isAffinity && percentage >= 50 -> MatchYellow
        !isAffinity && score >= 7.5 -> GoldAccent
        else -> Color.White.copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = matchColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayValue,
            color = matchColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
fun ReasonPill(reason: RecommendationReason, modifier: Modifier = Modifier) {
    val pillColor = when (reason.type) {
        ReasonType.GENRE -> PillGenre
        ReasonType.ACTOR -> PillActor
        ReasonType.NARRATIVE -> PillNarrative
        ReasonType.CREATOR -> PillCreator
        ReasonType.HIDDEN_GEM -> PillHiddenGem
        ReasonType.COLLABORATIVE -> PillCollab
        ReasonType.BINGE -> PillBinge
        ReasonType.COMPLETENESS -> PillCompleteness
        ReasonType.TRENDING -> PillTrending
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(
                width = 1.dp,
                color = pillColor.copy(alpha = 0.50f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = reason.description.asString(),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            softWrap = true,
            lineHeight = 11.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShowSection(
    title: String,
    items: List<MediaContent>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaContent, String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryPurple,
    tag: String = "list",
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (onSeeAll != null) Arrangement.SpaceBetween else Arrangement.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onSeeAll != null) {
                Box(
                    modifier = Modifier
                        .heightIn(min = ShowMateSpacing.xxxl)
                        .clickable(
                            onClickLabel = "Ver todos",
                            role = Role.Button
                        ) { onSeeAll() }
                        .padding(horizontal = ShowMateSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ver todos",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
        }
        val shouldLoadMore by remember {
            derivedStateOf {
                if (onLoadMore == null) {
                    false
                } else {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    val total = listState.layoutInfo.totalItemsCount
                    total > 0 && lastVisible >= total - 3
                }
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) onLoadMore?.invoke()
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.id }) { item ->
                ShowCard(
                    media = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onItemClick,
                    modifier = Modifier.animateItem(),
                    tag = tag
                )
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 2.8f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryPurple,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseLoader(modifier: Modifier = Modifier) {
    val circles = listOf(
        remember { Animatable(0.3f) },
        remember { Animatable(0.3f) },
        remember { Animatable(0.3f) }
    )

    circles.forEachIndexed { index, animatable ->
        LaunchedEffect(Unit) {
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            circles.forEach { animatable ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(animatable.value)
                        .background(PrimaryPurple, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = PrimaryPurple.copy(alpha = 0.8f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(ShowMateSpacing.m))
        Text(
            text = "¡Ups! Algo salió mal",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(ShowMateSpacing.xs))
        Text(
            text = message,
            color = TextGray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ShowMateSpacing.xxl))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .heightIn(min = ShowMateSpacing.xxxl)
                .defaultMinSize(minWidth = 160.dp),
            contentPadding = PaddingValues(horizontal = ShowMateSpacing.xxl, vertical = ShowMateSpacing.m)
        ) {
            Text("Reintentar", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer"
    )
    val baseColor = Color(0xFF2A2A3D)
    val highlightColor = Color(0xFF3A3A52)
    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, 0f)
    )
}

@Composable
fun ShowSectionSkeleton(title: String, modifier: Modifier = Modifier, cardCount: Int = 5) {
    val shimmer = shimmerBrush()
    Column(modifier = modifier) {
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
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cardCount) {
                Column(modifier = Modifier.width(110.dp)) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(2f / 2.8f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmer)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
            }
        }
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = PrimaryPurple,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) = Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = shape,
    tonalElevation = 2.dp,
    modifier = modifier,
    content = content
)

@Composable
fun <T> PremiumTabRow(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.05f),
    indicatorColor: Color = PrimaryPurple,
    selectedContentColor: Color = Color.White,
    unselectedContentColor: Color = TextGray
) {
    val selectedIndex = tabs.indexOf(selectedTab)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(4.dp)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        var tabWidths by remember { mutableStateOf(List(tabs.size) { 0.dp }) }

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidths.take(selectedIndex).fold(0.dp) { acc, d -> acc + d },
            animationSpec = ShowMateMotion.tabSpring,
            label = "indicatorOffset"
        )

        val currentTabWidth = tabWidths.getOrElse(selectedIndex) { 0.dp }

        if (tabWidths.all { it > 0.dp }) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(currentTabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(indicatorColor, indicatorColor.copy(alpha = 0.8f))
                        )
                    )
                    .zIndex(0f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                    label = "tabContentColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(vertical = 10.dp)
                        .onGloballyPositioned { coords ->
                            val newWidths = tabWidths.toMutableList()
                            newWidths[index] = with(density) { coords.size.width.toDp() }
                            tabWidths = newWidths
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(tab),
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.zIndex(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PrimaryPurple,
    onBackClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = ShowMateSpacing.m, vertical = ShowMateSpacing.s)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            trailingContent()
        }
    }
}

