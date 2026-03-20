package com.example.showmateapp.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun FriendsScreen(
    globalNavController: NavController,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var emailInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, Color(0xFF9C27B0)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Amigos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Compara tus favoritos con un amigo",
                color = TextGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email o nombre de usuario", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryPurple) },
                trailingIcon = {
                    if (emailInput.isNotBlank()) {
                        TextButton(onClick = { viewModel.compareWithFriend(emailInput.trim()) }) {
                            Text("Comparar", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedBorderColor = PrimaryPurple.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrimaryPurple
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
            uiState.errorMessage != null -> CenteredMessage(uiState.errorMessage!!)
            uiState.searchDone && uiState.commonShows.isEmpty() -> CenteredMessage(
                title = "No hay series en común",
                body = "Tú y tu amigo aún no habéis marcado las mismas series como favoritas."
            )
            uiState.searchDone -> {
                Text(
                    text = "${uiState.commonShows.size} series en común",
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    letterSpacing = 1.sp
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.commonShows, key = { it.id }) { media ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { globalNavController.navigate(Screen.Detail(media.id)) },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("https://image.tmdb.org/t/p/w185${media.posterPath}")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = media.name,
                                    modifier = Modifier.size(56.dp, 84.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(media.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
                                    if (media.voteAverage > 0f) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${"%.1f".format(media.voteAverage)} ★", color = Color(0xFFFFC107), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> CenteredMessage(
                icon = { Icon(Icons.Default.Group, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp)) },
                title = "Busca a un amigo",
                body = "Introduce el email o nombre de usuario de un amigo para ver las series que tenéis en común."
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    message: String? = null,
    title: String? = null,
    body: String? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.height(16.dp))
            val displayTitle = title ?: message ?: return@Column
            Text(displayTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (body != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(body, color = TextGray, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
    }
}
