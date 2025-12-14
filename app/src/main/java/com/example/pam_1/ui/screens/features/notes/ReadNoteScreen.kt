package com.example.pam_1.ui.screens.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Note

@Composable
fun ReadNoteScreen(
    note: Note,
    onEditNote: (Long) -> Unit,
    onBack: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var isPinned by remember { mutableStateOf(note.isPinned) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { note.id?.let { onEditNote(it) } },
                containerColor = Color(0xFFA56A3A),
                contentColor = Color.White
            ) {
                Text("✎", fontSize = 22.sp)
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

            // ===== TOP BAR =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFA56A3A)
                    )
                }

                Row {
                    // ===== DELETE ICON =====
                    IconButton(
                        onClick = {
                            note.id?.let { onDelete(it) }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }

                    // ===== PIN ICON =====
                    IconButton(
                        onClick = {
                            isPinned = !isPinned
                            onPinToggle(isPinned)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) Color(0xFFA56A3A) else Color.Gray
                        )
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== IMAGE (DARI URL SUPABASE) =====
            note.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Note Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== CONTENT =====
            Column(modifier = Modifier.padding(20.dp)) {

                // Tanggal
                note.updatedAt?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Judul
                Text(
                    text = note.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Isi
                Text(
                    text = note.description,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
