package com.andrea.showmateapp.ui.screens.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrea.showmateapp.data.model.Review
import com.andrea.showmateapp.ui.components.premium.shimmerBrush
import com.andrea.showmateapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SocialReviewsSection(
    showId: Int,
    numberOfSeasons: Int,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(showId) { viewModel.loadReviews(showId, numberOfSeasons) }

    if (state.showWriteSheet) {
        WriteReviewSheet(
            state = state,
            seasons = state.availableSeasons,
            onTextChange = viewModel::onWriteTextChange,
            onRatingChange = viewModel::onWriteRatingChange,
            onSpoilerToggle = viewModel::onSpoilerToggle,
            onSubmit = viewModel::submitReview,
            onDismiss = viewModel::closeWriteSheet
        )
    }

    Column(modifier = modifier) {
        if (state.availableSeasons.size > 1) {
            SeasonFilterRow(
                seasons = state.availableSeasons,
                selected = state.selectedSeason,
                onSelect = viewModel::selectSeason
            )
            Spacer(Modifier.height(20.dp))
        }

        MyReviewCard(
            review = state.myReview,
            writeRating = state.writeRating,
            onOpenSheet = viewModel::openWriteSheet,
            onDelete = viewModel::deleteMyReview
        )

        AnimatedVisibility(
            visible = state.friendReviews.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(28.dp))
                SectionLabel(text = "¿Qué dicen tus amigos?", emoji = "👥")
                Spacer(Modifier.height(12.dp))
                state.friendReviews.forEach { review ->
                    ReviewCard(
                        review = review,
                        myUserId = state.myUserId,
                        isReported = review.id in state.reportedReviewIds,
                        isFriend = true,
                        onLike = { viewModel.toggleLike(review.id) },
                        onReport = { viewModel.reportReview(review.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel(text = "Comunidad", emoji = "🌍")
        Spacer(Modifier.height(12.dp))

        if (state.isLoadingPublic) {
            repeat(3) {
                PublicReviewSkeleton()
                Spacer(Modifier.height(10.dp))
            }
        } else if (state.publicReviews.isEmpty()) {
            EmptyReviews()
        } else {
            state.publicReviews.forEach { review ->
                ReviewCard(
                    review = review,
                    myUserId = state.myUserId,
                    isReported = review.id in state.reportedReviewIds,
                    isFriend = false,
                    onLike = { viewModel.toggleLike(review.id) },
                    onReport = { viewModel.reportReview(review.id) }
                )
                Spacer(Modifier.height(10.dp))
            }

            if (state.hasMorePublic) {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = PrimaryPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        OutlinedButton(
                            onClick = viewModel::loadMorePublic,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver más reseñas", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            LaunchedEffect(state.errorMessage) {
                viewModel.dismissError()
            }
        }
    }
}

@Composable
private fun SeasonFilterRow(seasons: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        itemsIndexed(seasons) { _, season ->
            val isSelected = season == selected
            val bgColor by animateColorAsState(
                if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.06f),
                tween(200),
                label = "seaBg"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(season) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (season == 0) "Toda la serie" else "Temporada $season",
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MyReviewCard(review: Review?, writeRating: Int, onOpenSheet: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, PrimaryPurple.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tu reseña",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (review != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar reseña",
                        tint = ErrorRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (review != null) {
            Spacer(Modifier.height(6.dp))
            StarDisplay(rating = review.rating, size = 16.dp)
            if (review.text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.text,
                    color = TextGray,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onOpenSheet,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Editar", fontSize = 13.sp)
            }
        } else {
            Spacer(Modifier.height(8.dp))
            if (writeRating > 0) {
                StarDisplay(rating = writeRating, size = 16.dp)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onOpenSheet,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Icon(Icons.Default.Create, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Escribir reseña", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    myUserId: String?,
    isReported: Boolean,
    isFriend: Boolean,
    onLike: () -> Unit,
    onReport: () -> Unit
) {
    val isLiked = myUserId != null && myUserId in review.likedByIds
    var spoilerRevealed by remember(review.id) { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Reportar reseña", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Quieres reportar esta reseña por contenido inapropiado?",
                    color = TextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onReport()
                    showReportDialog = false
                }) { Text("Reportar", color = ErrorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancelar", color = TextGray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFriend) PrimaryPurple.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.03f)
            )
            .border(
                1.dp,
                if (isFriend) PrimaryPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(
                initial = review.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                isFriend = isFriend
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        review.username,
                        color = if (isFriend) PrimaryPurpleLight else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFriend) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("amigo", color = PrimaryPurpleLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (review.seasonNumber > 0) {
                        Text(
                            "T${review.seasonNumber}",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    formatDate(review.createdAt),
                    color = TextGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            if (review.rating > 0) {
                StarDisplay(rating = review.rating, size = 12.dp)
            }
        }

        if (review.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            if (review.hasSpoiler && !spoilerRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .clickable { spoilerRevealed = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = TextGray, modifier = Modifier.size(14.dp))
                        Text("Contiene spoilers — toca para revelar", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                AnimatedContent(
                    targetState = spoilerRevealed || !review.hasSpoiler,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                    label = "spoiler"
                ) {
                    Text(
                        text = review.text,
                        color = if (review.hasSpoiler) Color.White.copy(alpha = 0.85f) else TextGray,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLiked) HeartRed.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                    .clickable(enabled = myUserId != null && review.userId != myUserId) { onLike() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) HeartRed else TextGray,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (review.likeCount > 0) "${review.likeCount}" else "Me gusta",
                    color = if (isLiked) HeartRed else TextGray,
                    fontSize = 12.sp,
                    fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (review.userId != myUserId) {
                IconButton(
                    onClick = { if (!isReported) showReportDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isReported) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                        contentDescription = "Reportar",
                        tint = if (isReported) ErrorRed.copy(alpha = 0.5f) else TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteReviewSheet(
    state: ReviewUiState,
    seasons: List<Int>,
    onTextChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onSpoilerToggle: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (state.myReview != null) "Editar reseña" else "Escribir reseña",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            if (seasons.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sobre", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    SeasonFilterRow(
                        seasons = seasons,
                        selected = state.selectedSeason,
                        onSelect = { }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Puntuación", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                InteractiveStarRating(
                    current = state.writeRating,
                    onSelect = onRatingChange
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tu reseña", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.writeText,
                    onValueChange = onTextChange,
                    placeholder = { Text("¿Qué te ha parecido?", color = TextGray.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PrimaryPurple
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${state.writeText.length}/500",
                        color = TextGray.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    if (state.writeText.length > 500) {
                        Text("Demasiado largo", color = ErrorRed, fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { onSpoilerToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (state.writeSpoiler) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (state.writeSpoiler) StarYellow else TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Contiene spoilers",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Otros verán una advertencia antes de leer",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = state.writeSpoiler,
                    onCheckedChange = { onSpoilerToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StarYellow,
                        checkedTrackColor = StarYellow.copy(alpha = 0.35f)
                    )
                )
            }

            AnimatedVisibility(visible = state.offensiveWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Text(
                        "Tu reseña contiene palabras inapropiadas. Por favor, modifícala antes de publicar.",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            val canSubmit = (state.writeText.isNotBlank() || state.writeRating > 0) &&
                state.writeText.length <= 500 && !state.isSaving

            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publicar reseña", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun StarDisplay(rating: Int, size: androidx.compose.ui.unit.Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            Icon(
                if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = StarYellow,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
private fun InteractiveStarRating(current: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { i ->
            val star = i + 1
            Icon(
                if (star <= current) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star estrellas",
                tint = if (star <= current) StarYellow else TextGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onSelect(star) }
            )
        }
        if (current > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                listOf("", "Muy malo", "Malo", "Regular", "Bueno", "¡Excelente!")[current],
                color = StarYellow,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun AvatarCircle(initial: String, isFriend: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                if (isFriend) PrimaryPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                CircleShape
            )
            .border(
                1.dp,
                if (isFriend) PrimaryPurple.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = if (isFriend) PrimaryPurpleLight else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String, emoji: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EmptyReviews() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✍️", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text("Sé el primero en reseñar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("No hay reseñas para este filtro todavía.", color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PublicReviewSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(brush))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.width(90.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Box(modifier = Modifier.width(60.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp)).background(brush))
    }
}

private fun formatDate(epochMillis: Long): String = if (epochMillis == 0L) {
    ""
} else {
    SimpleDateFormat("d MMM yyyy", Locale("es", "ES")).format(Date(epochMillis))
}
