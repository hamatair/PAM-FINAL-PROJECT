package com.example.pam_1.ui.screens.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@Composable
fun AddEditNoteScreen(
    note: Note? = null,                 // null = ADD, ada = EDIT
    onBack: () -> Unit,
    onSave: (
        title: String,
        description: String,
        isPinned: Boolean
    ) -> Unit,
    onAddImage: () -> Unit               // nanti isi logic picker
) {

    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onSave(title, description, isPinned)
                },
                containerColor = Color(0xFFA56A3A),
                contentColor = Color.White
            ) {
                Text("Save", fontSize = 16.sp)
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

                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (isPinned) Color(0xFFA56A3A) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== GAMBAR (JIKA ADA) =====
            note?.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Note Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== TOMBOL TAMBAH GAMBAR (JIKA BELUM ADA) =====
            if (note?.imageUrl == null) {
                OutlinedButton(
                    onClick = onAddImage,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text("Tambah Gambar")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== FORM =====
            Column(modifier = Modifier.padding(20.dp)) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Isi Catatan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    maxLines = Int.MAX_VALUE
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
