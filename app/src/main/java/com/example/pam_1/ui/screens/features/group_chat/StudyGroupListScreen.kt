package com.example.pam_1.ui.screens.features.group_chat

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.data.model.StudyGroup
import com.example.pam_1.viewmodel.StudyGroupUIState
import com.example.pam_1.viewmodel.StudyGroupViewModel

@Composable
fun StudyGroupListScreen(navController: NavController, viewModel: StudyGroupViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadMyGroups()
        viewModel.loadPublicGroups()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is StudyGroupUIState.Success -> {
                if (uiState.message.isNotEmpty()) {
                    Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                }
                viewModel.resetState()
            }
            is StudyGroupUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 10.dp),) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Grup Saya") }
                )
                Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Grup Publik") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Content based on selected tab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 ->
                            GroupList(
                                    groups = viewModel.myGroups,
                                    isLoading = uiState is StudyGroupUIState.Loading,
                                    onGroupClick = { group ->
                                        navController.navigate("group_detail/${group.id}")
                                    },
                                    emptyMessage = "Anda belum bergabung dengan grup apapun"
                            )
                    1 ->
                            GroupList(
                                    groups = viewModel.publicGroups,
                                    isLoading = uiState is StudyGroupUIState.Loading,
                                    onGroupClick = { group ->
                                        navController.navigate("group_detail/${group.id}")
                                    },
                                    emptyMessage = "Tidak ada grup publik tersedia"
                            )
                }
            }

            // Bottom buttons
            Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                        onClick = { navController.navigate("create_group") },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Grup Baru")
                }

                OutlinedButton(
                        onClick = { navController.navigate("join_group") },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Gabung Grup dengan Kode") }
            }
        }
    }
}

@Composable
fun GroupList(
        groups: List<StudyGroup>,
        isLoading: Boolean,
        onGroupClick: (StudyGroup) -> Unit,
        emptyMessage: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator()
            groups.isEmpty() -> {
                Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { group ->
                        GroupCard(group = group, onClick = { onGroupClick(group) })
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: StudyGroup, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Icon(
                            imageVector =
                                    if (group.isPublic) Icons.Default.Public
                                    else Icons.Default.Lock,
                            contentDescription = if (group.isPublic) "Publik" else "Privat",
                            modifier = Modifier.size(16.dp),
                            tint =
                                    if (group.isPublic) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                    )
                }

                group.course?.let { course ->
                    Text(
                            text = course,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                }

                group.description?.let { desc ->
                    Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                    )
                }
            }
        }
    }
}
