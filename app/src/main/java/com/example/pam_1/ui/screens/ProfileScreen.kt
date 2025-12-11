package com.example.pam_1.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {

    val profileState by viewModel.profileState.collectAsState()
    val isUpdating = viewModel.isUpdatingProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE MODE EDIT ---
    var isEditing by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

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
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

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
    LaunchedEffect(profileState, isEditing) {
        if (profileState is ProfileUIState.Success) {
            val user = (profileState as ProfileUIState.Success).user
            if (!isEditing) {
                // Pisah full_name dari spasi
                val nameParts = user.full_name.trim().split(" ", limit = 2)
                firstName = nameParts.getOrNull(0) ?: "-"
                lastName = nameParts.getOrNull(1) ?: "-"

                username = user.username
                email = user.username // Email lengkap dari username
                phoneNumber = user.phone_number
                bio = user.bio ?: "-"
                photoUrl = user.photo_profile
                displayName = user.full_name.ifEmpty { "-" }
                photoBytes = null

                // Reset error
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
                ) {
                    Spacer(Modifier.height(64.dp))

                    // PERBAIKAN: Pisahkan struktur untuk mode edit dan non-edit
                    if (isEditing) {
                        // Mode Edit: Cancel & Save di row teratas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // ==== BACK BUTTON (LEFT) ====
                            IconButton(onClick = { isEditing = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }

                            // ==== CANCEL & CONFIRM (RIGHT) ====
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // CANCEL BUTTON
                                IconButton(onClick = {
                                    isEditing = false
                                    if (profileState is ProfileUIState.Success) {
                                        val user = (profileState as ProfileUIState.Success).user
                                        val nameParts = user.full_name.trim().split(" ", limit = 2)
                                        firstName = nameParts.getOrNull(0) ?: "-"
                                        lastName = nameParts.getOrNull(1) ?: "-"
                                        phoneNumber = user.phone_number
                                        bio = user.bio ?: "-"
                                        photoBytes = null
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Batal")
                                }

                                // CONFIRM BUTTON
                                IconButton(
                                    onClick = {
                                        firstNameError = false
                                        lastNameError = false
                                        phoneNumberError = false
                                        bioError = false
                                        var isValid = true

                                        if (firstName.isBlank() || firstName == "-") {
                                            firstNameError = true; isValid = false
                                        }
                                        if (lastName.isBlank() || lastName == "-") {
                                            lastNameError = true; isValid = false
                                        }
                                        if (phoneNumber.isBlank() || phoneNumber == "-") {
                                            phoneNumberError = true; isValid = false
                                        }
                                        if (bio.isBlank() || bio == "-") {
                                            bioError = true; isValid = false
                                        }

                                        if (isValid) {
                                            val fullName = "$firstName $lastName"
                                            viewModel.saveProfileChanges(
                                                username,
                                                fullName,
                                                phoneNumber,
                                                bio,
                                                photoBytes,
                                                onSuccess = {
                                                    Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                                                    isEditing = false
                                                    viewModel.fetchUserProfile()
                                                },
                                                onError = { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
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

                        // Foto Profil di row kedua (centered)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                val finalImageUrl = remember(photoUrl, photoBytes) {
                                    if (photoBytes != null) photoBytes else "${photoUrl}?t=${System.currentTimeMillis()}"
                                }

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
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            pickPhotoLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp).offset(x = 4.dp, y = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "📷",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Mode Non-Edit: Back button & Foto Profil
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.align(Alignment.Top)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }

                            // Foto Profil di Tengah
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    val finalImageUrl = remember(photoUrl, photoBytes) {
                                        if (photoBytes != null) photoBytes else "${photoUrl}?t=${System.currentTimeMillis()}"
                                    }

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
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                showDownloadDialog = true
                                            }
                                    )
                                }
                            }

                            // Spacer untuk balance layout
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Display Name (Hanya tampil saat tidak edit)
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

                    // --- FORM FIELDS ---

                    // First Name & Last Name (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // First Name
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = {
                                    firstName = it
                                    firstNameError = false
                                },
                                label = { Text("First name") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing,
                                singleLine = true,
                                isError = firstNameError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        // Last Name
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = {
                                    lastName = it
                                    lastNameError = false
                                },
                                label = { Text("Last name") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing,
                                singleLine = true,
                                isError = lastNameError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Email (Always Read Only)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } || input.isEmpty() || input == "-") {
                                phoneNumber = input
                                phoneNumberError = false
                            }
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = phoneNumberError,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Bio
                    OutlinedTextField(
                        value = bio,
                        onValueChange = {
                            bio = it
                            bioError = false
                        },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing,
                        singleLine = false,
                        maxLines = 3,
                        isError = bioError,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    // Text "Edit profile" (di kiri bawah field terakhir, hanya saat tidak edit)
                    if (!isEditing) {
                        Text(
                            text = "Edit profile",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { isEditing = true }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Tombol Logout (hanya tampil saat tidak edit)
                    if (!isEditing) {
                        Button(
                            onClick = {
                                viewModel.logout {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Logout", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Dialog Download Foto
                if (showDownloadDialog) {
                    AlertDialog(
                        onDismissRequest = { showDownloadDialog = false },
                        title = { Text("Foto Profil") },
                        text = { Text("Apakah Anda ingin mengunduh foto profil ini ke galeri?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDownloadDialog = false
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