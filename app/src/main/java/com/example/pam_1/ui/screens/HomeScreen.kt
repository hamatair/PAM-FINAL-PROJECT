package com.example.pam_1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.viewmodel.AuthViewModel
import io.github.jan.supabase.auth.auth

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // Ambil session (cara paling stabil untuk v3)
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val email = session?.user?.email ?: "Tidak ada data"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Berhasil Login!",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Email: $email")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.logout {
                    onNavigateToLogin()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Logout")
        }
    }
}
