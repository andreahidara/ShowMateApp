package com.andrea.showmateapp.ui.screens.friends

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrea.showmateapp.data.model.GroupFilters
import com.andrea.showmateapp.data.model.MemberVoteDoc
import com.andrea.showmateapp.data.model.MediaContent
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.navigation.Screen
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.util.GenreMapper
import com.andrea.showmateapp.util.TmdbUtils
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun GroupMatchScreen(
    navController: NavController,
    memberEmails: List<String>,
    viewModel: GroupMatchViewModel = hiltViewModel()
) {
    LaunchedEffect(memberEmails) { viewModel.loadGroupMatches(memberEmails) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark).statusBarsPadding()) {
        AnimatedContent(
            targetState = uiState.phase,
            transitionSpec = {
                (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.95f))
                    .togetherWith(fadeOut(tween(250)))
            },
            label = "phase"
        ) { phase ->
            when (phase) {
                GroupPhase.LOADING -> LoadingContent()
                GroupPhase.LOBBY -> LobbyContent(uiState, viewModel, navController)
                GroupPhase.VOTING -> VotingContent(uiState, viewModel)
                GroupPhase.MATCH_FOUND -> MatchFoundContent(uiState, viewModel, navController)
                GroupPhase.NO_MATCH -> NoMatchContent(uiState, viewModel, navController)
            }
        }

        if (uiState.showMatchCelebration) {
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
        }
    }

    if (uiState.showFiltersSheet) {
        FiltersBottomSheet(
            filters = uiState.filters,
            onDismiss = viewModel::hideFilters,
            onApply = viewModel::updateFilters
        )
    }

    if (uiState.showNightTitleDialog) {
        NightTitleDialog(
            initial = uiState.nightTitle,
            onConfirm = viewModel::saveNightTitle,
            onDismiss = viewModel::dismissNightTitleDialog
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(20.dp))
        Text("Creando la sala…", color = TextGray, fontSize = 15.sp)
    }
}

@Composable
private fun LobbyContent(uiState: GroupMatchUiState, viewModel: GroupMatchViewModel, navController: NavController) {
    val session = uiState.session
    val isHost = viewModel.isHost()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                Text("🎮 Sala de votación", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (session != null) {
                    Text(
                        "Sesión #${session.id.take(6).uppercase()}",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = viewModel::showFilters) {
                Icon(Icons.Default.Tune, null, tint = PrimaryPurpleLight)
            }
        }

        Spacer(Modifier.height(8.dp))

        SectionTitle("PARTICIPANTES", modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val allMembers = session?.memberEmails ?: (listOf("tú") + uiState.members)
            items(allMembers, key = { it }) { email ->
                MemberChipLobby(email = email, isHost = email == session?.hostEmail)
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle("CANDIDATOS", modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (uiState.isComputingCandidates) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PrimaryPurple,
                        strokeWidth = 2.dp
                    )
                    Text("Analizando gustos del grupo…", color = TextGray, fontSize = 14.sp)
                }
            } else {
                val count = uiState.candidates.size.takeIf { it > 0 }
                    ?: session?.candidateIds?.size ?: 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (count > 0) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        null,
                        tint = if (count > 0) SuccessGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (count > 0) "$count series listas para votar" else "Preparando series…",
                        color = if (count > 0) Color.White else TextGray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (uiState.filters.maxEpisodeDuration > 0 || uiState.filters.excludedGenreIds.isNotEmpty()) {
            ActiveFiltersBanner(uiState.filters, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))

        if (isHost) {
            val canStart = (uiState.candidates.isNotEmpty() || (session?.candidateIds?.isNotEmpty() == true)) &&
                !uiState.isComputingCandidates

            val infiniteTransition = rememberInfiniteTransition(label = "startPulse")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (canStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(PrimaryPurple.copy(alpha = glowAlpha * 0.25f))
                    )
                }
                Button(
                    onClick = viewModel::startVoting,
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        disabledContainerColor = PrimaryPurple.copy(alpha = 0.3f)
                    )
                ) {
                    Text("🚀", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("¡Iniciar votación!", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "waitDots")
            val dotPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                label = "dots"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceDark)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎮", fontSize = 20.sp)
                    Column {
                        Text(
                            "Esperando al host",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            buildString {
                                append("Preparando la sala")
                                repeat((dotPhase.toInt() % 3) + 1) { append("·") }
                            },
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun VotingContent(uiState: GroupMatchUiState, viewModel: GroupMatchViewModel) {
    if (uiState.isVotingDone) {
        WaitingForOthersContent(uiState)
        return
    }

    val candidate = uiState.currentCandidate ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${uiState.currentIndex + 1} / ${uiState.candidates.size}",
                    color = TextGray,
                    fontSize = 12.sp
                )
                LinearProgressIndicator(
                    progress = { uiState.votingProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = PrimaryPurple,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }
            VetoButton(used = uiState.myVetoUsed, onClick = viewModel::useVeto)
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.session?.memberEmails ?: emptyList()) { email ->
                MemberProgress(
                    email = email,
                    votedCount = uiState.memberVoteCount(email),
                    total = uiState.candidates.size
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LiveVoteBanner(
            mediaId = candidate.id,
            allVotes = uiState.allVotes,
            members = uiState.session?.memberEmails ?: emptyList(),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            key(candidate.id) {
                VoteSwipeCard(
                    media = candidate,
                    allVotes = uiState.allVotes,
                    members = uiState.session?.memberEmails ?: emptyList(),
                    onVoteYes = viewModel::voteYes,
                    onVoteNo = viewModel::voteNo,
                    onVoteMaybe = viewModel::voteMaybe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BigVoteButton(
                label = "✗",
                sublabel = "No",
                color = ErrorRed,
                onClick = viewModel::voteNo,
                modifier = Modifier.weight(1f)
            )
            BigVoteButton(
                label = "?",
                sublabel = "Quizás",
                color = StarYellow,
                onClick = viewModel::voteMaybe,
                modifier = Modifier.weight(1f)
            )
            BigVoteButton(
                label = "✓",
                sublabel = "Sí",
                color = SuccessGreen,
                onClick = viewModel::voteYes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WaitingForOthersContent(uiState: GroupMatchUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 60.sp)
        Spacer(Modifier.height(16.dp))
        Text("¡Ya votaste todo!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            "Esperando al resto del grupo…",
            color = TextGray.copy(alpha = alpha),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        val session = uiState.session
        if (session != null) {
            session.memberEmails.forEach { email ->
                val ready = uiState.allVotes[email]?.ready == true
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (ready) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (ready) SuccessGreen else TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        email.substringBefore("@"),
                        color = if (ready) Color.White else TextGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveVoteBanner(
    mediaId: Int,
    allVotes: Map<String, MemberVoteDoc>,
    members: List<String>,
    modifier: Modifier = Modifier
) {
    if (members.size < 2) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("En vivo", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        members.forEach { email ->
            val vote = allVotes[email]
            val emoji = when {
                vote == null -> "⬜"
                mediaId in vote.yes -> "✅"
                mediaId in vote.no -> "❌"
                mediaId in vote.maybe -> "❓"
                else -> "⬜"
            }
            val initial = email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            when {
                                vote == null -> Color.White.copy(alpha = 0.06f)
                                mediaId in vote.yes -> SuccessGreen.copy(alpha = 0.25f)
                                mediaId in vote.no -> ErrorRed.copy(alpha = 0.25f)
                                mediaId in vote.maybe -> StarYellow.copy(alpha = 0.25f)
                                else -> Color.White.copy(alpha = 0.06f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(emoji, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MatchFoundContent(
    uiState: GroupMatchUiState,
    viewModel: GroupMatchViewModel,
    navController: NavController
) {
    val media = uiState.matchedMedia
    val context = LocalContext.current

    val scale = remember { androidx.compose.animation.core.Animatable(0.8f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉 ¡MATCH!", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text("¡Todos queréis ver esta!", color = PrimaryPurpleLight, fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))

        if (media != null) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .width(200.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(24.dp, RoundedCornerShape(20.dp))
            ) {
                TmdbImage(
                    path = media.posterPath,
                    contentDescription = media.name,
                    size = TmdbUtils.ImageSize.W342,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.9f)))
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(media.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 2)
                    if (media.voteAverage > 0f) {
                        Text("⭐ ${"%.1f".format(media.voteAverage)}", color = StarYellow, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        val mediaProviders = media?.watchProviders
        val esProviders = mediaProviders?.results?.get("ES")
        if (esProviders?.flatrate?.isNotEmpty() == true) {
            Text("Disponible en:", color = TextGray, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(esProviders.flatrate ?: emptyList()) { provider ->
                    TmdbImage(
                        path = provider.logoPath,
                        contentDescription = provider.providerName,
                        size = TmdbUtils.ImageSize.W154,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (media != null) {
            val watchLink = media.watchProviders?.results?.get("ES")?.link
            Button(
                onClick = {
                    if (watchLink != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watchLink))
                        context.startActivity(intent)
                    } else {
                        navController.navigate(Screen.Detail(media.id))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Icon(
                    if (watchLink != null) Icons.AutoMirrored.Filled.Launch else Icons.Default.PlayCircle,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (watchLink != null) "¡A verla ahora!" else "Ver detalles",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        val members = uiState.session?.memberEmails ?: emptyList()
        val matchedMedia = uiState.matchedMedia
        if (members.isNotEmpty() && matchedMedia != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                members.forEach { email ->
                    val vote = uiState.allVotes[email]
                    val matchedId = matchedMedia.id
                    val emoji = when {
                        vote == null -> "⬜"
                        matchedId in vote.yes -> "✅"
                        matchedId in vote.maybe -> "❓"
                        else -> "⬜"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 22.sp)
                        Text(
                            email.substringBefore("@").take(8),
                            color = TextGray,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

    }
}

@Composable
private fun NoMatchContent(uiState: GroupMatchUiState, viewModel: GroupMatchViewModel, navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😅", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Sin match esta vez",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No hay ninguna serie en la que todo el grupo haya coincidido.",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Intentar de nuevo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun VoteSwipeCard(
    media: MediaContent,
    allVotes: Map<String, MemberVoteDoc>,
    members: List<String>,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    onVoteMaybe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val thresholdH = 220f
    val thresholdV = 180f

    val yesAlpha = (offsetX.value / thresholdH).coerceIn(0f, 1f)
    val noAlpha = (-offsetX.value / thresholdH).coerceIn(0f, 1f)
    val maybeAlpha = (-offsetY.value / thresholdV).coerceIn(0f, 1f)
    val rotation = (offsetX.value / 20f).coerceIn(-12f, 12f)

    val yesCount = remember(allVotes, media.id) { allVotes.values.count { media.id in it.yes } }
    val noCount = remember(allVotes, media.id) { allVotes.values.count { media.id in it.no } }
    val maybeCount = remember(allVotes, media.id) { allVotes.values.count { media.id in it.maybe } }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation
                shadowElevation = 24.dp.toPx()
            }
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val bounceSpec = spring<Float>(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                            when {
                                offsetX.value > thresholdH -> {
                                    offsetX.animateTo(1400f, tween(210))
                                    onVoteYes()
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                                offsetX.value < -thresholdH -> {
                                    offsetX.animateTo(-1400f, tween(210))
                                    onVoteNo()
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                                offsetY.value < -thresholdV -> {
                                    offsetY.animateTo(-1400f, tween(210))
                                    onVoteMaybe()
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                                else -> {
                                    coroutineScope.launch { offsetX.animateTo(0f, bounceSpec) }
                                    coroutineScope.launch { offsetY.animateTo(0f, bounceSpec) }
                                }
                            }
                        }
                    },
                    onDrag = { _, amount ->
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + amount.x)
                            offsetY.snapTo(offsetY.value + amount.y)
                        }
                    }
                )
            }
    ) {
        TmdbImage(
            path = media.posterPath,
            contentDescription = media.name,
            size = TmdbUtils.ImageSize.W342,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            BackgroundDark.copy(0.6f),
                            BackgroundDark.copy(0.97f)
                        ),
                        startY = 0f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                media.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (media.voteAverage > 0f) {
                    Text(
                        "⭐ ${"%.1f".format(media.voteAverage)}",
                        color = StarYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                media.firstAirDate?.take(4)?.let { year ->
                    Text("·  $year", color = TextGray, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                media.safeGenreIds.take(3).forEach { genreId ->
                    GenreChip(GenreMapper.getGenreName(genreId))
                }
            }
        }

        if (members.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundDark.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (yesCount > 0) {
                    Text(
                        "✅ $yesCount",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (maybeCount > 0) {
                    Text(
                        "❓ $maybeCount",
                        color = StarYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (noCount > 0) Text("❌ $noCount", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (yesAlpha < 0.05f && noAlpha < 0.05f && maybeAlpha < 0.05f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundDark.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("← No", color = ErrorRed.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("↑ Quizás", color = StarYellow.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("Sí →", color = SuccessGreen.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }

        if (yesAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    SuccessGreen.copy(alpha = yesAlpha * 0.35f),
                    RoundedCornerShape(24.dp)
                )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuccessGreen)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .alpha(yesAlpha)
            ) {
                Text("✓ SÍ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }

        if (noAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    ErrorRed.copy(alpha = noAlpha * 0.35f),
                    RoundedCornerShape(24.dp)
                )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .alpha(noAlpha)
            ) {
                Text("✗ NO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }

        if (maybeAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    StarYellow.copy(alpha = maybeAlpha * 0.3f),
                    RoundedCornerShape(24.dp)
                )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StarYellow)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .alpha(maybeAlpha)
            ) {
                Text("❓ QUIZÁS", color = BackgroundDark, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private data class Particle(
    val x0: Float,
    val speed: Float,
    val hSpeed: Float,
    val phase: Float,
    val freq: Float,
    val color: Color,
    val size: Float,
    val rotSpeed: Float,
    val isCircle: Boolean
)

private val confettiColors = listOf(
    Color(0xFF9C27B0),
    Color(0xFFFFD700),
    Color(0xFF4CAF50),
    Color(0xFFFF5722),
    Color(0xFFE91E63),
    Color(0xFF2196F3),
    Color(0xFF00BCD4),
    Color(0xFFFFEB3B)
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(110) {
            Particle(
                x0 = Random.nextFloat(),
                speed = 0.20f + Random.nextFloat() * 0.70f,
                hSpeed = (Random.nextFloat() - 0.5f) * 0.30f,
                phase = Random.nextFloat() * 6.28f,
                freq = 0.5f + Random.nextFloat() * 2.5f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = 8f + Random.nextFloat() * 16f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 4f,
                isCircle = Random.nextBoolean()
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "confetti_t"
    )

    val emojiScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emoji_scale"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val rawY = (time * p.speed + p.phase / 6.28f) % 1.3f - 0.15f
                val y = rawY * size.height
                val xNorm = (
                    (
                        p.x0 + p.hSpeed * time +
                            sin((time * p.freq * 6.28f + p.phase).toDouble()).toFloat() * 0.05f
                        )
                        .coerceIn(0f, 1f)
                    )
                val x = xNorm * size.width
                withTransform({ rotate(time * 360f * p.rotSpeed, Offset(x, y)) }) {
                    if (p.isCircle) {
                        drawCircle(p.color, p.size / 2f, Offset(x, y))
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(x - p.size / 2f, y - p.size * 0.3f),
                            size = Size(p.size, p.size * 0.55f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .graphicsLayer {
                    scaleX = emojiScale
                    scaleY = emojiScale
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("🎉", "🏆", "🎬", "🍿", "✨").forEach { emoji ->
                Text(emoji, fontSize = 32.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersBottomSheet(filters: GroupFilters, onDismiss: () -> Unit, onApply: (GroupFilters) -> Unit) {
    var duration by remember { mutableIntStateOf(filters.maxEpisodeDuration) }
    var excluded by remember { mutableStateOf(filters.excludedGenreIds.toSet()) }

    val genreOptions = listOf(
        10759 to "Acción/Aventura", 35 to "Comedia", 18 to "Drama",
        80 to "Crimen", 10765 to "Sci-Fi/Fantasía", 9648 to "Misterio",
        10751 to "Familiar", 16 to "Animación", 99 to "Documental"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("🎛️ Filtros del grupo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Duración máx. por episodio",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (duration == 0) "Sin límite" else "$duration min",
                        color = PrimaryPurpleLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 0f..120f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Excluir géneros", color = Color.White, fontSize = 14.sp)
                genreOptions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (id, name) ->
                            val isExcluded = id in excluded
                            FilterChip(
                                selected = isExcluded,
                                onClick = {
                                    excluded = if (isExcluded) excluded - id else excluded + id
                                },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ErrorRed.copy(alpha = 0.2f),
                                    selectedLabelColor = ErrorRed,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = TextGray
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onApply(GroupFilters(duration, excluded.toList())) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Aplicar filtros", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun NightTitleDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("🌙 Nombra la noche", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Viernes de terror 🎃", color = TextGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Guardar", color = PrimaryPurpleLight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) }
        }
    )
}

@Composable
private fun MemberChipLobby(email: String, isHost: Boolean) {
    val label = email.substringBefore("@").take(10)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(
                        if (isHost) {
                            listOf(PrimaryPurple, Color(0xFF9C27B0))
                        } else {
                            listOf(SurfaceDark, SurfaceDark)
                        }
                    ),
                    CircleShape
                )
                .border(2.dp, if (isHost) PrimaryPurple else Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(label, color = if (isHost) PrimaryPurpleLight else TextGray, fontSize = 11.sp, maxLines = 1)
        if (isHost) Text("host", color = PrimaryPurple.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun MemberProgress(email: String, votedCount: Int, total: Int) {
    val progress = if (total > 0) votedCount.toFloat() / total else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            PrimaryPurple.copy(alpha = progress),
                            PrimaryPurple.copy(alpha = progress),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    CircleShape
                )
                .border(
                    1.5.dp,
                    if (progress >= 1f) SuccessGreen else PrimaryPurple.copy(alpha = 0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text("$votedCount", color = TextGray, fontSize = 9.sp)
    }
}

@Composable
private fun VetoButton(used: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (used) Color.White.copy(alpha = 0.04f) else ErrorRed.copy(alpha = 0.15f))
            .border(1.dp, if (used) Color.White.copy(alpha = 0.08f) else ErrorRed.copy(alpha = 0.4f), CircleShape)
            .clickable(enabled = !used, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (used) "🚫" else "🔥",
            fontSize = 20.sp,
            modifier = Modifier.alpha(if (used) 0.35f else 1f)
        )
    }
}

@Composable
private fun BigVoteButton(
    label: String,
    sublabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(sublabel, color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GenreChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(name, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}

@Composable
private fun ActiveFiltersBanner(filters: GroupFilters, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PrimaryPurple.copy(alpha = 0.08f))
            .border(1.dp, PrimaryPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.FilterList, null, tint = PrimaryPurpleLight, modifier = Modifier.size(16.dp))
        val parts = buildList {
            if (filters.maxEpisodeDuration > 0) add("≤${filters.maxEpisodeDuration} min/ep")
            filters.excludedGenreIds.forEach { id -> add("sin ${GenreMapper.getGenreName(id)}") }
        }
        Text(
            parts.joinToString(" · "),
            color = PrimaryPurpleLight,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.shadow(elevation: androidx.compose.ui.unit.Dp, shape: RoundedCornerShape) =
    this.then(Modifier.graphicsLayer { shadowElevation = elevation.value })

