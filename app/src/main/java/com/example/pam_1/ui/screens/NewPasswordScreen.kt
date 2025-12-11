package com.example.pam_1.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.viewmodel.AuthUIState
import com.example.pam_1.viewmodel.AuthViewModel

@Composable
fun NewPasswordScreen(navController: NavController, viewModel: AuthViewModel) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var confirmError by remember { mutableStateOf(false) }

    val uiState = viewModel.authState
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Password berhasil diubah. Silakan Login.", Toast.LENGTH_LONG).show()
                navController.navigate("login") {
                    // Membersihkan stack agar user tidak bisa kembali ke halaman ini
                    popUpTo("new_password") { inclusive = true }
                }
                viewModel.resetState()
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Atur Password Baru",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Masukkan password baru Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        // New Password
        PasswordTextFieldToggle(
            value = newPassword,
            onValueChange = {
                newPassword = it
                passwordError = false
            },
            label = "Password Baru",
            isError = passwordError,
            supportingText = {
                if (passwordError) Text("Min 8 char, ada Huruf Besar, Kecil, & Angka")
            }
        )
        Spacer(Modifier.height(16.dp))

        // Confirm Password
        PasswordTextFieldToggle(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmError = false
            },
            label = "Konfirmasi Password",
            isError = confirmError,
            supportingText = {
                if (confirmError) Text("Konfirmasi password tidak cocok")
            }
        )
        Spacer(Modifier.height(32.dp))

        if (uiState is AuthUIState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    var isValid = true
                    // 1. Validasi format password
                    if (!isValidPassword(newPassword)) {
                        passwordError = true
                        isValid = false
                    }
                    // 2. Validasi kecocokan password
                    if (newPassword != confirmPassword) {
                        confirmError = true
                        isValid = false
                    }

                    if (isValid) {
                        viewModel.updatePassword(newPassword)
                    } else {
                        Toast.makeText(context, "Periksa kembali input password Anda.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Ubah Password")
            }
        }
    }
}

/**
 * Composable kustom untuk OutlinedTextField dengan toggle visibilitas password.
 */
@Composable
fun PasswordTextFieldToggle(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    supportingText: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val description = if (passwordVisible) "Hide password" else "Show password"

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = visualTransformation,
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = icon, contentDescription = description)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        supportingText = supportingText,
        singleLine = true
    )
}