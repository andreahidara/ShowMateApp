package com.example.showmateapp.ui.screens.profile.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.showmateapp.ui.components.premium.CardSurface
import com.example.showmateapp.ui.components.premium.outlinedTextFieldColors
import com.example.showmateapp.ui.theme.PrimaryPurple
import com.example.showmateapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListsScreen(
    navController: NavController,
    viewModel: CustomListsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideCreateDialog,
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text("Nueva lista", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = uiState.newListName,
                    onValueChange = viewModel::onNewListNameChange,
                    label = { Text("Nombre de la lista", color = TextGray) },
                    singleLine = true,
                    colors = outlinedTextFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::createList,
                    enabled = uiState.newListName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideCreateDialog) {
                    Text("Cancelar", color = TextGray)
                }
            }
        )
    }

    var listToDelete by remember { mutableStateOf<String?>(null) }
    if (listToDelete != null) {
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Eliminar lista", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar \"$listToDelete\"? Esta acción no se puede deshacer.", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        listToDelete?.let { viewModel.deleteList(it) }
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancelar", color = TextGray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis listas", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showCreateDialog) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva lista", tint = PrimaryPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = PrimaryPurple
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva lista", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (uiState.lists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = PrimaryPurple.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        "Aún no tienes listas",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crea tu primera lista para organizar tus series",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = viewModel::showCreateDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear lista", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.lists.entries.toList(), key = { it.key }) { (name, ids) ->
                    CardSurface(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryPurple.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ids.size} ${if (ids.size == 1) "serie" else "series"}",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { listToDelete = name }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar lista",
                                    tint = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
