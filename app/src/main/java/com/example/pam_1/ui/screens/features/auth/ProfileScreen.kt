package com.example.pam_1.ui.screens.features.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.ProfileUIState
import com.example.pam_1.utils.FileUtils
// PENTING: Import Extension Functions
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
                if (bytes != null) {
                    photoBytes = bytes
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

                    // Logika URL Gambar dengan Timestamp
                    val finalImageUrl = remember(photoUrl) {
                        "${photoUrl ?: ""}?t=${System.currentTimeMillis()}"
                    }

                    if (isEditing) {
                        // --- MODE EDIT ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isEditing = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBrown)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    isEditing = false
                                    // Reset data
                                    if (profileState is ProfileUIState.Success) {
                                        val user = (profileState as ProfileUIState.Success).user
                                        phoneNumber = user.phone_number ?: ""
                                        bio = user.bio ?: ""
                                        photoBytes = null
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Batal")
                                }

                                // TOMBOL SIMPAN
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
                                            viewModel.saveProfileChanges(
                                                username, fullName, phoneNumber, bio, photoBytes,
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

                        // FOTO PROFIL (MODE EDIT)
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
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("📷", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                    } else {
                        // --- MODE VIEW (NON-EDIT) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                // --- PERBAIKAN 1: Gunakan popBackStackSafe ---
                                onClick = { navController.popBackStackSafe() },
                                modifier = Modifier.align(Alignment.Top)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }

                            // FOTO PROFIL (MODE VIEW)
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
                                            .clickable { showDownloadDialog = true }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it; firstNameError = false },
                                label = { Text("First name") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing, singleLine = true, isError = firstNameError
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it; lastNameError = false },
                                label = { Text("Last name") },
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
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = phoneNumberError,
                        supportingText = {
                            if (phoneNumberError) Text("Nomor HP tidak valid", color = MaterialTheme.colorScheme.error)
                            else if (isEditing) Text("Kosongkan jika tidak ada", style = MaterialTheme.typography.bodySmall)
                        }
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
                            text = "Edit profile",
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
                                    // --- PERBAIKAN 2: Gunakan navigateSafe saat logout ---
                                    navController.navigateSafe("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Logout") }
                    }
                }

                // Dialog Download
                if (showDownloadDialog) {
                    AlertDialog(
                        onDismissRequest = { showDownloadDialog = false },
                        title = { Text("Foto Profil") },
                        text = { Text("Unduh foto ini?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDownloadDialog = false
                                    scope.launch {
                                        state.user.photo_profile?.let { url ->
                                            val success = FileUtils.downloadImage(context, url)
                                            Toast.makeText(context, if (success) "Tersimpan" else "Gagal", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) { Text("Unduh") }
                        },
                        dismissButton = { TextButton(onClick = { showDownloadDialog = false }) { Text("Batal") } }
                    )
                }
            }
        }
    }
}