package com.example.pam_1.ui.screens.features.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.ProfileUIState
import com.example.pam_1.utils.FileUtils
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.navigations.popBackStackSafe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {

    val profileState by viewModel.profileState.collectAsState()
    val isUpdating = viewModel.isUpdatingProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE MODE EDIT ---
    var isEditing by remember { mutableStateOf(false) }

    // State untuk Full Screen Image Viewer
    var showImageViewer by remember { mutableStateOf(false) }

    // State untuk Dialog Pilihan Hapus/Edit Foto
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    // --- STATE DATA FORM ---
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // --- STATE VALIDASI ERROR ---
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    var phoneNumberError by remember { mutableStateOf(false) }
    var bioError by remember { mutableStateOf(false) }

    // State Foto
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isPhotoDeleted by remember { mutableStateOf(false) } // Penanda foto dihapus

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // Default avatar URL
    val DEFAULT_AVATAR = "https://jhrbjirccxuhtzygwzgx.supabase.co/storage/v1/object/public/profile/avatar/default.png"

    // Logika URL Gambar dengan Timestamp
    val finalImageUrl = remember(photoUrl, isPhotoDeleted) {
        if (isPhotoDeleted) {
            DEFAULT_AVATAR
        } else {
            "${photoUrl ?: DEFAULT_AVATAR}?t=${System.currentTimeMillis()}"
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    photoBytes = bytes
                    isPhotoDeleted = false // Reset flag hapus
                    Toast.makeText(context, "Foto terpilih (klik simpan untuk menerapkan)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- SINKRONISASI DATA ---
    LaunchedEffect(profileState, isEditing) {
        if (profileState is ProfileUIState.Success) {
            val user = (profileState as ProfileUIState.Success).user
            if (!isEditing) {
                val nameParts = user.full_name.trim().split(" ", limit = 2)
                firstName = nameParts.getOrNull(0) ?: "-"
                lastName = nameParts.getOrNull(1) ?: "-"

                username = user.username
                email = user.email
                phoneNumber = user.phone_number ?: ""
                bio = user.bio ?: ""
                photoUrl = user.photo_profile
                displayName = user.full_name.ifEmpty { "-" }
                photoBytes = null
                isPhotoDeleted = false

                firstNameError = false
                lastNameError = false
                phoneNumberError = false
                bioError = false
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }

    // --- KONTEN UTAMA ---
    Box(
        modifier = Modifier.fillMaxSize(),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .blur(radius = if (showImageViewer) 15.dp else 0.dp)
                ) {
                    Spacer(Modifier.height(64.dp))

                    if (isEditing) {
                        // --- MODE EDIT ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isEditing = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = PrimaryBrown)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    isEditing = false
                                    val user = state.user
                                    phoneNumber = user.phone_number ?: ""
                                    bio = user.bio ?: ""
                                    photoBytes = null
                                    isPhotoDeleted = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Batal")
                                }

                                IconButton(
                                    onClick = {
                                        firstNameError = false
                                        lastNameError = false
                                        phoneNumberError = false

                                        var isValid = true
                                        if (firstName.isBlank()) { firstNameError = true; isValid = false }
                                        if (lastName.isBlank()) { lastNameError = true; isValid = false }
                                        if (phoneNumber.isNotBlank() && phoneNumber == "-") {
                                            phoneNumberError = true; isValid = false
                                        }

                                        if (isValid) {
                                            val fullName = "$firstName $lastName".trim()

                                            // Handle foto yang dihapus
                                            val finalPhotoBytes = if (isPhotoDeleted) {
                                                null // Akan di-handle di ViewModel untuk set DEFAULT_AVATAR
                                            } else {
                                                photoBytes
                                            }

                                            viewModel.saveProfileChanges(
                                                username, fullName, phoneNumber, bio,
                                                finalPhotoBytes, isPhotoDeleted,
                                                onSuccess = {
                                                    Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                                                    isEditing = false
                                                    viewModel.fetchUserProfile()
                                                },
                                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                            )
                                        } else {
                                            Toast.makeText(context, "Cek kembali data Anda", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isUpdating
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, contentDescription = "Simpan")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // FOTO PROFIL (MODE EDIT) - Klik untuk buka dialog pilihan
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(finalImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Profil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            // Buka dialog pilihan hapus/edit
                                            showPhotoOptionsDialog = true
                                        }
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp).offset(x = 4.dp, y = 4.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("📷", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                    } else {
                        // --- MODE VIEW ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStackSafe() },
                                modifier = Modifier.align(Alignment.Top)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(finalImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Foto Profil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                showImageViewer = true
                                            }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!isEditing) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }

                    // Field Input Formulir
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it; firstNameError = false },
                                label = { Text("Nama Depan") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing, singleLine = true, isError = firstNameError
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it; lastNameError = false },
                                label = { Text("Nama Belakang") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing, singleLine = true, isError = lastNameError
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { }, label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(), enabled = false, singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } || input.isEmpty() || input == "-") {
                                phoneNumber = input
                                phoneNumberError = false
                            }
                        },
                        label = { Text("Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = phoneNumberError
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bio, onValueChange = { bio = it; bioError = false },
                        label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing, singleLine = false, maxLines = 3
                    )
                    Spacer(Modifier.height(8.dp))

                    if (!isEditing) {
                        Text(
                            text = "Edit Profil",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { isEditing = true }
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    if (!isEditing) {
                        Button(
                            onClick = {
                                viewModel.logout {
                                    navController.navigateSafe("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Keluar") }
                    }
                }

                // --- DIALOG PILIHAN HAPUS/EDIT FOTO ---
                if (showPhotoOptionsDialog) {
                    AlertDialog(
                        onDismissRequest = { showPhotoOptionsDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = "Foto Profil",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Text("Pilih tindakan untuk foto profil Anda")
                        },
                        confirmButton = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Tombol Hapus
                                OutlinedButton(
                                    onClick = {
                                        isPhotoDeleted = true
                                        photoBytes = null
                                        photoUrl = DEFAULT_AVATAR
                                        showPhotoOptionsDialog = false
                                        Toast.makeText(context, "Foto akan dihapus setelah simpan", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Hapus")
                                }

                                // Tombol Edit
                                Button(
                                    onClick = {
                                        showPhotoOptionsDialog = false
                                        pickPhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPhotoOptionsDialog = false }
                            ) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // --- FULL SCREEN IMAGE VIEWER ---
                if (showImageViewer) {
                    Dialog(
                        onDismissRequest = { showImageViewer = false },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                .clickable { showImageViewer = false },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(finalImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto Profil Penuh",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clickable(enabled = false) {}
                            )

                            IconButton(
                                onClick = { showImageViewer = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            state.user.photo_profile?.let { url ->
                                                val success = FileUtils.downloadImage(context, url)
                                                Toast.makeText(context,
                                                    if (success) "Foto disimpan ke Galeri" else "Gagal mengunduh",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Unduh Gambar",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}