package com.example.pam_1.ui.screens.features.group_chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.pam_1.data.model.GroupInvite
import com.example.pam_1.viewmodel.StudyGroupUIState
import com.example.pam_1.viewmodel.StudyGroupViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteManagementScreen(
        navController: NavController,
        viewModel: StudyGroupViewModel,
        groupId: Long
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val invites = viewModel.groupInvites
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) { viewModel.loadGroupInvites(groupId) }

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

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Kelola Undangan") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Kembali")
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Buat Undangan")
                }
            }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (invites.isEmpty()) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                            "Belum ada undangan",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                            "Buat undangan untuk dibagikan kepada orang lain",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(invites) { invite ->
                        InviteCard(
                                invite = invite,
                                onDeactivate = { viewModel.deactivateInvite(invite.id!!, groupId) }
                        )
                    }
                }
            }
        }
    }

    // Create Invite Dialog
    if (showCreateDialog) {
        CreateInviteDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { maxUses, expiresInDays ->
                    viewModel.createInvite(groupId, maxUses, expiresInDays)
                    showCreateDialog = false
                }
        )
    }
}

@Composable
fun InviteCard(invite: GroupInvite, onDeactivate: () -> Unit) {
    var showDeactivateDialog by remember { mutableStateOf(false) }
    val isExpired = invite.expiresAt?.let { Instant.parse(it).isBefore(Instant.now()) } ?: false

    val isUsedUp = invite.maxUses > 0 && invite.usedCount >= invite.maxUses
    val isInactive = !invite.isActive || isExpired || isUsedUp

    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isInactive) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                    )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = invite.code,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color =
                                if (isInactive) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                )

                if (isInactive) {
                    AssistChip(
                            onClick = {},
                            label = { Text("Tidak Aktif") },
                            colors =
                                    AssistChipDefaults.assistChipColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.errorContainer,
                                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                    )
                } else {
                    AssistChip(
                            onClick = {},
                            label = { Text("Aktif") },
                            colors =
                                    AssistChipDefaults.assistChipColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.primaryContainer
                                    )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                            "Penggunaan: ${invite.usedCount}/${if (invite.maxUses == 0) "∞" else invite.maxUses}",
                            style = MaterialTheme.typography.bodyMedium
                    )

                    invite.expiresAt?.let { expiresAt ->
                        val formatter =
                                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                                        .withZone(ZoneId.systemDefault())
                        val dateStr = formatter.format(Instant.parse(expiresAt))
                        Text(
                                "Kedaluwarsa: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        if (isExpired) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                        )
                    }
                }
            }

            if (!isInactive) {
                Spacer(Modifier.height(8.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Code Button
                    val context = LocalContext.current
                    OutlinedButton(
                            onClick = {
                                val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as
                                                ClipboardManager
                                val clip = ClipData.newPlainText("Invite Code", invite.code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salin")
                    }

                    OutlinedButton(
                            onClick = {
                                // TODO: Show QR code
                            },
                            modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("QR")
                    }

                    Button(
                            onClick = { showDeactivateDialog = true },
                            modifier = Modifier.weight(1f),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.errorContainer,
                                            contentColor =
                                                    MaterialTheme.colorScheme.onErrorContainer
                                    )
                    ) {
                        Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        // Text("Nonaktifkan")
                    }
                }
            }
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
                onDismissRequest = { showDeactivateDialog = false },
                title = { Text("Nonaktifkan Undangan") },
                text = { Text("Apakah Anda yakin ingin menonaktifkan kode undangan ini?") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                onDeactivate()
                                showDeactivateDialog = false
                            }
                    ) { Text("Nonaktifkan") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateDialog = false }) { Text("Batal") }
                }
        )
    }
}

@Composable
fun CreateInviteDialog(
        onDismiss: () -> Unit,
        onCreate: (maxUses: Int, expiresInDays: Int?) -> Unit
) {
    var selectedMaxUses by remember { mutableStateOf(1) }
    var selectedExpiry by remember { mutableStateOf(7) }
    var noExpiry by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Buat Kode Undangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Max Uses
                    Column {
                        Text("Penggunaan Maksimal", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                    selected = selectedMaxUses == 1,
                                    onClick = { selectedMaxUses = 1 },
                                    label = { Text("1 (Sekali pakai)") }
                            )
                            FilterChip(
                                    selected = selectedMaxUses == 5,
                                    onClick = { selectedMaxUses = 5 },
                                    label = { Text("5") }
                            )
                            FilterChip(
                                    selected = selectedMaxUses == 0,
                                    onClick = { selectedMaxUses = 0 },
                                    label = { Text("Tanpa Batas") }
                            )
                        }
                    }

                    // Expiry
                    Column {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kedaluwarsa", style = MaterialTheme.typography.titleSmall)
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                        "Tidak kedaluwarsa",
                                        style = MaterialTheme.typography.bodySmall
                                )
                                Switch(checked = noExpiry, onCheckedChange = { noExpiry = it })
                            }
                        }

                        if (!noExpiry) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                        selected = selectedExpiry == 1,
                                        onClick = { selectedExpiry = 1 },
                                        label = { Text("1 hari") }
                                )
                                FilterChip(
                                        selected = selectedExpiry == 7,
                                        onClick = { selectedExpiry = 7 },
                                        label = { Text("7 hari") }
                                )
                                FilterChip(
                                        selected = selectedExpiry == 30,
                                        onClick = { selectedExpiry = 30 },
                                        label = { Text("30 hari") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            onCreate(selectedMaxUses, if (noExpiry) null else selectedExpiry)
                        }
                ) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
