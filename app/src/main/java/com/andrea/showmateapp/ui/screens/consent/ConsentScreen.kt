package com.andrea.showmateapp.ui.screens.consent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrea.showmateapp.ui.components.AuthBackground
import com.andrea.showmateapp.ui.theme.*
import com.andrea.showmateapp.ui.theme.ShowMateAppTheme

@Composable
fun ConsentScreen(onAccepted: () -> Unit, viewModel: ConsentViewModel = hiltViewModel()) {

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            HeroShieldIcon()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tu privacidad, primero",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Queremos que sepas exactamente\ncómo usamos tus datos",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ConsentCard()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No vendemos tus datos a terceros · Solo para mejorar tu experiencia",
                    color = TextGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    viewModel.giveConsent()
                    onAccepted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Acepto y continúo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No acepto — salir de la app",
                    color = TextGray.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroShieldIcon() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = glowAlpha))
                .blur(20.dp)
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryPurple.copy(alpha = 0.4f), PrimaryPurpleDark.copy(alpha = 0.25f))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(PrimaryPurpleLight.copy(alpha = 0.5f), Color.Transparent)),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun ConsentCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            ConsentDataRow(
                icon = Icons.Default.Person,
                iconColor = AccentBlue,
                title = "Datos de cuenta",
                description = "Tu email se usa para identificarte y proteger tu cuenta."
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(16.dp))
            ConsentDataRow(
                icon = Icons.Default.Lock,
                iconColor = SuccessGreen,
                title = "Historial de uso",
                description = "Tus valoraciones personalizan las recomendaciones que ves."
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(16.dp))
            ConsentDataRow(
                icon = Icons.Default.Gavel,
                iconColor = StarYellow,
                title = "Cumplimiento RGPD",
                description = "Tus datos están protegidos según la normativa europea."
            )
        }
    }
}

@Composable
private fun ConsentDataRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = iconColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Preview(name = "Consent — Icono hero", showBackground = true, backgroundColor = 0xFF151522, widthDp = 200, heightDp = 160)
@Composable
private fun HeroShieldIconPreview() {
    ShowMateAppTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HeroShieldIcon()
        }
    }
}

@Preview(name = "Consent — Card privacidad", showBackground = true, backgroundColor = 0xFF151522, widthDp = 360)
@Composable
private fun ConsentCardPreview() {
    ShowMateAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConsentCard()
        }
    }
}

@Preview(name = "Consent — Fila de dato", showBackground = true, backgroundColor = 0xFF151522, widthDp = 360)
@Composable
private fun ConsentDataRowPreview() {
    ShowMateAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConsentDataRow(
                icon = Icons.Default.Lock,
                iconColor = SuccessGreen,
                title = "Historial de uso",
                description = "Tus valoraciones personalizan las recomendaciones que ves."
            )
        }
    }
}
