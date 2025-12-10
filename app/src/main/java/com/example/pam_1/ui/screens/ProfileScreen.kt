package com.example.pam_1.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.ProfileUIState
import com.example.pam_1.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {

    val profileState by viewModel.profileState.collectAsState()
    val isUpdating = viewModel.isUpdatingProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE MODE EDIT ---
    // false = Mode Lihat, true = Mode Edit
    var isEditing by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // --- STATE DATA FORM (Untuk Edit) ---
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // State Foto
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) } // Jika user pilih foto baru

    // --- PHOTO PICKER (Untuk Mode Edit) ---
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) photoBytes = bytes
            }
        }
    }

    // --- SINKRONISASI DATA ---
    // Setiap kali profileState sukses atau mode edit dibatalkan, reset form ke data asli
    LaunchedEffect(profileState, isEditing) {
        if (profileState is ProfileUIState.Success) {
            val user = (profileState as ProfileUIState.Success).user
            // Hanya update state lokal jika kita TIDAK sedang mengedit (untuk reset)
            // atau jika baru pertama kali load
            if (!isEditing) {
                username = user.username
                fullName = user.full_name
                phone = user.phone_number
                bio = user.bio ?: ""
                photoUrl = user.photo_profile
                photoBytes = null // Reset foto baru jika batal edit
            }
        }
    }

    // Fetch awal
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Profil" else "Profil Saya") },
                actions = {
                    if (isEditing) {
                        // --- MODE EDIT: Tombol Batal & Simpan ---
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = {
                                viewModel.saveProfileChanges(
                                    username, fullName, phone, bio, photoBytes,
                                    onSuccess = {
                                        Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                                        isEditing = false // Kembali ke mode lihat
                                    },
                                    onError = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            enabled = !isUpdating
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Simpan", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // --- MODE LIHAT: Tombol Refresh, Edit, Logout ---
                        IconButton(onClick = { viewModel.refreshProfile() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Mode")
                        }
                        IconButton(onClick = {
                            viewModel.logout {
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->

        // Container Utama
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = profileState) {
                is ProfileUIState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileUIState.Error -> {
                    Column(
                        modifier = Modifier.padding(16.dp).align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌ ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchUserProfile() }) { Text("Coba Lagi") }
                    }
                }
                is ProfileUIState.Success -> {
                    // --- CONTENT UTAMA ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Logic Gambar
                        val finalImageUrl = remember(photoUrl, photoBytes) {
                            if (photoBytes != null) photoBytes else "${photoUrl}?t=${System.currentTimeMillis()}"
                        }

                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(finalImageUrl)
                                    .crossfade(true)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .build(),
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isEditing) {
                                            // Mode Edit: Ganti Gambar
                                            pickPhotoLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            // Mode Lihat: Tampilkan Popup Download
                                            showDownloadDialog = true
                                        }
                                    }
                            )

                            // Ikon kecil indikator edit pada foto
                            if (isEditing) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp).offset(x = (-4).dp, y = (-4).dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Ganti Foto",
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Hint text di bawah foto
                        Text(
                            text = if (isEditing) "Ketuk foto untuk mengganti" else "Ketuk foto untuk opsi unduh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(24.dp))

                        // 2. Form Fields (Editable vs ReadOnly)
                        ProfileTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            isEditing = isEditing
                        )
                        ProfileTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Nama Lengkap",
                            isEditing = isEditing
                        )
                        ProfileTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Nomor HP",
                            isEditing = isEditing
                        )
                        ProfileTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = "Bio",
                            isEditing = isEditing,
                            singleLine = false,
                            maxLines = 3
                        )
                    }

                    // --- DIALOG UNDUH (Popup Instagram Style) ---
                    if (showDownloadDialog) {
                        AlertDialog(
                            onDismissRequest = { showDownloadDialog = false },
                            title = { Text("Foto Profil") },
                            text = { Text("Apakah Anda ingin mengunduh foto profil ini ke galeri?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDownloadDialog = false
                                        // Proses Download
                                        scope.launch {
                                            state.user.photo_profile?.let { url ->
                                                val success = FileUtils.downloadImage(context, url)
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Foto tersimpan di galeri" else "Gagal mengunduh",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ) { Text("Unduh") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDownloadDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Komponen Custom TextField agar lebih rapi
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isEditing: Boolean,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing, // KUNCI: Hanya bisa diketik jika mode edit
            singleLine = singleLine,
            maxLines = maxLines,
            // Styling agar saat mode 'Lihat' terlihat bersih tapi tetap terbaca
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = Color.Transparent, // Hilangkan border saat mode lihat (opsional)
                disabledLabelColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Sedikit background
            ),
            shape = MaterialTheme.shapes.medium
        )
    }
}