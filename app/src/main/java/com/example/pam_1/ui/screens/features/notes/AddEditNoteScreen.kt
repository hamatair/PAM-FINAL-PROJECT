package com.example.pam_1.ui.screens.features.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Note

@Composable
fun AddEditNoteScreen(
    note: Note? = null,
    onBack: () -> Unit,
    // Perubahan: Tambahkan parameter imageBytes ke onSave
    onSave: (title: String, description: String, isPinned: Boolean, imageBytes: ByteArray?) -> Unit,
    onAddImage: () -> Unit = {} // Parameter ini bisa kita abaikan/hapus krn logic ada di dalam screen
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    // STATE UNTUK GAMBAR
    // Uri lokal (untuk preview gambar yg baru dipilih)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // ByteArray (untuk dikirim ke Supabase)
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // LAUNCHER GALERI
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                selectedImageUri = it
                // Konversi Uri ke ByteArray
                val inputStream = context.contentResolver.openInputStream(it)
                selectedImageBytes = inputStream?.readBytes()
                inputStream?.close()
            }
        }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Kirim bytes gambar ke onSave
                    onSave(title, description, isPinned, selectedImageBytes)
                },
                containerColor = Color(0xFFA56A3A),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save Note")
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
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFA56A3A))
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

            // ===== LOGIKA TAMPILAN GAMBAR =====
            // Prioritas Preview:
            // 1. Gambar baru dari galeri (selectedImageUri)
            // 2. Gambar lama dari database (note.imageUrl)
            val imageToShow = selectedImageUri ?: note?.imageUrl

            if (imageToShow != null) {
                // Tampilkan Gambar
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = imageToShow,
                        contentDescription = "Note Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            // Jika diklik, bisa ganti gambar lagi
                            .clickable {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Tampilkan Tombol Tambah Gambar (Jika belum ada gambar)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .clickable {
                            // Buka Galeri
                            singlePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tambah Gambar", color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // FORM
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Isi Catatan") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    maxLines = Int.MAX_VALUE
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}