package com.andrea.showmateapp.ui.screens.profile.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.ui.components.premium.CardSurface
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.components.premium.outlinedTextFieldColors
import com.andrea.showmateapp.ui.theme.AccentBlue
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.StarYellow
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendCompareScreen(
    navController: NavController,
    initialFriendEmail: String = "",
    viewModel: FriendCompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialFriendEmail) {
        if (initialFriendEmail.isNotBlank()) {
            viewModel.initWithEmail(initialFriendEmail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Comparar con amigo", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CardSurface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentBlue.copy(alpha = 0.15f),
                                shape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Gustos en común",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Descubre qué series tenéis en común",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.friendEmail,
                            onValueChange = viewModel::onEmailChange,
                            label = { Text("Email de tu amigo", color = TextGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedTextFieldColors(),
                            isError = uiState.error != null,
                            supportingText = uiState.error?.let {
                                {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        Button(
                            onClick = viewModel::compare,
                            enabled = !uiState.isLoading && uiState.friendEmail.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Comparar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            if (uiState.hasSearched) {
                uiState.compatibilityScore?.let { score ->
                    item {
                        CompatibilityScoreCard(score = score, friendEmail = uiState.friendEmail)
                    }
                }

                if (uiState.commonShows.isEmpty()) {
                    item {
                        CardSurface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Sin series en común",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Parece que tú y tu amigo tenéis gustos muy diferentes, " +
                                        "o el email no está registrado.",
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "${uiState.commonShows.size} " +
                                "${if (uiState.commonShows.size == 1) "serie" else "series"} en común",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    items(uiState.commonShows, key = { it.id }) { show ->
                        CardSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TmdbImage(
                                    path = show.posterPath,
                                    contentDescription = show.name,
                                    size = TmdbUtils.ImageSize.W185,
                                    modifier = Modifier
                                        .size(width = 60.dp, height = 85.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = show.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 2
                                    )
                                    if (show.voteAverage > 0f) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${"%.1f".format(show.voteAverage)} ★",
                                            color = StarYellow,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = AccentBlue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Os gusta a los dos",
                                            color = AccentBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatibilityScoreCard(score: Int, friendEmail: String) {
    val scoreColor = when {
        score >= 75 -> Color(0xFF4CAF50)
        score >= 50 -> AccentBlue
        score >= 25 -> Color(0xFFFFC107)
        else -> TextGray
    }
    val scoreLabel = when {
        score >= 75 -> "¡Almas gemelas del streaming!"
        score >= 50 -> "Muy buen match de gustos"
        score >= 25 -> "Tenéis algo en común"
        else -> "Gustos muy diferentes"
    }

    CardSurface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Compatibilidad con ${friendEmail.substringBefore("@")}",
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(scoreColor.copy(alpha = 0.25f), scoreColor.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score%",
                    color = scoreColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = scoreLabel,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
