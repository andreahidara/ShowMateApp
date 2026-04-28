package com.andrea.showmateapp.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrea.showmateapp.ui.components.AuthBackground
import com.andrea.showmateapp.ui.components.TmdbImage
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.util.TmdbUtils

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel(), onFinish: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onFinish()
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (state.step < 5) {
                OnboardingHeader(
                    step = state.step,
                    onBack = { viewModel.goBack() }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(380)) { if (forward) it else -it } + fadeIn(tween(280)))
                            .togetherWith(
                                slideOutHorizontally(tween(280)) { if (forward) -it else it } + fadeOut(tween(200))
                            )
                    },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        1 -> GenreStep(state, viewModel)
                        2 -> ShowsStep(state, viewModel)
                        3 -> PreferencesStep(state, viewModel)
                        4 -> AnalysisStep(state)
                        else -> PersonalityStep(state, viewModel)
                    }
                }
            }

            if (state.step != 4) {
                OnboardingButton(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                TextButton(onClick = onBack) {
                    Text("←", color = TextGray, fontSize = 18.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$step / 4",
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { index ->
                val filled = index < step
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.2f,
                    animationSpec = tween(400),
                    label = "seg_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = animatedAlpha))
                )
            }
        }
    }
}

@Composable
private fun OnboardingButton(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val enabled = when (state.step) {
        1 -> state.selectedGenres.size in 3..5
        2 -> true
        3 -> state.episodeLengthPref != null && state.statusPref != null && state.dubbedPref != null
        5 -> true
        else -> false
    }
    val label = when (state.step) {
        1 -> if (state.selectedGenres.size < 3) "Elige al menos 3 géneros" else "Continuar →"
        2 -> if (state.watchedShowIds.isEmpty()) {
            "Saltar este paso →"
        } else {
            "Continuar (${state.watchedShowIds.size} vistas) →"
        }
        3 -> if (!enabled) "Responde todas las preguntas" else "Analizar mis gustos →"
        else -> "¡Empezar ShowMate!"
    }

    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Button(
            onClick = {
                if (state.step == 5) {
                    viewModel.completeOnboarding()
                } else {
                    viewModel.advance()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled && !state.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.step == 5) Color(0xFF4CAF50) else Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = label,
                    color = if (enabled) Color.Black else Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GenreStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Qué tipo de series\nte enganchan?",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Elige entre 3 y 5 géneros favoritos",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        val count = state.selectedGenres.size
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                val filled = i < count
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.7f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label = "dot_$i"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .scale(scale)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (filled) PrimaryPurple else Color.White.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.availableGenres.entries.toList(), key = { it.key }) { (id, name) ->
                val isSelected = state.selectedGenres.contains(id)
                val disabled = !isSelected && state.selectedGenres.size >= 5
                GenreCard(
                    name = name,
                    emoji = state.genreEmojis[id] ?: "🎬",
                    isSelected = isSelected,
                    disabled = disabled,
                    posterPath = state.genrePosters[id],
                    onClick = { if (!disabled) viewModel.toggleGenre(id) }
                )
            }
        }
    }
}

@Composable
private fun GenreCard(
    name: String,
    emoji: String,
    isSelected: Boolean,
    disabled: Boolean,
    posterPath: String?,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "border_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (disabled) 0.4f else 1f,
        animationSpec = tween(200),
        label = "disabled_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = PrimaryPurple.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (posterPath != null) {
            TmdbImage(
                path = posterPath,
                contentDescription = null,
                size = TmdbUtils.ImageSize.W342,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (isSelected) 0.0f else 0.08f),
                            Color.Black.copy(alpha = if (isSelected) 0.35f else 0.52f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                color = Color.White.copy(alpha = contentAlpha),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ShowsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Cuáles de estas ya has visto?",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Toca para marcar · ❤️ para las que te encantaron",
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoadingShows) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.popularShows, key = { it.id }) { show ->
                    val watched = state.watchedShowIds.contains(show.id)
                    val loved = state.lovedShowIds.contains(show.id)
                    ShowPosterCard(
                        posterPath = show.posterPath,
                        name = show.name,
                        watched = watched,
                        loved = loved,
                        onToggleWatched = { viewModel.toggleWatched(show.id) },
                        onToggleLoved = { viewModel.toggleLoved(show.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowPosterCard(
    posterPath: String?,
    name: String,
    watched: Boolean,
    loved: Boolean,
    onToggleWatched: () -> Unit,
    onToggleLoved: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (watched) 1f else 0.97f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "show_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (watched) 2.dp else 0.dp,
                color = if (loved) Color(0xFFE91E63) else if (watched) Color(0xFF4CAF50) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggleWatched)
    ) {
        if (posterPath != null) {
            TmdbImage(
                path = posterPath,
                contentDescription = name,
                size = TmdbUtils.ImageSize.W185,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(2), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!watched) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        if (watched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }

        if (watched) {
            IconButton(
                onClick = onToggleLoved,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = if (loved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (loved) Color(0xFFE91E63) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PreferencesStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Cuéntanos cómo\ndisfrutas las series",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        )

        PreferenceQuestion(
            question = "¿Prefieres episodios cortos o largos?",
            emoji = "⏱️",
            options = EpisodeLengthPref.entries,
            selected = state.episodeLengthPref,
            labelOf = { it.label },
            descOf = { it.description },
            onSelect = { viewModel.setEpisodeLengthPref(it) }
        )

        PreferenceQuestion(
            question = "¿Series finalizadas o en emisión?",
            emoji = "📡",
            options = StatusPref.entries,
            selected = state.statusPref,
            labelOf = { it.label },
            descOf = { it.description },
            onSelect = { viewModel.setStatusPref(it) }
        )

        PreferenceQuestion(
            question = "¿Dobladas o en versión original?",
            emoji = "🌍",
            options = DubbedPref.entries,
            selected = state.dubbedPref,
            labelOf = { it.label },
            descOf = { it.description },
            onSelect = { viewModel.setDubbedPref(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun <T> PreferenceQuestion(
    question: String,
    emoji: String,
    options: List<T>,
    selected: T?,
    labelOf: (T) -> String,
    descOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = question,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        }
        options.forEach { option ->
            val isSelected = option == selected
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(220),
                label = "pref_bg"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PrimaryPurple.copy(alpha = bgAlpha * 0.3f),
                                Color(0xFF9C27B0).copy(alpha = bgAlpha * 0.2f)
                            )
                        )
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = labelOf(option),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = descOf(option),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private val analysisPhrases = listOf(
    "Analizando tus géneros favoritos...",
    "Procesando las series que has visto...",
    "Calculando tu perfil de espectador...",
    "¡Casi listo!"
)

@Composable
private fun AnalysisStep(state: OnboardingUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "analysis")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(PrimaryPurple, Color(0xFF9C27B0), Color(0xFF3F51B5), PrimaryPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedContent(
                targetState = state.analyzePhase,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(300))
                },
                label = "analyze_text"
            ) { phase ->
                Text(
                    text = analysisPhrases.getOrElse(phase) { analysisPhrases.last() },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { (state.analyzePhase + 1) / 4f },
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = PrimaryPurple,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun PersonalityStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val personality = state.personality ?: return

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val emojiScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emoji_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400),
        label = "content_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Tu perfil de espectador",
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .scale(emojiScale)
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(PrimaryPurple.copy(alpha = 0.4f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(personality.emoji, fontSize = 72.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.alpha(contentAlpha.coerceIn(0f, 1f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = personality.title,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = personality.tagline,
                color = PrimaryPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = personality.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val watched = state.watchedShowIds.size
            val genres = state.selectedGenres.size
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(emoji = "🎬", label = "$genres géneros")
                if (watched > 0) StatChip(emoji = "👁️", label = "$watched vistas")
                if (state.lovedShowIds.isNotEmpty()) {
                    StatChip(emoji = "❤️", label = "${state.lovedShowIds.size} favoritas")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatChip(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
