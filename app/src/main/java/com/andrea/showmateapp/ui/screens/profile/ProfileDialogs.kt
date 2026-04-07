package com.andrea.showmateapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.showmateapp.R
import com.andrea.showmateapp.ui.theme.PrimaryPurple
import com.andrea.showmateapp.ui.theme.TextGray

@Composable
fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                stringResource(R.string.profile_logout),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.profile_logout_confirm),
                color = TextGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text(stringResource(R.string.profile_logout_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        }
    )
}

@Composable
fun AvatarColorPickerDialog(
    palette: List<Int>,
    selectedColorInt: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                stringResource(R.string.profile_avatar_color_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.profile_avatar_color_subtitle),
                    color = TextGray,
                    fontSize = 14.sp
                )
                val rows = palette.chunked(4)
                rows.forEach { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowColors.forEach { colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .clickable { onColorSelected(colorInt) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorInt == colorInt) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextGray)
            }
        }
    )
}

@Composable
fun ResetAlgorithmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                stringResource(R.string.profile_reset_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.profile_reset_confirm),
                color = TextGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(stringResource(R.string.profile_reset_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        }
    )
}
