package com.example.pam_1.ui.screens.features.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pam_1.data.model.Note
import com.example.pam_1.ui.theme.BackgroundBeige
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.White
import com.example.pam_1.viewmodel.NoteViewModel
import com.example.pam_1.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NoteViewModel,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val noteState by viewModel.noteListState.collectAsState()

    // State untuk menyimpan Note mana yang sedang ditekan lama
    // Jika null, berarti menu sedang tertutup
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    // State untuk kontrol BottomSheet
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BackgroundBeige,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = PrimaryBrown,
                contentColor = White
            ) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            Text(
                text = "Catatan",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )

            Divider(thickness = 1.dp, color = Color(0xFFDDDDDD))

            when (noteState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Success -> {
                    val notes = (noteState as UiState.Success<List<Note>>).data

                    if (notes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada catatan")
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalItemSpacing = 12.dp,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { note.id?.let { onNoteClick(it) } },
                                    onLongClick = {
                                        // Saat ditahan lama, simpan note ke state agar menu muncul
                                        selectedNote = note
                                    }
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (noteState as UiState.Error).message,
                            color = Color.Red
                        )
                    }
                }
                else -> {}
            }
        }

        // ===== LOGIKA BOTTOM SHEET (MENU BAWAH) =====
        if (selectedNote != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedNote = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp) // Padding bawah agar tidak mepet navigasi HP
                ) {
                    val note = selectedNote!!

                    // --- OPSI 1: SEMATKAN / LEPAS SEMATAN ---
                    ListItem(
                        headlineContent = {
                            Text(if (note.isPinned) "Lepas Sematan" else "Sematkan")
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color(0xFFA56A3A)
                            )
                        },
                        modifier = Modifier.clickable {
                            // Aksi Pin
                            note.id?.let { id ->
                                viewModel.updatePinStatus(id, !note.isPinned)
                            }
                            // Tutup Menu
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                selectedNote = null
                            }
                        }
                    )

                    // --- OPSI 2: HAPUS (MERAH) ---
                    ListItem(
                        headlineContent = {
                            Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        },
                        modifier = Modifier.clickable {
                            // Aksi Hapus
                            note.id?.let { id ->
                                viewModel.deleteNote(id)
                            }
                            // Tutup Menu
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                selectedNote = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit // Callback baru untuk Long Press
) {
    val rawDate = note.updatedAt ?: note.createdAt
    val displayDate = rawDate?.substringBefore("T") ?: ""

    Surface(
        // Hapus onClick dari Surface, kita pindahkan ke Modifier.combinedClickable
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // Clip agar ripple effect rapi
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick // Ini yang mendeteksi tahan lama
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // Ikon Pin HANYA sebagai indikator visual (tidak bisa diklik di sini)
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color(0xFFA56A3A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}