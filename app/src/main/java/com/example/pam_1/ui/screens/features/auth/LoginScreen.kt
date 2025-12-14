package com.example.pam_1.ui.screens.features.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.viewmodel.AuthUIState
import com.example.pam_1.viewmodel.AuthViewModel
// PENTING: Import Extension Functions
import com.example.pam_1.navigations.navigateSafe

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uiState = viewModel.authState

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Login Berhasil", Toast.LENGTH_SHORT).show()
                // --- PERBAIKAN 1: Gunakan navigateSafe dengan popUpTo ---
                navController.navigateSafe("home") {
                    popUpTo("login") { inclusive = true }
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

    // Tentukan VisualTransformation berdasarkan state passwordVisible
    val visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val description = if (passwordVisible) "Hide password" else "Show password"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login Supabase", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        // Password Field dengan Ikon Toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = visualTransformation,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        // Forgot Password & Remember Me
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Checkbox "Remember Me"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                )
                Text(
                    text = "Ingat Saya",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 2. Tautan "Lupa Password?"
            Text(
                text = "Lupa Password?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                // --- PERBAIKAN 2: Gunakan navigateSafe ---
                modifier = Modifier.clickable {
                    navController.navigateSafe("forgot_password")
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        if (uiState is AuthUIState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.login(email, password, rememberMe)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Masuk")
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum punya akun? ",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Daftar",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    // --- PERBAIKAN 3: Gunakan navigateSafe ---
                    modifier = Modifier.clickable { navController.navigateSafe("register") }
                )
            }
        }
    }
}