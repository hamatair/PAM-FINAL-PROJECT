package com.example.pam_1.ui.screens.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadNoteScreen(
    note: Note,
    onEditNote: (Long) -> Unit,
    onBack: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var isPinned by remember { mutableStateOf(note.isPinned) }
    val displayDate = (note.updatedAt ?: note.createdAt)?.substringBefore("T") ?: ""

    // STATE UNTUK DIALOG KONFIRMASI HAPUS
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFA56A3A)
                        )
                    }
                },
                actions = {
                    // ===== DELETE ICON: Tampilkan dialog saat diklik =====
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }

                    // ===== PIN ICON =====
                    IconButton(onClick = {
                        isPinned = !isPinned
                        onPinToggle(isPinned)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) Color(0xFFA56A3A) else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { note.id?.let { onEditNote(it) } },
                containerColor = Color(0xFFA56A3A),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ... (Kode untuk Image dan Content di sini sama) ...

            // IMAGE
            note.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Note Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // CONTENT
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // TANGGAL
                if (displayDate.isNotEmpty()) {
                    Text(
                        text = displayDate,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // JUDUL
                Text(
                    text = note.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ISI
                Text(
                    text = note.description,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // ===== KOTAK DIALOG KONFIRMASI HAPUS =====
    if (showDeleteDialog && note.id != null) {
        AlertDialog(
            onDismissRequest = {
                // Tutup dialog jika klik di luar
                showDeleteDialog = false
            },
            title = {
                Text("Konfirmasi Hapus")
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus catatan ini secara permanen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        note.id?.let { onDelete(it) }
                        showDeleteDialog = false // Tutup dialog
                        // Navigasi kembali (popBackStack) akan dipicu dari AppNavigation
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}