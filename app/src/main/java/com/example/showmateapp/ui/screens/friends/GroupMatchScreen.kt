package com.example.showmateapp.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun GroupMatchScreen(
    navController: NavController,
    memberEmails: List<String>,
    viewModel: GroupMatchViewModel = hiltViewModel()
) {
    LaunchedEffect(memberEmails) {
        viewModel.loadGroupMatches(memberEmails)
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Matches del grupo",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${uiState.members.size + 1} personas en el grupo",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MemberChip(label = "Tú")
            uiState.members.forEach { email ->
                MemberChip(label = email.substringBefore("@"))
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryPurple)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buscando matches...", color = TextGray, fontSize = 14.sp)
                    }
                }
            }
            uiState.errorMessage != null -> {
                GroupEmptyState(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp)) },
                    title = "No se pudo cargar",
                    body = uiState.errorMessage!!
                )
            }
            uiState.strictMatches.isEmpty() && uiState.softMatches.isEmpty() -> {
                GroupEmptyState(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp)) },
                    title = "Sin matches todavía",
                    body = "Nadie del grupo ha marcado las mismas series como favoritas. Cuantos más favoritos tengáis, mejores serán los matches."
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.strictMatches.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Todos lo tienen en favoritos",
                                subtitle = "${uiState.strictMatches.size} series"
                            )
                        }
                        items(uiState.strictMatches, key = { it.media.id }) { item ->
                            GroupMatchCard(
                                item = item,
                                onClick = { navController.navigate(Screen.Detail(item.media.id)) }
                            )
                        }
                    }
                    if (uiState.softMatches.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "Le gusta a la mayoría",
                                subtitle = "${uiState.softMatches.size} series"
                            )
                        }
                        items(uiState.softMatches, key = { "soft_${it.media.id}" }) { item ->
                            GroupMatchCard(
                                item = item,
                                onClick = { navController.navigate(Screen.Detail(item.media.id)) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MemberChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PrimaryPurple.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            color = PrimaryPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = subtitle,
            color = TextGray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GroupMatchCard(
    item: GroupMatchItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://image.tmdb.org/t/p/w185${item.media.posterPath}")
                    .crossfade(true)
                    .build(),
                contentDescription = item.media.name,
                modifier = Modifier
                    .size(56.dp, 84.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.media.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.media.voteAverage > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"%.1f".format(item.media.voteAverage)} ★",
                        color = StarYellow,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                GroupMatchBar(likedBy = item.likedByCount, total = item.totalMembers)
            }
        }
    }
}

@Composable
private fun GroupMatchBar(likedBy: Int, total: Int) {
    val fraction = likedBy.toFloat() / total
    val barColor = when {
        fraction >= 1f -> Color(0xFF4CAF50)
        fraction >= 0.66f -> PrimaryPurple
        else -> Color(0xFFFF9800)
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(barColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = "$likedBy de $total les gusta",
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.7f), barColor))
                    )
            )
        }
    }
}

@Composable
private fun GroupEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, color = TextGray, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}
