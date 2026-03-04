package com.example.showmateapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.SurfaceDark
import com.example.showmateapp.ui.theme.TextGray

@Composable
fun ProfileScreen(
    globalNavController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val userName by viewModel.userEmail.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // User Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(userName.take(1).uppercase(), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (favoritesCount > 5) "Cinéfilo Experto" else "Cinéfilo Principiante", color = PrimaryPurple, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat(value = favoritesCount.toString(), label = "Favoritos")
            Divider(color = TextGray.copy(alpha = 0.2f), modifier = Modifier.height(40.dp).width(1.dp))
            ProfileStat(value = "${favoritesCount * 2}h", label = "Tiempo")
            Divider(color = TextGray.copy(alpha = 0.2f), modifier = Modifier.height(40.dp).width(1.dp))
            ProfileStat(value = "0", label = "Reviews")
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Actions List
        Text(
            text = "General",
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        ProfileActionButton(
            icon = Icons.Default.List,
            title = "Mi Lista de Seguimiento",
            onClick = { /* TODO */ }
        )
        
        ProfileActionButton(
            icon = Icons.Default.Settings,
            title = "Ajustes de la Cuenta",
            onClick = { globalNavController.navigate("settings") }
        )
        
        ProfileActionButton(
            icon = Icons.Default.Info,
            title = "Ayuda y Soporte",
            onClick = { /* TODO */ }
        )
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileActionButton(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Go", tint = TextGray)
    }
}
