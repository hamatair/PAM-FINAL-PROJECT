package com.example.pam_1.ui.screens.features.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pam_1.viewmodel.AuthUIState
import com.example.pam_1.viewmodel.AuthViewModel
// PENTING: Import Extension Functions yang sudah dibuat di AppNavigation.kt
// Jika merah, arahkan kursor lalu tekan Alt+Enter untuk Import
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.navigations.popBackStackSafe

// Fungsi Validasi Kata Sandi
fun isValidPassword(password: String): Boolean {
    // Pola: Minimal 8 karakter, harus ada angka, huruf kecil, dan huruf besar.
    val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$"
    return password.matches(passwordPattern.toRegex())
}

// Fungsi Validasi Email Gmail
fun isValidGmail(email: String): Boolean {
    // Memeriksa apakah email diakhiri dengan "@gmail.com"
    return email.endsWith("@gmail.com")
}

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    // State untuk input
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // State untuk Penanganan Error (Validasi UI)
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // State BARU untuk visibilitas kata sandi
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uiState = viewModel.authState

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUIState.AwaitingOTP -> {
                // Navigasi ke layar verifikasi OTP
                Toast.makeText(context, "Kode OTP telah dikirim ke email Anda", Toast.LENGTH_LONG).show()

                // --- PERBAIKAN 1: Gunakan navigateSafe ---
                navController.navigateSafe("otp_verification")
            }

            is AuthUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }

            else -> {}
        }
    }

    // Tentukan VisualTransformation dan ikon berdasarkan state passwordVisible
    val visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val description = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(40.dp))

        // Header
        Text(
            text = "Daftar",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Mari buat akun baru",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // First Name & Last Name (Bersebelahan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nama Depan
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    firstNameError = false
                },
                label = {
                    Text(
                        "Nama Depan",
                        fontSize = 12.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f),
                isError = firstNameError,
                singleLine = true
            )

            // Nama Belakang
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    lastNameError = false
                },
                label = {
                    Text(
                        "Nama Belakang",
                        fontSize = 12.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f),
                isError = lastNameError,
                singleLine = true
            )

        }
        Spacer(Modifier.height(16.dp))

        // Nama Pengguna (Username)
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                usernameError = false
            },
            label = {
                Text(
                    "Nama Pengguna",
                    fontSize = 12.sp
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = usernameError,
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = false
            },
            label = {
                Text(
                    text = "Email",
                    fontSize = 12.sp
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError,
            supportingText = {
                if (emailError) {
                    Text(
                        text = "Email harus menggunakan domain @gmail.com",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true
        )
        Spacer(Modifier.height(2.dp))

        // Kata Sandi dengan Ikon Toggle
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
            },
            label = {
                Text(
                    text = "Kata Sandi",
                    fontSize = 12.sp
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            visualTransformation = visualTransformation,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text(
                        text = "Minimal 8 karakter, mengandung Huruf Besar, Huruf Kecil, & Angka",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        // Tombol Pendaftaran
        if (uiState is AuthUIState.Loading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Button(
                onClick = {
                    // LOGIKA VALIDASI SEBELUM KIRIM KE VIEWMODEL
                    var isValid = true

                    // Cek Nama Depan
                    if (firstName.isBlank()) {
                        firstNameError = true
                        isValid = false
                    }

                    // Cek Nama Belakang
                    if (lastName.isBlank()) {
                        lastNameError = true
                        isValid = false
                    }

                    // Cek Nama Pengguna
                    if (username.isBlank()) {
                        usernameError = true
                        isValid = false
                    }

                    // Cek Email
                    if (email.isBlank()) {
                        emailError = true
                        isValid = false
                    } else if (!isValidGmail(email)) {
                        emailError = true
                        isValid = false
                    }

                    // Cek Kata Sandi
                    if (password.isBlank()) {
                        passwordError = true
                        isValid = false
                    } else if (!isValidPassword(password)) {
                        passwordError = true
                        isValid = false
                    }

                    // Jika Validasi Lolos
                    if (isValid) {
                        // Gabungkan firstName dan lastName menjadi full_name
                        val full_name = "$firstName $lastName"
                        // Nomor telepon kosong karena tidak ada di formulir
                        val phone_number = null

                        // Kirim data dengan urutan: email, password, username, full_name, phone_number
                        viewModel.register(email, password, username, full_name, phone_number)
                    } else {
                        Toast.makeText(
                            context,
                            "Mohon lengkapi semua data dengan benar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Daftar")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sudah punya akun?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alignByBaseline()
            )

            TextButton(
                // --- PERBAIKAN 2: Gunakan popBackStackSafe ---
                onClick = { navController.popBackStackSafe() },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.alignByBaseline()
            ) {
                Text("Masuk")
            }
        }
    }
}