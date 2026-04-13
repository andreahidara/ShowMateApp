package com.andrea.showmateapp.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.R
import com.andrea.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.andrea.showmateapp.domain.usecase.GetWrappedStatsUseCase
import com.andrea.showmateapp.ui.components.premium.shimmerBrush
import com.andrea.showmateapp.ui.theme.*

@Composable
fun StatsScreen(navController: NavController, viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        if (uiState.isLoading) {
            StatsSkeleton(modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con gradiente (consistente con Home/Discover/Friends)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Estadísticas",
                            style = TextStyle(
                                brush = Brush.linearGradient(listOf(PrimaryPurple, PrimaryMagenta))
                            ),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(34.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryMagenta)))
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFFF6B35),
                            value = "${uiState.currentStreak}",
                            label = "Racha actual",
                            subtitle = "días seguidos"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = PrimaryPurple,
                            value = "${uiState.longestStreak}",
                            label = "Racha récord",
                            subtitle = "días"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Star,
                            iconTint = StarYellow,
                            value = "${uiState.dailyRecord}",
                            label = "Récord diario",
                            subtitle = "episodios"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Movie,
                            iconTint = Color(0xFF4CAF50),
                            value = "${uiState.totalEpisodesWatched}",
                            label = "Total episodios",
                            subtitle = "vistos"
                        )
                    }

                    uiState.wrappedStats?.let { wrapped ->
                        WrappedHeroCard(wrapped)
                    }

                    val lineData = uiState.wrappedStats?.monthlyActivity
                        ?.takeIf { it.isNotEmpty() }
                        ?: uiState.activityByMonth.entries.sortedBy { it.key }.takeLast(6)
                            .map { it.key.takeLast(5) to it.value }

                    if (lineData.isNotEmpty()) {
                        MonthlyLineChartCard(data = lineData)
                    }

                    if (uiState.topGenresByMonth.isNotEmpty()) {
                        TopGenresSection(topGenresByMonth = uiState.topGenresByMonth)
                    }

                    uiState.wrappedStats?.let { wrapped ->
                        if (wrapped.topActorName != null || wrapped.topDirectorName != null) {
                            TopPeopleCard(
                                actorName = wrapped.topActorName,
                                directorName = wrapped.topDirectorName
                            )
                        }

                        if (wrapped.mostActiveMonthLabel != null || wrapped.favoriteDayOfWeek != null) {
                            TemporalInsightsCard(
                                mostActiveMonth = wrapped.mostActiveMonthLabel,
                                favoriteDay = wrapped.favoriteDayOfWeek
                            )
                        }

                        if (wrapped.topRewatchedShows.isNotEmpty()) {
                            TopRewatchedCard(shows = wrapped.topRewatchedShows)
                        }

                        if (wrapped.countryDistribution.isNotEmpty()) {
                            CountryDistributionCard(distribution = wrapped.countryDistribution)
                        }
                    }

                    uiState.personalityProfile?.let { PersonalitySection(it) }

                    Spacer(modifier = Modifier.height(32.dp))
                } // close inner padding Column
            }
        }
    }
}

@Composable
fun WrappedHeroCard(wrapped: GetWrappedStatsUseCase.WrappedStats) {
    val animatedHours = remember { Animatable(0f) }
    LaunchedEffect(wrapped.totalHoursWatched) {
        animatedHours.animateTo(
            targetValue = wrapped.totalHoursWatched,
            animationSpec = tween(durationMillis = 1200, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF3D1A78), Color(0xFF1A1A4E)))
            )
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 20.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.10f))
        )

        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Tu año en series",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.0f".format(animatedHours.value),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 56.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "h",
                    color = PrimaryPurpleLight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "de series vistas",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = wrapped.viewerType.emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = wrapped.viewerType.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = wrapped.viewerType.description,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyLineChartCard(data: List<Pair<String, Int>>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = AccentBlue,
                title = "Actividad mensual"
            )
            Spacer(modifier = Modifier.height(20.dp))
            LineChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }
    }
}

@Composable
private fun LineChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(900))
    }

    val lineColor = PrimaryPurple
    val dotColor = PrimaryPurpleLight
    val gridColor = Color.White.copy(alpha = 0.06f)
    val labelColor = TextGray
    val maxVal = data.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    val progress = animProgress.value

    Column {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val padBottom = 0f
            val chartH = h - padBottom
            val stepX = if (data.size > 1) w / (data.size - 1) else w

            for (i in 0..2) {
                val y = chartH * (1f - i / 2f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            if (data.size < 2) return@Canvas

            val totalPoints = data.size
            val animatedEnd = (progress * (totalPoints - 1))
            val fullSegments = animatedEnd.toInt()
            val partialFraction = animatedEnd - fullSegments

            val path = Path()
            data.forEachIndexed { idx, (_, value) ->
                val x = idx * stepX
                val y = chartH * (1f - value / maxVal)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                if (idx >= fullSegments) return@forEachIndexed
            }

            if (fullSegments < data.size - 1 && partialFraction > 0f) {
                val (_, v0) = data[fullSegments]
                val (_, v1) = data[fullSegments + 1]
                val x0 = fullSegments * stepX
                val x1 = (fullSegments + 1) * stepX
                val y0 = chartH * (1f - v0 / maxVal)
                val y1 = chartH * (1f - v1 / maxVal)
                path.lineTo(x0 + (x1 - x0) * partialFraction, y0 + (y1 - y0) * partialFraction)
            }

            val fillPath = Path().apply {
                addPath(path)
                val lastX = minOf(animatedEnd, (data.size - 1).toFloat()) * stepX
                lineTo(lastX, chartH)
                lineTo(0f, chartH)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryPurple.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = chartH
                )
            )

            drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            data.take(fullSegments + 1).forEachIndexed { idx, (_, value) ->
                val x = idx * stepX
                val y = chartH * (1f - value / maxVal)
                drawCircle(dotColor, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(Color(0xFF1A1A2E), radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEachIndexed { idx, (label, _) ->
                val showLabel = data.size <= 6 || idx % 2 == 0
                Text(
                    text = if (showLabel) label else "",
                    color = labelColor,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TopPeopleCard(actorName: String?, directorName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actorName?.let {
            PersonCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Person,
                iconTint = Color(0xFFFF9800),
                label = "Actor/Actriz favorito",
                name = it
            )
        }
        directorName?.let {
            PersonCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.VideoCameraBack,
                iconTint = Color(0xFF00BCD4),
                label = "Director favorito",
                name = it
            )
        }
    }
}

@Composable
private fun PersonCard(modifier: Modifier, icon: ImageVector, iconTint: Color, label: String, name: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(iconTint, iconTint.copy(alpha = 0f))))
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, color = TextGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TemporalInsightsCard(mostActiveMonth: String?, favoriteDay: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                iconTint = StarYellow,
                title = "Tus momentos"
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                mostActiveMonth?.let {
                    InsightItem(
                        modifier = Modifier.weight(1f),
                        emoji = "\uD83D\uDCC5",
                        label = "Mes más activo",
                        value = it
                    )
                }
                favoriteDay?.let {
                    InsightItem(
                        modifier = Modifier.weight(1f),
                        emoji = "\uD83D\uDCC6",
                        label = "Día favorito",
                        value = it
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightItem(modifier: Modifier, emoji: String, label: String, value: String) {
    Column(modifier = modifier) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextGray, fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun TopRewatchedCard(shows: List<GetWrappedStatsUseCase.RewatchedShow>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.Repeat,
                iconTint = Color(0xFF9C27B0),
                title = "Más revisionadas"
            )

            shows.forEachIndexed { idx, show ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (idx) {
                                    0 -> Color(0xFFFFD700).copy(alpha = 0.15f)
                                    1 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
                                    else -> Color(0xFFCD7F32).copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = when (idx) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                else -> Color(0xFFCD7F32)
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = show.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${show.sessionCount} sesiones",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CountryDistributionCard(distribution: List<Pair<String, Int>>) {
    val max = distribution.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                icon = Icons.Default.Public,
                iconTint = Color(0xFF4CAF50),
                title = "Países de origen"
            )

            distribution.forEach { (country, count) ->
                val frac = count / max
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(country, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(120.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784))))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    subtitle: String
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = tween(durationMillis = 400),
        label = "statCardScale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to iconTint.copy(alpha = 0.10f),
                        1f to SurfaceVariantDark
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(iconTint, iconTint.copy(alpha = 0f))))
        )
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp,
                modifier = Modifier.scale(scale)
            )
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun PersonalitySection(profile: GetViewerPersonalityUseCase.PersonalityProfile) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SurfaceVariantDark)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Perfil de espectador", color = TextGray, fontSize = 11.sp)
                    Text(profile.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (profile.topGenres.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Text("Géneros favoritos", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                profile.topGenres.forEach { (name, fraction) ->
                    PersonalityBar(label = name, fraction = fraction, color = PrimaryPurple)
                }
            }

            if (profile.topNarrativeStyles.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Text("Estilos narrativos", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                profile.topNarrativeStyles.forEach { (name, fraction) ->
                    PersonalityBar(label = name, fraction = fraction, color = Color(0xFF9C27B0))
                }
            }

            if (profile.topKeywords.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Text("Temas recurrentes", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.topKeywords.forEach { keyword ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(PrimaryPurple.copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = keyword,
                                color = PrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalityBar(label: String, fraction: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(130.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))))
            )
        }
    }
}

@Composable
fun TopGenresSection(topGenresByMonth: Map<String, List<Pair<String, Int>>>) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SurfaceVariantDark)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(PrimaryPurple, PrimaryPurpleDark)))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.stats_favorite_genres),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val allGenres = topGenresByMonth.values.flatten()
                .groupBy { it.first }
                .mapValues { (_, vals) -> vals.sumOf { it.second } }
                .entries.sortedByDescending { it.value }

            val maxScore = allGenres.firstOrNull()?.value ?: 1
            allGenres.take(6).forEach { (genre, score) ->
                val fraction = score.toFloat() / maxScore.toFloat()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(genre, color = Color.White, fontSize = 14.sp, modifier = Modifier.width(120.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(PrimaryPurple, AccentBlue)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, iconTint: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun StatsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(shimmerBrush())
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerBrush())
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerBrush())
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerBrush())
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerBrush())
        )
        Spacer(Modifier.height(80.dp))
    }
}
