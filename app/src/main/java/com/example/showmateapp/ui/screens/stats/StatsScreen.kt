package com.example.showmateapp.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.domain.usecase.GetViewerPersonalityUseCase
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Estadísticas",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak cards row
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
                        icon = Icons.Default.TrendingUp,
                        iconTint = PrimaryPurple,
                        value = "${uiState.longestStreak}",
                        label = "Racha récord",
                        subtitle = "días"
                    )
                }

                // Episodes cards row
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

                // Activity by month
                if (uiState.activityByMonth.isNotEmpty()) {
                    ActivityChart(activityByMonth = uiState.activityByMonth)
                }

                // Top genres
                if (uiState.topGenresByMonth.isNotEmpty()) {
                    TopGenresSection(topGenresByMonth = uiState.topGenresByMonth)
                }

                // Viewer personality profile
                uiState.personalityProfile?.let { PersonalitySection(it) }

                Spacer(modifier = Modifier.height(32.dp))
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ActivityChart(activityByMonth: Map<String, Int>) {
    val sortedEntries = remember(activityByMonth) {
        activityByMonth.entries.sortedBy { it.key }.takeLast(6)
    }
    val maxValue = remember(activityByMonth) { sortedEntries.maxOfOrNull { it.value } ?: 1 }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Actividad mensual",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                sortedEntries.forEach { (month, count) ->
                    val barHeight = if (maxValue > 0) (count.toFloat() / maxValue * 120).dp else 4.dp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$count",
                            color = PrimaryPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(PrimaryPurple, Color(0xFF9C27B0))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = month.takeLast(5),
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalitySection(profile: GetViewerPersonalityUseCase.PersonalityProfile) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Perfil de espectador", color = TextGray, fontSize = 11.sp)
                    Text(profile.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(130.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))))
            )
        }
    }
}

@Composable
fun TopGenresSection(topGenresByMonth: Map<String, List<Pair<String, Int>>>) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Géneros favoritos",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            val allGenres = topGenresByMonth.values.flatten()
                .groupBy { it.first }
                .mapValues { (_, vals) -> vals.sumOf { it.second } }
                .entries
                .sortedByDescending { it.value }

            val maxScore = allGenres.firstOrNull()?.value ?: 1

            allGenres.take(6).forEach { (genre, score) ->
                val fraction = score.toFloat() / maxScore.toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = genre,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PrimaryPurple, Color(0xFF9C27B0))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
