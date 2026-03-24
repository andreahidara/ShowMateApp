package com.example.showmateapp.ui.screens.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.ui.components.premium.shimmerBrush
import com.example.showmateapp.ui.navigation.Screen
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleLight
import com.example.showmateapp.ui.theme.StarYellow
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

private val friendsGradient = listOf(PrimaryPurple, Color(0xFF9C27B0))

@Composable
fun FriendsScreen(
    globalNavController: NavController,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var emailInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        item { FriendsHeader() }

        item {
            ModeToggle(
                selected = uiState.mode,
                onSelect = { viewModel.setMode(it); emailInput = "" },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            )
        }

        when (uiState.mode) {
            FriendsMode.COMPARE -> {
                item {
                    CompareInputSection(
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        onCompare = { viewModel.compareWithFriend(emailInput.trim()); emailInput = "" }
                    )
                }

                when {
                    uiState.isLoading -> item { CenteredProgress() }

                    uiState.errorMessage != null -> item {
                        FriendsEmptyState(
                            icon = Icons.Default.Group,
                            title = uiState.errorMessage!!,
                            body = null
                        )
                    }

                    uiState.searchDone && uiState.commonShows.isEmpty() -> item {
                        FriendsEmptyState(
                            icon = Icons.Default.Group,
                            title = "Nada en común aún",
                            body = "Tú y tu amigo todavía no habéis marcado las mismas series como favoritas."
                        )
                    }

                    uiState.searchDone -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${uiState.commonShows.size} en común",
                                        color = PrimaryPurpleLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "series que os gustan a los dos",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        items(uiState.commonShows, key = { it.id }) { media ->
                            CommonShowCard(
                                media = media,
                                onClick = { globalNavController.navigate(Screen.Detail(media.id)) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }

                    else -> item {
                        FriendsEmptyState(
                            icon = Icons.Default.Search,
                            title = "Compara con un amigo",
                            body = "Introduce el email de un amigo para ver las series que tenéis en común."
                        )
                    }
                }
            }

            FriendsMode.GROUP -> {
                item {
                    GroupInputSection(
                        uiState = uiState,
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it; viewModel.clearGroupError() },
                        onAddMember = { viewModel.addGroupMember(emailInput.trim()); emailInput = "" }
                    )
                }

                if (uiState.groupMembers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "GRUPO",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "tú + ${uiState.groupMembers.size}",
                                    color = PrimaryPurpleLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${uiState.groupMembers.size}/4",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    items(uiState.groupMembers, key = { it }) { email ->
                        MemberRow(
                            email = email,
                            onRemove = { viewModel.removeGroupMember(email) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                globalNavController.navigate(
                                    Screen.GroupMatch(uiState.groupMembers.joinToString(","))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buscar matches del grupo", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                } else {
                    item {
                        FriendsEmptyState(
                            icon = Icons.Default.Group,
                            title = "Crea un grupo",
                            body = "Añade amigos por email. Encontraréis las series que tenéis en favoritos en común."
                        )
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun FriendsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Brush.linearGradient(friendsGradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                text = "Amigos",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Compara y descubre juntos",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModeToggle(
    selected: FriendsMode,
    onSelect: (FriendsMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FriendsMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Brush.linearGradient(friendsGradient)
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        FriendsMode.COMPARE -> "Comparar 1:1"
                        FriendsMode.GROUP -> "Grupo"
                    },
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CompareInputSection(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    onCompare: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Introduce el email de tu amigo para ver qué series tenéis en común.",
            color = TextGray,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FriendsInputField(
            value = emailInput,
            onValueChange = onEmailChange,
            placeholder = "Email del amigo",
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = if (emailInput.isNotEmpty()) PrimaryPurple else TextGray, modifier = Modifier.size(20.dp))
            },
            trailingContent = {
                if (emailInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryPurple)
                            .clickable(onClick = onCompare)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Comparar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
private fun GroupInputSection(
    uiState: FriendsUiState,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    onAddMember: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Añade amigos al grupo para encontrar series que os gusten a todos.",
            color = TextGray,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FriendsInputField(
            value = emailInput,
            onValueChange = onEmailChange,
            placeholder = "Email del amigo",
            isError = uiState.groupAddError != null,
            leadingIcon = {
                Icon(Icons.Default.PersonAdd, null, tint = if (emailInput.isNotEmpty()) PrimaryPurple else TextGray, modifier = Modifier.size(20.dp))
            },
            trailingContent = {
                if (emailInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryPurple)
                            .clickable(onClick = onAddMember)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Añadir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
        if (uiState.groupAddError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                uiState.groupAddError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun FriendsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    leadingIcon: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = when {
                    isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    value.isNotEmpty() -> PrimaryPurple.copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leadingIcon()
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextGray, fontSize = 14.sp)
                    }
                    inner()
                }
            )
        }
        trailingContent()
    }
}

@Composable
private fun CommonShowCard(
    media: MediaContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp, 87.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" })
                    .crossfade(true)
                    .build(),
                contentDescription = media.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (media.voteAverage > 0f) {
                    Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(12.dp))
                    Text("%.1f".format(media.voteAverage), color = StarYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                media.firstAirDate?.take(4)?.let { year ->
                    if (media.voteAverage > 0f) Text("·", color = TextGray, fontSize = 12.sp)
                    Text(year, color = TextGray, fontSize = 12.sp)
                }
                media.numberOfSeasons?.let { s ->
                    Text("·", color = TextGray, fontSize = 12.sp)
                    Text("$s temp.", color = TextGray, fontSize = 12.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(PrimaryPurple.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun MemberRow(
    email: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Brush.linearGradient(friendsGradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = email.first().uppercaseChar().toString(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = email,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = TextGray, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun FriendsEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(listOf(PrimaryPurple.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryPurple.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    val shimmer = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmer)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmer)
                    )
                }
            }
        }
    }
}
