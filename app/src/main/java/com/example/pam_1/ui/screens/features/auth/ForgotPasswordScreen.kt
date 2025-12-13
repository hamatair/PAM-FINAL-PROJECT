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


@Composable
fun ForgotPasswordScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    val uiState = viewModel.authState
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUIState.AwaitingOTP -> {
                Toast.makeText(context, "Kode OTP telah dikirim ke email Anda", Toast.LENGTH_LONG).show()
                // Navigasi ke layar verifikasi OTP
                navController.navigate("otp_verification")
            }
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
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "Lupa Password?",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Masukkan email kamu untuk mendapatkan kode reset password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        if (uiState is AuthUIState.Loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        // MENGGANTI: Memanggil fungsi untuk mengirim OTP
                        viewModel.sendResetPasswordOTP(email)
                    } else {
                        Toast.makeText(context, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Kirim Kode Reset")
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                navController.popBackStack()
                viewModel.resetState()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Kembali ke Login")
        }
    }
}