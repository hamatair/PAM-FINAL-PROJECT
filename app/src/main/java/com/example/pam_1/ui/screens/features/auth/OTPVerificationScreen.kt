package com.example.pam_1.ui.screens.features.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.viewmodel.AuthUIState
import com.example.pam_1.viewmodel.AuthViewModel
// PENTING: Import Extension Functions
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.navigations.popBackStackSafe

@Composable
fun OTPVerificationScreen(navController: NavController, viewModel: AuthViewModel) {
    var otpCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val uiState = viewModel.authState
    val email = viewModel.pendingEmail

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Verifikasi Berhasil! Silakan login.", Toast.LENGTH_SHORT).show()
                // Navigasi setelah sukses dari Registrasi
                // --- PERBAIKAN 1: Gunakan navigateSafe dengan popUpTo ---
                navController.navigateSafe("login") {
                    popUpTo("register") { inclusive = true }
                }
                viewModel.resetState()
            }

            // --- PERUBAHAN UTAMA: Handle alur Reset Password ---
            is AuthUIState.AwaitingNewPassword -> {
                // Setelah OTP reset password sukses, navigasi ke layar input password baru
                // --- PERBAIKAN 2: Gunakan navigateSafe dengan popUpTo ---
                navController.navigateSafe("new_password") {
                    // Pastikan semua layar OTP dan Forgot Password dihapus dari back stack
                    popUpTo("forgot_password") { inclusive = true }
                }
                viewModel.resetState()
            }
            // ----------------------------------------------------

            is AuthUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verifikasi Email",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Kode verifikasi telah dikirim ke",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = {
                // Batasi input hanya angka dan maksimal 6 digit
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    otpCode = it
                }
            },
            label = { Text("Kode OTP") },
            placeholder = { Text("Masukkan 6 digit kode") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        if (uiState is AuthUIState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (otpCode.length == 6) {
                        viewModel.verifyOTP(otpCode)
                    } else {
                        Toast.makeText(context, "Kode OTP harus 6 digit", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Verifikasi")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tidak menerima kode?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alignByBaseline()
            )

            TextButton(
                onClick = { viewModel.resendOTP() },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.alignByBaseline()
            ) {
                Text("Kirim Ulang")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            // --- PERBAIKAN 3: Gunakan popBackStackSafe ---
            onClick = {
                navController.popBackStackSafe()
                viewModel.resetState()
            }
        ) {
            Text("Kembali ke Registrasi")
        }
    }
}