package com.andrea.showmateapp.ui.screens.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.data.model.FriendInfo
import com.andrea.showmateapp.data.model.FriendRequest
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.ui.components.shimmerBrush
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.ErrorDark
import com.andrea.showmateapp.ui.theme.PillGenre
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.SuccessDark
import com.andrea.showmateapp.ui.theme.SuccessGreen
import com.andrea.showmateapp.ui.theme.SurfaceDark
import com.andrea.showmateapp.ui.theme.TextGray

private val socialGradient = listOf(PrimaryPurple, PillGenre)

@Composable
fun FriendsScreen(globalNavController: NavController, viewModel: FriendsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissSuccess()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            item { SocialHeader(unreadCount = uiState.unreadRequestCount) }

            item {
                SocialTabRow(
                    selected = uiState.tab,
                    onSelect = viewModel::setTab,
                    unreadRequestCount = uiState.unreadRequestCount,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }

            when (uiState.tab) {
                FriendsTab.FRIENDS -> friendsTabItems(uiState, viewModel, globalNavController)
                FriendsTab.REQUESTS -> requestsTabItems(uiState, viewModel)
                FriendsTab.SEARCH -> searchTabItems(uiState, viewModel)
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        AnimatedVisibility(
            visible = uiState.successMessage != null || uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) {
            val isSuccess = uiState.successMessage != null
            val msg = uiState.successMessage ?: uiState.errorMessage ?: ""
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSuccess) {
                    SuccessDark.copy(alpha = 0.95f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.95f)
                }
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.friendsTabItems(
    uiState: FriendsUiState,
    viewModel: FriendsViewModel,
    navController: NavController
) {
    when {
        uiState.isFriendsLoading -> item { ShimmerList() }

        uiState.friends.isEmpty() -> item {
            SocialEmptyState(
                icon = Icons.Default.Group,
                title = "Sin amigos todavía",
                body = "Ve a Buscar para añadir personas con gustos similares."
            )
        }

        else -> {
            if (uiState.groupMembers.isNotEmpty()) {
                item {
                    GroupBar(
                        members = uiState.groupMembers,
                        onRemove = viewModel::removeGroupMember,
                        onLaunch = {
                            navController.navigate(
                                Screen.GroupMatch(uiState.groupMembers.joinToString(","))
                            )
                        },
                        error = uiState.groupAddError
                    )
                }
            }

            items(uiState.friends, key = { it.uid }) { friend ->
                FriendCard(
                    friend = friend,
                    onAddToGroup = { viewModel.addFriendToGroup(friend) },
                    onRemove = { viewModel.removeFriend(friend.uid) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.requestsTabItems(
    uiState: FriendsUiState,
    viewModel: FriendsViewModel
) {
    when {
        uiState.isRequestsLoading -> item { ShimmerList(itemHeight = 72) }

        uiState.incomingRequests.isEmpty() && uiState.outgoingRequests.isEmpty() -> item {
            SocialEmptyState(
                icon = Icons.Default.PersonAdd,
                title = "Sin solicitudes pendientes",
                body = "Busca amigos en la pestaña Buscar."
            )
        }

        else -> {
            if (uiState.incomingRequests.isNotEmpty()) {
                item {
                    SectionLabel(
                        text = "RECIBIDAS",
                        count = uiState.incomingRequests.size,
                        highlight = true,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.incomingRequests, key = { "in_${it.id}" }) { req ->
                    IncomingRequestCard(
                        request = req,
                        onAccept = { viewModel.acceptRequest(req) },
                        onReject = { viewModel.rejectRequest(req.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.outgoingRequests.isNotEmpty()) {
                item {
                    SectionLabel(
                        text = "ENVIADAS",
                        count = uiState.outgoingRequests.size,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (uiState.incomingRequests.isNotEmpty()) 20.dp else 8.dp,
                            bottom = 8.dp
                        )
                    )
                }
                items(uiState.outgoingRequests, key = { "out_${it.id}" }) { req ->
                    OutgoingRequestCard(
                        request = req,
                        onCancel = { viewModel.cancelOutgoingRequest(req.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchTabItems(
    uiState: FriendsUiState,
    viewModel: FriendsViewModel
) {
    item {
        SocialSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            isSearching = uiState.isSearching,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        )
    }

    when {
        uiState.searchQuery.length < 2 -> item {
            SocialEmptyState(
                icon = Icons.Default.PersonSearch,
                title = "Busca por nombre",
                body = "Escribe al menos 2 caracteres para encontrar usuarios.",
                compact = true
            )
        }

        uiState.isSearching -> item { ShimmerList(count = 3) }

        uiState.searchResults.isEmpty() -> item {
            SocialEmptyState(
                icon = Icons.Default.SearchOff,
                title = "Sin resultados",
                body = "No hay usuarios con ese nombre. Prueba con otro.",
                compact = true
            )
        }

        else -> {
            items(uiState.searchResults, key = { "sr_${it.userId}" }) { user ->
                val alreadyFriend = uiState.friends.any { it.uid == user.userId }
                val requestSent = user.userId in uiState.sentRequestUids
                SearchResultCard(
                    user = user,
                    alreadyFriend = alreadyFriend,
                    requestSent = requestSent,
                    onSendRequest = { viewModel.sendFriendRequest(user.userId, user.username) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SocialHeader(unreadCount: Int) {
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
                .background(Brush.linearGradient(socialGradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Amigos",
                style = TextStyle(brush = Brush.linearGradient(socialGradient)),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp, bottom = 2.dp)
                    .width(38.dp)
                    .height(3.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(Brush.linearGradient(socialGradient))
            )
            Text("Descubre y conecta", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SocialTabRow(
    selected: FriendsTab,
    onSelect: (FriendsTab) -> Unit,
    unreadRequestCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FriendsTab.entries.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(socialGradient)
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                if (tab == FriendsTab.REQUESTS && unreadRequestCount > 0 && !isSelected) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TabLabel(tab = tab, isSelected = false)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp)
                                .size(16.dp)
                                .background(ErrorDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadRequestCount > 9) "9+" else unreadRequestCount.toString(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    TabLabel(tab = tab, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun TabLabel(tab: FriendsTab, isSelected: Boolean) {
    val label = when (tab) {
        FriendsTab.FRIENDS -> "Amigos"
        FriendsTab.REQUESTS -> "Solicitudes"
        FriendsTab.SEARCH -> "Buscar"
    }
    Text(
        text = label,
        color = if (isSelected) Color.White else TextGray,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun FriendCard(
    friend: FriendInfo,
    onAddToGroup: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val compatColor = compatibilityColor(friend.compatibilityScore)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarCircle(label = friend.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(friend.email, color = TextGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CompatibilityBadge(score = friend.compatibilityScore, color = compatColor)
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FriendActionButton(
                    label = "Grupo",
                    icon = Icons.Default.Group,
                    onClick = onAddToGroup,
                    modifier = Modifier.weight(1f)
                )
                FriendActionButton(
                    label = "Eliminar",
                    icon = Icons.Default.PersonRemove,
                    onClick = onRemove,
                    isDestructive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FriendActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else PrimaryPurple
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompatibilityBadge(score: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$score%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GroupBar(
    members: List<String>,
    onRemove: (String) -> Unit,
    onLaunch: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryPurple.copy(alpha = 0.08f))
            .border(1.dp, PrimaryPurple.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "GRUPO",
                color = PrimaryPurpleLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text("tú + ${members.size}", color = TextGray, fontSize = 11.sp)
        }
        members.forEach { email ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    email.substringBefore("@"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { onRemove(email) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Eliminar",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Button(
            onClick = onLaunch,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ver matches del grupo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarCircle(label = request.fromUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                request.fromUsername,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text("quiere ser tu amigo", color = TextGray, fontSize = 12.sp)
        }
        IconButton(
            onClick = onReject,
            modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.06f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = TextGray, modifier = Modifier.size(16.dp))
        }
        IconButton(
            onClick = onAccept,
            modifier = Modifier.size(36.dp).background(PrimaryPurple.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Aceptar",
                tint = PrimaryPurple,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun OutgoingRequestCard(request: FriendRequest, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarCircle(label = request.toUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?", dim = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                request.toUsername,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text("Pendiente de respuesta", color = TextGray, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Cancelar", color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SocialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = if (query.isNotEmpty()) PrimaryPurple.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = if (query.isNotEmpty()) PrimaryPurple else TextGray,
            modifier = Modifier.size(20.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Buscar por nombre de usuario…", color = TextGray, fontSize = 14.sp)
                    inner()
                }
            )
        }
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryPurple, strokeWidth = 2.dp)
        } else if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Limpiar",
                tint = TextGray,
                modifier = Modifier.size(
                    18.dp
                ).clickable {
                    onQueryChange("")
                }
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    user: UserProfile,
    alreadyFriend: Boolean,
    requestSent: Boolean,
    onSendRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarCircle(label = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.username.ifBlank {
                    "Sin nombre"
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
            if (user.email.isNotBlank()) {
                Text(
                    user.email,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when {
            alreadyFriend -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuccessGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Amigo", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

            requestSent -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TextGray.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Pendiente", color = TextGray, fontSize = 12.sp) }

            else -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPurple)
                    .clickable(onClick = onSendRequest)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Añadir", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AvatarCircle(label: String, dim: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (dim) {
                    Brush.linearGradient(listOf(TextGray.copy(alpha = 0.3f), TextGray.copy(alpha = 0.15f)))
                } else {
                    Brush.linearGradient(socialGradient)
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionLabel(text: String, count: Int? = null, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (highlight) Color(0xFFE53935).copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = if (highlight) Color(0xFFE53935) else PrimaryPurpleLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SocialEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = if (compact) 24.dp else 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 56.dp else 80.dp)
                .background(
                    Brush.radialGradient(listOf(PrimaryPurple.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryPurple.copy(alpha = 0.5f),
                modifier = Modifier.size(if (compact) 28.dp else 40.dp)
            )
        }
        Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
        Text(
            title,
            color = Color.White,
            fontSize = if (compact) 15.sp else 18.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(body, color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
    }
}

@Composable
private fun ShimmerList(count: Int = 4, itemHeight: Int = 80) {
    val shimmer = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmer)
            )
        }
    }
}

private fun compatibilityColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF4CAF50)
    score >= 50 -> Color(0xFF2196F3)
    score >= 25 -> Color(0xFFFFC107)
    else -> TextGray
}
