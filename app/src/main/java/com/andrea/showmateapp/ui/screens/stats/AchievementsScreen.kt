package com.andrea.showmateapp.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.model.AchievementCategory
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.usecase.AchievementDefs
import com.andrea.showmateapp.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    val filteredAchievements = remember(uiState.achievements, selectedCategory) {
        if (selectedCategory == null) uiState.achievements
        else uiState.achievements.filter { it.category == selectedCategory }
    }
    val achievementRows = remember(filteredAchievements) { filteredAchievements.chunked(2) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logros & Clasificación", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { XpBanner(xp = uiState.xp) }

            item {
                Spacer(Modifier.height(10.dp))
                CategoryFilterRow(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = if (selectedCategory == it) null else it }
                )
                Spacer(Modifier.height(4.dp))
            }

            items(achievementRows, key = { it.first().id }) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { ach ->
                        AchievementCard(ach, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (uiState.leaderboard.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    LeaderboardSection(entries = uiState.leaderboard, myXp = uiState.xp)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun XpBanner(xp: Int) {
    val level = AchievementDefs.levelForXp(xp)
    val progress = AchievementDefs.progressInLevel(xp)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "xpProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(listOf(PrimaryPurpleDark, Color(0xFF1A1040)))
            )
            .border(1.dp, PrimaryPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Nivel ${level.level}", color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(level.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$xp XP total", color = StarYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (level.maxXp != Int.MAX_VALUE) {
                        Text("${level.maxXp + 1} para siguiente nivel", color = TextGray, fontSize = 11.sp)
                    } else {
                        Text("¡Nivel máximo!", color = StarYellow, fontSize = 11.sp)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryPurpleLight, StarYellow)))
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: AchievementCategory?,
    onSelect: (AchievementCategory) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AchievementCategory.entries, key = { it.name }) { cat ->
            val active = selected == cat
            val bg = if (active) PrimaryPurple else SurfaceVariantDark
            val textColor = if (active) Color.White else TextGray
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .then(if (!active) Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("${cat.emoji} ${cat.label}", color = textColor, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val unlocked = achievement.isUnlocked
    val cardAlpha = if (unlocked) 1f else 0.45f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariantDark)
            .then(
                if (unlocked) Modifier.border(1.dp, PrimaryPurple.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (unlocked) PrimaryPurple.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.04f)
                    )
                    .then(if (!unlocked) Modifier.blur(2.dp) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (unlocked) achievement.emoji else "🔒",
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayerAlpha(cardAlpha)
                )
            }
            Text(
                achievement.title,
                color = if (unlocked) Color.White else TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                achievement.description,
                color = TextGray.copy(alpha = if (unlocked) 1f else 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                lineHeight = 14.sp
            )
            if (unlocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarYellow.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("+${achievement.xpReward} XP", color = StarYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("+${achievement.xpReward} XP", color = TextGray.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LeaderboardSection(
    entries: List<IAchievementRepository.LeaderboardEntry>,
    myXp: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(StarYellow.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = StarYellow, modifier = Modifier.size(20.dp))
                }
                Text("Clasificación entre amigos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            entries.forEachIndexed { index, entry ->
                LeaderboardRow(position = index + 1, entry = entry)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tú", color = PrimaryPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("⭐", fontSize = 16.sp) }
                Text("Tú", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("$myXp XP", color = StarYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(position: Int, entry: IAchievementRepository.LeaderboardEntry) {
    val medal = when (position) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (medal != null) {
            Text(medal, fontSize = 18.sp, modifier = Modifier.width(22.dp))
        } else {
            Text("#$position", color = TextGray, fontSize = 12.sp, modifier = Modifier.width(22.dp))
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.username, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(entry.levelName, color = TextGray, fontSize = 11.sp)
        }
        Text("${entry.xp} XP", color = StarYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.graphicsLayerAlpha(alpha: Float): Modifier = this.then(
    Modifier // no-op wrapper; actual alpha is handled per-widget above
)
