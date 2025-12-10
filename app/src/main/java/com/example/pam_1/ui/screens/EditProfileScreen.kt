package com.example.pam_1.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest // <--- WAJIB IMPORT INI
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.ProfileUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, viewModel: AuthViewModel) {

    val profileState by viewModel.profileState.collectAsState()
    val isUpdating = viewModel.isUpdatingProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }

    // --- FIX IMAGE PICKER ---
    val pickPhoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) photoBytes = bytes
            }
        }
    }

    LaunchedEffect(profileState) {
        if (profileState is ProfileUIState.Success) {
            val u = (profileState as ProfileUIState.Success).user
            if (username.isEmpty()) username = u.username
            if (fullName.isEmpty()) fullName = u.full_name
            if (phone.isEmpty()) phone = u.phone_number
            if (bio.isEmpty()) bio = u.bio ?: ""
            photoUrl = u.photo_profile
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Profil") }) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gambar
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoBytes ?: photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(130.dp)
            )

            Spacer(Modifier.height(8.dp))

            // --- FIX ERROR LAUNCH ---
            Button(onClick = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text("Ganti Foto")
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveProfileChanges(
                        username, fullName, phone, bio, photoBytes,
                        onSuccess = {
                            Toast.makeText(context, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating
            ) {
                if (isUpdating) Text("Menyimpan...") else Text("Simpan Perubahan")
            }
        }
    }
}