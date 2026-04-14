package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.*
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.TmdbUtils

@Composable
fun DetailSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PrimaryPurple)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun DetailActionButtonsRow(
    isWatched: Boolean,
    isLiked: Boolean,
    isEssential: Boolean,
    isInWatchlist: Boolean,
    onWatchedClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEssentialClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailActionButton(
            icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.Check,
            text = stringResource(R.string.detail_watched),
            containerColor = if (isWatched) SuccessGreen else Color.White.copy(alpha = 0.08f),
            contentColor = if (isWatched) Color.White else TextGray,
            isActive = isWatched,
            modifier = Modifier.weight(1f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onWatchedClick()
            }
        )

        DetailActionButton(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            text = stringResource(R.string.detail_favorite),
            containerColor = if (isLiked) HeartRed else Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            isActive = isLiked,
            modifier = Modifier.weight(1f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLikeClick()
            }
        )

        DetailActionButton(
            icon = if (isEssential) Icons.Default.Star else Icons.Default.StarBorder,
            text = stringResource(R.string.detail_top),
            containerColor = if (isEssential) StarYellow else Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            isActive = isEssential,
            modifier = Modifier.weight(0.8f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEssentialClick()
            }
        )

        DetailActionButton(
            icon = if (isInWatchlist) Icons.Default.WatchLater else Icons.Outlined.WatchLater,
            text = stringResource(R.string.detail_watchlist),
            containerColor = if (isInWatchlist) AccentBlue else Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            isActive = isInWatchlist,
            modifier = Modifier.weight(1f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onWatchlistClick()
            }
        )
    }
}

@Composable
fun DetailActionButton(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.22f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "iconScale"
    )

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .semantics { role = Role.Button }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CastMemberItem(
    member: com.andrea.showmateapp.data.model.CastMember,
    modifier: Modifier = Modifier,
    onClick: ((Int, String) -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(84.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = "Ver perfil de ${member.name}") {
                        onClick(member.id, member.name)
                    }
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = "${member.name}, ${member.character}" }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .then(
                    if (onClick != null) {
                        Modifier.border(
                            1.dp,
                            PrimaryPurple.copy(alpha = 0.3f),
                            CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            TmdbImage(
                path = member.profilePath,
                contentDescription = null,
                size = TmdbUtils.ImageSize.W185,
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            color = if (onClick != null) Color.White else Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
        Text(
            text = member.character,
            color = TextGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun WatchProvidersSection(providers: com.andrea.showmateapp.data.model.CountryProviders, showName: String) {
    val jwLink = providers.link

    Column {
        DetailSectionHeader(title = stringResource(R.string.detail_where_to_watch))
        Spacer(modifier = Modifier.height(16.dp))

        val flatrate = providers.flatrate
        if (!flatrate.isNullOrEmpty()) {
            ProviderTypeRow(
                label = stringResource(R.string.detail_provider_flatrate),
                providers = flatrate,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        val rent = providers.rent
        if (!rent.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = stringResource(R.string.detail_provider_rent),
                providers = rent,
                showName = showName,
                fallbackUrl = jwLink
            )
        }
        val buy = providers.buy
        if (!buy.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProviderTypeRow(
                label = stringResource(R.string.detail_provider_buy),
                providers = buy,
                showName = showName,
                fallbackUrl = jwLink
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.detail_justwatch_attribution),
            fontSize = 11.sp,
            color = TextGray.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ProviderTypeRow(
    label: String,
    providers: List<com.andrea.showmateapp.data.model.Provider>,
    showName: String,
    fallbackUrl: String?
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Text(
        text = label,
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
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
fun ProviderLogo(
    provider: com.andrea.showmateapp.data.model.Provider,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    TmdbImage(
        path = provider.logoPath,
        contentDescription = provider.providerName,
        size = TmdbUtils.ImageSize.ORIGINAL,
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    )
}

private fun providerSearchUrl(providerId: Int, showName: String): String? {
    val encoded = java.net.URLEncoder.encode(showName, "UTF-8")
    return when (providerId) {
        8, 213 -> "https://www.netflix.com/search?q=$encoded"
        9, 119 -> "https://www.primevideo.com/search?k=$encoded"
        337 -> "https://www.disneyplus.com/es-es/search/$encoded"
        384, 29 -> "https://play.max.com/search?q=$encoded"
        2, 350 -> "https://tv.apple.com/search?term=$encoded"
        531 -> "https://www.paramountplus.com/es/search/$encoded/"
        1773 -> "https://www.skyshowtime.com/es/search?q=$encoded"
        63 -> "https://www.filmin.es/buscar?q=$encoded"
        149 -> "https://ver.movistarplus.es/buscar/?q=$encoded"
        35, 105 -> "https://www.rakuten.tv/es/search?q=$encoded"
        541 -> "https://www.atresplayer.com/buscar/?q=$encoded"
        566 -> "https://www.rtve.es/play/buscar/?q=$encoded"
        188 -> "https://www.youtube.com/results?search_query=$encoded"
        else -> null
    }
}

@Composable
fun StarRatingSection(
    userRating: Int?,
    onRateClick: (Int) -> Unit,
    onClearRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starCount = 5
    val haptic = LocalHapticFeedback.current
    val visualRating = userRating?.let { ((it + 1) / 2).coerceIn(1, 5) } ?: 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        PrimaryPurple.copy(alpha = 0.14f),
                        SurfaceVariantDark
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.detail_your_rating),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (userRating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StarYellow.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "$userRating/10",
                                color = StarYellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onClearRateClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.detail_remove_rating),
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                repeat(starCount) { index ->
                    val filled = index < visualRating
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRateClick((index + 1) * 2)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (filled) StarYellow else Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewSection(
    reviewText: String,
    isSaving: Boolean,
    isReviewSaved: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember(isReviewSaved) {
        mutableStateOf(reviewText.isBlank() || !isReviewSaved)
    }

    Column(modifier = modifier) {
        DetailSectionHeader(title = stringResource(R.string.detail_my_review))
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (!isEditing && reviewText.isNotBlank()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.detail_review_saved_label),
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { isEditing = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.edit),
                                        color = PrimaryPurpleLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = onDelete,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.delete),
                                        color = ErrorRed.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryPurple.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(PrimaryPurple, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = reviewText,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { if (it.length <= 500) onTextChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.detail_review_placeholder),
                                color = Color.White.copy(alpha = 0.28f),
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${reviewText.length}/500",
                            color = if (reviewText.length > 450) {
                                StarYellow
                            } else {
                                Color.White.copy(alpha = 0.28f)
                            },
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSave()
                                isEditing = false
                            },
                            enabled = reviewText.isNotBlank() && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    stringResource(R.string.save),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodesSection(
    seasons: List<com.andrea.showmateapp.data.model.Season>,
    selectedSeason: com.andrea.showmateapp.data.model.SeasonResponse?,
    isSeasonLoading: Boolean,
    watchedEpisodes: List<Int>,
    modifier: Modifier = Modifier,
    onEpisodeToggle: (Int, Boolean) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onToggleSeason: () -> Unit,
    onMarkNextEpisode: () -> Unit = {}
) {
    val episodeProgressGradient = remember {
        Brush.linearGradient(listOf(PrimaryPurpleLight, PrimaryPurple))
    }
    val completedGradient = remember {
        Brush.linearGradient(listOf(SuccessGreenLight, SuccessGreen))
    }

    Column(modifier = modifier) {
        DetailSectionHeader(title = stringResource(R.string.detail_episodes))
        Spacer(modifier = Modifier.height(16.dp))

        val filteredSeasons = remember(seasons) { seasons.filter { it.seasonNumber > 0 } }

        var selectedSeasonNumber by remember(selectedSeason) {
            mutableIntStateOf(
                selectedSeason?.seasonNumber ?: filteredSeasons.firstOrNull()?.seasonNumber ?: 1
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredSeasons, key = { it.seasonNumber }) { season ->
                val isSelected = selectedSeasonNumber == season.seasonNumber
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                Brush.linearGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.07f),
                                        Color.White.copy(alpha = 0.07f)
                                    )
                                )
                            }
                        )
                        .clickable {
                            selectedSeasonNumber = season.seasonNumber
                            onSeasonChange(season.seasonNumber)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.detail_season_short, season.seasonNumber),
                        color = if (isSelected) Color.White else TextGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSeasonLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (selectedSeason != null && selectedSeason.episodes.isNotEmpty()) {
            val totalEps = selectedSeason.episodes.size
            val watchedInSeason = selectedSeason.episodes.count { watchedEpisodes.contains(it.id) }
            val progress = watchedInSeason.toFloat() / totalEps.toFloat()
            val isCompleted = watchedInSeason == totalEps

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.detail_progress),
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$watchedInSeason / $totalEps ep",
                                    color = if (isCompleted) SuccessGreen else PrimaryPurpleLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = onToggleSeason,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) {
                                        SuccessGreen.copy(
                                            alpha = 0.1f
                                        )
                                    } else {
                                        PrimaryPurple.copy(alpha = 0.1f)
                                    },
                                    contentColor = if (isCompleted) SuccessGreen else PrimaryPurpleLight
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.DoneAll else Icons.Default.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCompleted) {
                                        stringResource(
                                            R.string.detail_unmark_all
                                        )
                                    } else {
                                        stringResource(R.string.detail_mark_all)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) completedGradient else episodeProgressGradient)
                            )
                        }
                    }
                }
            }

            val episodes = selectedSeason.episodes
            Column(modifier = Modifier.fillMaxWidth()) {
                episodes.forEach { episode ->
                    EpisodeItem(
                        episode = episode,
                        isWatched = watchedEpisodes.contains(episode.id),
                        onToggle = { onEpisodeToggle(episode.id, false) },
                        onTogglePrevious = { onEpisodeToggle(episode.id, true) }
                    )
                    if (episode != episodes.last()) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val nextEpisode = selectedSeason.episodes.firstOrNull { it.id !in watchedEpisodes }
            if (nextEpisode != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryPurple.copy(alpha = 0.20f), PrimaryPurpleDark.copy(alpha = 0.10f))
                            )
                        )
                        .clickable { onMarkNextEpisode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimaryPurpleLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(
                                R.string.detail_next_episode_format,
                                nextEpisode.episodeNumber,
                                nextEpisode.name
                            ),
                            color = PrimaryPurpleLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeItem(
    episode: com.andrea.showmateapp.data.model.Episode,
    isWatched: Boolean,
    onToggle: () -> Unit,
    onTogglePrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePrevious()
                }
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 66.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            TmdbImage(
                path = episode.stillPath,
                contentDescription = episode.name,
                size = TmdbUtils.ImageSize.W300,
                modifier = Modifier.fillMaxSize()
            )
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name}",
                color = if (isWatched) TextGray else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (episode.airDate != null) {
                Text(
                    text = episode.airDate,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            if (episode.runtime != null) {
                Text(
                    text = "${episode.runtime} min",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }
        Checkbox(
            checked = isWatched,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = SuccessGreen,
                uncheckedColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun WhyRecommendedDialog(factors: List<RecommendationReason>, onDismiss: () -> Unit) {
    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsVisible = true }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InputBackground,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = PrimaryPurple)
            }
        },
        title = {
            Text(
                stringResource(R.string.detail_why_recommended),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "El algoritmo de ShowMate ha cruzado tu historial visual interactivo, tus géneros guardados y la afinidad de otros usuarios similares para calcular tu nivel de afinidad con este título.",
                    color = TextGray,
                    fontSize = 13.sp,
                )
                if (factors.isEmpty()) {
                    Text(
                        stringResource(R.string.detail_why_recommended_empty),
                        color = TextGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    factors.forEach { reason ->
                        val barColor = when (reason.type) {
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
                        val animatedFraction by animateFloatAsState(
                            targetValue = if (barsVisible) reason.weight else 0f,
                            animationSpec = spring(stiffness = 120f),
                            label = "bar_${reason.type}"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = reason.description,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(reason.weight * 100).toInt()}%",
                                    color = barColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedFraction)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(barColor.copy(alpha = 0.6f), barColor)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    text: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDialog,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = TextGray) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) { Text(confirmText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextGray)
            }
        }
    )
}

@Composable
fun AddToListDialog(
    customLists: Map<String, List<Int>>,
    mediaId: Int?,
    onAddToList: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDialog,
        title = {
            Text(
                stringResource(R.string.detail_add_to_list),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (customLists.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.detail_no_lists),
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    customLists.entries.toList().forEach { (name, ids) ->
                        val alreadyAdded = mediaId?.let { ids.contains(it) } == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !alreadyAdded) { onAddToList(name) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name,
                                    color = if (alreadyAdded) TextGray else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.profile_series_count, ids.size),
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            if (alreadyAdded) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = TextGray)
            }
        }
    )
}

