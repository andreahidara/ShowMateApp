package com.example.showmateapp.ui.screens.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.showmateapp.R
import com.example.showmateapp.ui.components.premium.AuthBackground
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.PrimaryPurpleDark
import com.example.showmateapp.ui.theme.TextGray
import kotlinx.coroutines.launch

private data class WelcomePage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

private val pages = listOf(
    WelcomePage(
        icon = Icons.Default.Stars,
        title = "Descubre series que amarás",
        subtitle = "Un algoritmo personal aprende de tus gustos con cada interacción y te recomienda exactamente lo que necesitas ver.",
        accentColor = Color(0xFF7C4DFF)
    ),
    WelcomePage(
        icon = Icons.Default.Psychology,
        title = "Conoce tu perfil de espectador",
        subtitle = "Identifica tus géneros, estilos narrativos y temas favoritos. Descubre qué tipo de espectador eres.",
        accentColor = Color(0xFF9C27B0)
    ),
    WelcomePage(
        icon = Icons.Default.Group,
        title = "Conecta con amigos",
        subtitle = "Compara favoritos, encuentra series en común y organiza maratones de grupo con un solo toque.",
        accentColor = Color(0xFF6200EA)
    )
)

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "welcomeOrbs")
    val orb1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.50f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val orb2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.38f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing, delayMillis = 1000), RepeatMode.Reverse),
        label = "orb2"
    )

    AuthBackground {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .alpha(orb1Alpha)
                .blur(90.dp)
                .background(PrimaryPurple.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .alpha(orb2Alpha)
                .blur(90.dp)
                .background(PrimaryPurpleDark.copy(alpha = 0.6f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.logosm),
                contentDescription = "ShowMate",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ShowMate",
                color = PrimaryPurple,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                WelcomePageContent(page = pages[index])
            }

            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                                else
                                    Brush.horizontalGradient(
                                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.2f))
                                    )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Siguiente", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Omitir", color = TextGray, fontSize = 14.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable { onGetStarted() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Empezar", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            page.accentColor.copy(alpha = 0.25f),
                            page.accentColor.copy(alpha = 0.04f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            color = TextGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}
