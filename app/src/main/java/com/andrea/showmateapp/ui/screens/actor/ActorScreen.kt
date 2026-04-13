package com.andrea.showmateapp.ui.screens.actor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.data.network.MediaContent
import com.andrea.showmateapp.data.network.PersonResponse
import com.andrea.showmateapp.ui.components.premium.TmdbImage
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.StarYellow
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorScreen(
    navController: NavController,
    personId: Int,
    personName: String,
    viewModel: ActorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(personId) { viewModel.loadActor(personId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.person?.name ?: personName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
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
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
            return@Scaffold
        }

        if (uiState.error != null && uiState.person == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error al cargar el perfil", color = TextGray)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.loadActor(personId) }) {
                        Text("Reintentar", color = PrimaryPurple)
                    }
                }
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 105.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActorProfileHeader(person = uiState.person, personName = personName)
            }

            if (uiState.credits.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Filmografía (${uiState.credits.size})",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(uiState.credits, key = { it.id }) { show ->
                    ActorShowCard(
                        show = show,
                        onClick = { navController.navigate(Screen.Detail(show.id, null)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActorProfileHeader(person: PersonResponse?, personName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .semantics { contentDescription = "${person?.name ?: personName} foto de perfil" },
            contentAlignment = Alignment.Center
        ) {
            if (person?.profilePath != null) {
                TmdbImage(
                    path = person.profilePath,
                    contentDescription = "${person.name} foto",
                    size = TmdbUtils.ImageSize.W185,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = person?.name ?: personName,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        person?.knownForDepartment?.takeIf { it.isNotBlank() }?.let { dept ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = dept,
                color = PrimaryPurpleLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            person?.birthday?.takeIf { it.isNotBlank() }?.let { bday ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Cake,
                        contentDescription = "Fecha de nacimiento",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(bday.take(10), color = TextGray, fontSize = 12.sp)
                }
            }
            person?.placeOfBirth?.takeIf { it.isNotBlank() }?.let { place ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Lugar de nacimiento",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = place.take(28),
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if ((person?.popularity ?: 0f) > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Popularidad",
                        tint = StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("%.0f".format(person?.popularity ?: 0f), color = StarYellow, fontSize = 12.sp)
                }
            }
        }

        person?.biography?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(Modifier.height(16.dp))
            var expanded by remember { mutableStateOf(false) }
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = bio,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                    )
                    if (!expanded) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Leer más",
                            color = PrimaryPurpleLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActorShowCard(show: MediaContent, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClickLabel = show.name) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
        ) {
            TmdbImage(
                path = show.posterPath,
                contentDescription = "${show.name} portada",
                size = TmdbUtils.ImageSize.W342,
                modifier = Modifier.fillMaxSize()
            )
            if (show.voteAverage > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "%.1f".format(show.voteAverage),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = show.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
        show.firstAirDate?.take(4)?.let { year ->
            Text(year, color = TextGray, fontSize = 10.sp)
        }
    }
}
