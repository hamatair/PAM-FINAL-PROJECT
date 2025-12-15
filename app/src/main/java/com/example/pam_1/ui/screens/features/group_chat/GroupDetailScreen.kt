package com.example.pam_1.ui.screens.features.group_chat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.data.model.GroupMember
import com.example.pam_1.data.model.GroupRole
import com.example.pam_1.viewmodel.StudyGroupUIState
import com.example.pam_1.viewmodel.StudyGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(navController: NavController, viewModel: StudyGroupViewModel, groupId: Long) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val group = viewModel.selectedGroup
    val userRole = viewModel.currentUserRole
    val memberCount = viewModel.memberCount
    val members = viewModel.groupMembers

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(groupId) { viewModel.loadGroupById(groupId) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is StudyGroupUIState.Success -> {
                if (uiState.message.contains("deleted") || uiState.message.contains("Left")) {
                    Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
            }
            is StudyGroupUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(group?.name ?: "Detail Grup") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Kembali")
                            }
                        },
                        actions = {
                            if (userRole == GroupRole.OWNER) {
                                IconButton(
                                        onClick = { navController.navigate("edit_group/$groupId") }
                                ) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, "Hapus")
                                }
                            }
                        }
                )
            }
    ) { paddingValues ->
        if (group == null) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Group Header
                Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                            )
                            Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                            )
                            Icon(
                                    imageVector =
                                            if (group.isPublic) Icons.Default.Public
                                            else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                            )
                        }

                        group.course?.let { course ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                    text = "Mata Kuliah: $course",
                                    style = MaterialTheme.typography.titleMedium
                            )
                        }

                        group.description?.let { desc ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                                text =
                                        "$memberCount ${if (memberCount == 1) "anggota" else "anggota"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        userRole?.let { role ->
                            Text(
                                    text = "Peran Anda: ${role.value}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userRole == GroupRole.OWNER || userRole == GroupRole.MODERATOR) {
                        Button(
                                onClick = { navController.navigate("manage_invites/$groupId") },
                                modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Undangan")
                        }
                    }

                    if (userRole != null && userRole != GroupRole.OWNER) {
                        OutlinedButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.weight(1f),
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                        )
                        ) {
                            Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Keluar")
                        }
                    }

                    if (userRole == null && group.isPublic) {
                        Button(
                                onClick = { viewModel.joinGroup(groupId) },
                                modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gabung Grup")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Anggota") }
                    )
                    Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Obrolan") }
                    )
                }

                // Content
                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(members) { member ->
                                MemberCard(
                                        member = member,
                                        canManage =
                                                userRole == GroupRole.OWNER ||
                                                        userRole == GroupRole.MODERATOR,
                                        isOwner = userRole == GroupRole.OWNER,
                                        onChangeRole = { newRole ->
                                            viewModel.updateMemberRole(
                                                    groupId,
                                                    member.userId,
                                                    newRole
                                            )
                                        },
                                        onRemove = {
                                            viewModel.removeMember(groupId, member.userId)
                                        }
                                )
                            }
                        }
                    }
                    1 -> {
                        // Navigate to chat screen
                        LaunchedEffect(Unit) {
                            navController.navigate("group_chat/$groupId")
                            selectedTab = 0 // Reset tab when returning
                        }
                    }
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus Grup") },
                text = {
                    Text(
                            "Apakah Anda yakin ingin menghapus grup ini? Tindakan ini tidak dapat dibatalkan."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deleteGroup(groupId)
                                showDeleteDialog = false
                            },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                    )
                    ) { Text("Hapus") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
                }
        )
    }

    // Leave Dialog
    if (showLeaveDialog) {
        AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Keluar dari Grup") },
                text = { Text("Apakah Anda yakin ingin keluar dari grup ini?") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.leaveGroup(groupId)
                                showLeaveDialog = false
                            }
                    ) { Text("Keluar") }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) { Text("Batal") }
                }
        )
    }
}

@Composable
fun MemberCard(
        member: GroupMember,
        canManage: Boolean,
        isOwner: Boolean,
        onChangeRole: (GroupRole) -> Unit,
        onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val role = GroupRole.fromString(member.role)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
            ) {
                Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                )
                Column {
                    // Display full name or username
                    Text(
                            text = member.getDisplayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                    )
                    // Show username if available and different from display name
                    member.username?.let { username ->
                        if (username != member.fullName) {
                            Text(
                                    text = "@$username",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                            )
                            )
                        }
                    }
                    AssistChip(
                            onClick = {},
                            label = { Text(role.value) },
                            colors =
                                    AssistChipDefaults.assistChipColors(
                                            containerColor =
                                                    when (role) {
                                                        GroupRole.OWNER ->
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        GroupRole.MODERATOR ->
                                                                MaterialTheme.colorScheme
                                                                        .secondaryContainer
                                                        GroupRole.MEMBER ->
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                    }
                                    ),
                            modifier = Modifier.height(24.dp)
                    )
                }
            }

            if (canManage && role != GroupRole.OWNER) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (isOwner && role != GroupRole.MODERATOR) {
                            DropdownMenuItem(
                                    text = { Text("Jadikan Moderator") },
                                    onClick = {
                                        onChangeRole(GroupRole.MODERATOR)
                                        showMenu = false
                                    }
                            )
                        }
                        if (isOwner && role != GroupRole.MEMBER) {
                            DropdownMenuItem(
                                    text = { Text("Jadikan Anggota") },
                                    onClick = {
                                        onChangeRole(GroupRole.MEMBER)
                                        showMenu = false
                                    }
                            )
                        }
                        DropdownMenuItem(
                                text = { Text("Hapus") },
                                onClick = {
                                    onRemove()
                                    showMenu = false
                                }
                        )
                    }
                }
            }
        }
    }
}
