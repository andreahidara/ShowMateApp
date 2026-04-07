package com.andrea.showmateapp.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.components.premium.ShowMateSpacing
import com.andrea.showmateapp.ui.theme.PrimaryMagenta
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight

@Composable
fun greeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    return when {
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 20 -> stringResource(R.string.home_greeting_afternoon)
        else      -> stringResource(R.string.home_greeting_night)
    }
}

@Composable
fun HomeTopAppBar(
    userName: String = "",
    onPickWhatToWatch: () -> Unit = {},
    onAvatarClick: () -> Unit = {}
) {
    val gradientColors = listOf(PrimaryPurple, PrimaryMagenta)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ShowMateSpacing.m, vertical = ShowMateSpacing.xs)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ShowMateSpacing.s)
        ) {
            if (userName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Brush.linearGradient(gradientColors), CircleShape)
                        .clickable(
                            onClickLabel = "Ir al perfil",
                            role = Role.Button,
                            onClick = onAvatarClick
                        )
                        .semantics { contentDescription = "Avatar de $userName" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.first().uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Column {
                Text(
                    text = "ShowMate",
                    style = TextStyle(brush = Brush.linearGradient(colors = gradientColors)),
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
                        .background(Brush.linearGradient(colors = gradientColors))
                )
                if (userName.isNotBlank()) {
                    Text(
                        text = "${greeting()}, $userName",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryPurple.copy(alpha = 0.14f))
                .clickable(
                    onClickLabel = stringResource(R.string.home_what_to_watch),
                    role = Role.Button,
                    onClick = onPickWhatToWatch
                )
                .padding(horizontal = ShowMateSpacing.s, vertical = ShowMateSpacing.s),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ShowMateSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = stringResource(R.string.home_what_to_watch_short),
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.home_what_to_watch_short),
                    color = PrimaryPurpleLight,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
