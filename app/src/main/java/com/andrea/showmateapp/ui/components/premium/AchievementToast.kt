package com.andrea.showmateapp.ui.components.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleDark
import com.andrea.showmateapp.ui.theme.StarYellow
import kotlinx.coroutines.delay

@Composable
fun AchievementToastOverlay(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(achievement) {
        if (achievement != null) {
            visible = true
            delay(3500)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && achievement != null,
        enter = slideInVertically { -it - 40 } + fadeIn(tween(280)),
        exit  = slideOutVertically { -it - 40 } + fadeOut(tween(280)),
        modifier = modifier
            .zIndex(20f)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        achievement?.let { AchievementToastCard(it) }
    }
}

@Composable
private fun AchievementToastCard(achievement: Achievement) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.72f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "toastScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, PrimaryPurple.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PrimaryPurpleDark.copy(alpha = 0.97f),
                        Color(0xFF130D30).copy(alpha = 0.97f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryPurple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(achievement.emoji, fontSize = 26.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¡Logro desbloqueado!",
                    color = PrimaryPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    achievement.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    achievement.description,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "+${achievement.xpReward}",
                    color = StarYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    "XP",
                    color = StarYellow.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
