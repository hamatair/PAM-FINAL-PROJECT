package com.example.pam_1.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.pam_1.data.repository.AuthRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, repository: AuthRepository) {
    val isLoggedIn = repository.isUserLoggedIn()

    LaunchedEffect(true) {
        // Tambahkan delay agar popup permission sempat muncul dan user tidak kaget
        delay(1500) // 1.5 detik

        navController.navigate(if (isLoggedIn) "home" else "login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // Tampilan loading yang lebih rapi
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Bisa ganti Text dengan Logo app kamu
        CircularProgressIndicator()
    }
}