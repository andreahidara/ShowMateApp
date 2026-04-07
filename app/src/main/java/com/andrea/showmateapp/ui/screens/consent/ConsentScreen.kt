package com.andrea.showmateapp.ui.screens.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrea.showmateapp.ui.components.premium.AuthBackground
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.PrimaryPurpleDark
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.TextGray

@Composable
fun ConsentScreen(
    onAccepted: () -> Unit,
    viewModel: ConsentViewModel = hiltViewModel()
) {
    val isConsentGiven by viewModel.isConsentGiven.collectAsStateWithLifecycle()

    LaunchedEffect(isConsentGiven) {
        if (isConsentGiven) onAccepted()
    }

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
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(PrimaryPurple.copy(alpha = 0.3f), PrimaryPurpleDark.copy(alpha = 0.2f)))
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ShowMate",
                color = PrimaryPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Antes de continuar",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Queremos que sepas cómo usamos tus datos",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ConsentDataRow(
                        icon = Icons.Default.Person,
                        title = "Datos de cuenta",
                        description = "Tu email y nombre de usuario se almacenan en Firebase para identificarte."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    ConsentDataRow(
                        icon = Icons.Default.Lock,
                        title = "Historial de uso",
                        description = "Tus interacciones (series vistas, favoritos, valoraciones) se usan para personalizar tus recomendaciones."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    ConsentDataRow(
                        icon = Icons.Default.Gavel,
                        title = "Cumplimiento RGPD",
                        description = "Tus datos se almacenan de forma segura conforme al Reglamento General de Protección de Datos (UE) 2016/679. Puedes solicitar su eliminación en cualquier momento desde Ajustes."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryPurple.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "ShowMate no vende ni comparte tus datos con terceros. Toda la información se usa exclusivamente para mejorar tu experiencia en la app.",
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.giveConsent()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(
                    text = "Acepto y continúo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No acepto (salir de la app)",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsentDataRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryPurple.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryPurpleLight, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
