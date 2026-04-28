package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.andrea.showmateapp.R
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.ShowMateSpacing
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.screens.home.MoodOption
import com.andrea.showmateapp.ui.screens.home.TimeOption
import com.andrea.showmateapp.ui.theme.AccentBlue
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.StarYellow
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.TmdbUtils

@Composable
fun WhatToWatchDialog(media: MediaContent, onDismiss: () -> Unit, onViewDetails: () -> Unit) {
    val matchPct = remember(media.affinityScore) {
        if (media.affinityScore > 0f) {
            (media.affinityScore * 10.05f).toInt().coerceIn(0, 100)
        } else {
            -1
        }
    }
    val matchColor = when {
        matchPct >= 80 -> Color(0xFF4CAF50)
        matchPct >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFE91E63)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val matchGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val genreNames = remember(media.id) {
        (
            media.genres?.map { it.name }?.takeIf { it.isNotEmpty() }
                ?: media.safeGenreIds.map { GenreMapper.getGenreName(it) }
            ).take(3)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1A1A2E),
            tonalElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    TmdbImage(
                        path = media.backdropPath ?: media.posterPath,
                        contentDescription = null,
                        size = TmdbUtils.ImageSize.W780,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(ShowMateSpacing.xxs)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (matchPct >= 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            color = matchColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, matchColor.copy(alpha = matchGlow))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(matchColor, CircleShape)
                                )
                                Text(
                                    text = "$matchPct% afinidad",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_for_you_today),
                            color = PrimaryPurpleLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = media.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (media.voteAverage > 0f) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StarYellow,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "%.1f".format(media.voteAverage),
                                    color = StarYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val year = media.firstAirDate?.take(4)
                            if (year != null) {
                                Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                Text(year, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            media.numberOfSeasons?.let {
                                Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                Text("$it temp.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    if (media.overview.isNotBlank()) {
                        Text(
                            text = media.overview,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    if (genreNames.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            genreNames.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.home_view_details),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContextSelectorDialog(onDismiss: () -> Unit, onConfirm: (MoodOption?, TimeOption?) -> Unit) {
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    var selectedTime by remember { mutableStateOf<TimeOption?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.home_what_to_watch),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.home_how_are_you),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MoodOption.entries.forEach { mood ->
                            val selected = selectedMood == mood
                            Surface(
                                onClick = { selectedMood = if (selected) null else mood },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) PrimaryPurple else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(mood.emoji, fontSize = 18.sp)
                                    Text(
                                        mood.label,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.home_how_much_time),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TimeOption.entries.forEach { time ->
                            val selected = selectedTime == time
                            Surface(
                                onClick = { selectedTime = if (selected) null else time },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) AccentBlue else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = time.label,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConfirm(selectedMood, selectedTime) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.home_surprise_me), fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}
