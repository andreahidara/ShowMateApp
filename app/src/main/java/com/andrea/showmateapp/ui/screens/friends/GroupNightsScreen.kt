package com.andrea.showmateapp.ui.screens.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.TmdbUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupNightsScreen(navController: NavController, viewModel: GroupNightsViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🌙 Noches de series",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text("Historial del grupo", color = TextGray, fontSize = 12.sp)
            }
            Icon(
                Icons.Default.NightsStay,
                null,
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        when {
            isLoading -> LoadingNights()
            entries.isEmpty() -> EmptyNights()
            else -> {
                NightsStatsStrip(entries)
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(entries, key = { _, entry -> entry.session.id }) { index, entry ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 60L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                        ) {
                            NightCard(
                                entry = entry,
                                onClickDetail = { showId ->
                                    navController.navigate(Screen.Detail(showId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightsStatsStrip(entries: List<NightEntry>) {
    val matches = entries.count { it.session.matchedMediaId != 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("🎬", entries.size.toString(), "noches")
        StatItem("✅", matches.toString(), "matches")
        StatItem("🔥", (entries.size - matches).toString(), "sin match")
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}

@Composable
private fun NightCard(entry: NightEntry, onClickDetail: (Int) -> Unit) {
    val session = entry.session
    val media = entry.matchedMedia
    val hasMatch = session.matchedMediaId != 0

    val dateStr = remember(session.finishedAt) {
        if (session.finishedAt > 0L) {
            SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "ES"))
                .format(Date(session.finishedAt))
        } else {
            "Fecha desconocida"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (hasMatch) PrimaryPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
            .then(
                if (hasMatch && media != null) {
                    Modifier.clickable { onClickDetail(session.matchedMediaId) }
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (hasMatch) {
                        PrimaryPurple.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    }
                )
        ) {
            if (media?.posterPath != null) {
                TmdbImage(
                    path = media.posterPath,
                    contentDescription = media.name,
                    size = TmdbUtils.ImageSize.W185,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.3f))
                            )
                        )
                )
            } else {
                Icon(
                    if (hasMatch) Icons.Default.EmojiEvents else Icons.Default.Groups,
                    null,
                    tint = if (hasMatch) PrimaryPurpleLight else TextGray,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val displayTitle = session.nightTitle
                .ifBlank { if (hasMatch) media?.name ?: "Noche sin nombre" else "Sin match" }
            Text(
                displayTitle,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(dateStr, color = TextGray, fontSize = 11.sp)

            val memberList = session.memberEmails
                .map { it.substringBefore("@") }
                .joinToString(" · ")
            Text(
                memberList,
                color = TextGray.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))
            if (hasMatch) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SuccessGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("✓ Match encontrado", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Sin match", color = TextGray, fontSize = 11.sp)
                }
            }
        }

        if (media != null && media.voteAverage > 0f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐", fontSize = 18.sp)
                Text(
                    "${"%.1f".format(media.voteAverage)}",
                    color = StarYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingNights() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text("Cargando historial…", color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyNights() {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🌙",
            fontSize = 72.sp,
            modifier = Modifier.offset(y = translateY.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Sin noches todavía",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Organiza tu primera sesión grupal con amigos y el historial aparecerá aquí.",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
