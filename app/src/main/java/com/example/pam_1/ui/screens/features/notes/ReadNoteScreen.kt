package com.example.pam_1.ui.screens.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox // Import Baru
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // Import Baru
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
 // Jika ViewModel menggunakan UiState


// ... (imports lainnya) ...
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
// ... (imports lainnya) ...

import com.example.pam_1.data.model.Note
// Import ViewModel yang diasumsikan
import com.example.pam_1.viewmodel.NoteViewModel
import com.example.pam_1.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadNoteScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    onEditNote: (Long) -> Unit,
    onBack: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    val noteDetailState by viewModel.noteDetailState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        viewModel.loadNoteDetail(noteId)
    }

    fun onRefresh() {
        isRefreshing = true
        viewModel.loadNoteDetail(noteId)
    }

    LaunchedEffect(noteDetailState) {
        if (noteDetailState !is UiState.Loading && isRefreshing) {
            isRefreshing = false
        }
    }

    val currentNote = (noteDetailState as? UiState.Success)?.data
    var isPinned by remember(currentNote) { mutableStateOf(currentNote?.isPinned ?: false) }
    val displayDate = (currentNote?.updatedAt ?: currentNote?.createdAt)?.substringBefore("T") ?: ""
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (noteDetailState is UiState.Loading && currentNote == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (noteDetailState is UiState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = (noteDetailState as UiState.Error).message, color = Color.Red)
        }
        return
    }

    if (currentNote == null) {
        return
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFA56A3A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }

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
                onClick = { currentNote.id?.let { onEditNote(it) } },
                containerColor = Color(0xFFA56A3A),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Note")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullRefreshState,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // IMAGE
                currentNote.imageUrl?.let { imageUrl ->
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
                        text = currentNote.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ISI
                    Text(
                        text = currentNote.description,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showDeleteDialog && currentNote.id != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan ini secara permanen?") },
            confirmButton = {
                Button(
                    onClick = {
                        currentNote.id?.let { onDelete(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}